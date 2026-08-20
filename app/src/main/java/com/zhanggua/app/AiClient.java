package com.zhanggua.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class AiClient {
    private static final String SYSTEM_PROMPT =
            "你是“掌卦”的《周易》解卦助手。最终回答只输出面向用户的结论，不得在最终回答中展示、复述或暗示内部思考过程、分析草稿、reasoning、thinking 或 <think> 标签；若 API 提供独立 reasoning 字段，客户端会另行折叠展示。"
            + "解读以传统《周易》卦义为参考，语言自然、克制、具体，不故弄玄虚，不使用绝对预言式措辞，也不要逐段复述用户已经看到的卦象数据。"
            + "输出固定为四部分：①卦意：2至3句话；②动爻：仅有动爻时用2至4句话；③变卦：1至2句话；④建议：2至3条简短可执行建议。"
            + "全文原则上控制在300至450个中文字符，最多不超过500个中文字符。医疗、法律、投资等高风险事项仅作传统文化娱乐参考，不替代专业判断。";

    interface StreamListener {
        void onAnswerDelta(String delta);
        void onReasoningDelta(String delta, boolean summaryOnly);
    }

    private static final class ChunkParts {
        final String answer;
        final String reasoning;
        final boolean reasoningSummary;

        ChunkParts(String answer, String reasoning, boolean reasoningSummary) {
            this.answer = answer == null ? "" : answer;
            this.reasoning = reasoning == null ? "" : reasoning;
            this.reasoningSummary = reasoningSummary;
        }
    }

    /** Splits compatible endpoints that place <think>...</think> inside content. */
    private static final class InlineThinkingRouter {
        private String carry = "";
        private boolean inThinking = false;

        void accept(String chunk, StringBuilder answer, StreamListener listener) {
            if (chunk == null || chunk.isEmpty()) return;
            String data = carry + chunk;
            carry = "";
            int pos = 0;
            while (pos < data.length()) {
                String tag = inThinking ? "</think>" : "<think>";
                int found = indexOfIgnoreCase(data, tag, pos);
                if (found >= 0) {
                    emit(data.substring(pos, found), answer, listener);
                    pos = found + tag.length();
                    inThinking = !inThinking;
                    continue;
                }
                int keep = trailingTagPrefix(data.substring(pos), tag);
                int end = data.length() - keep;
                if (end > pos) emit(data.substring(pos, end), answer, listener);
                if (keep > 0) carry = data.substring(end);
                break;
            }
        }

        void finish(StringBuilder answer, StreamListener listener) {
            if (!carry.isEmpty()) emit(carry, answer, listener);
            carry = "";
        }

        private void emit(String text, StringBuilder answer, StreamListener listener) {
            if (text == null || text.isEmpty()) return;
            if (inThinking) {
                if (listener != null) listener.onReasoningDelta(text, false);
            } else {
                answer.append(text);
                if (listener != null) listener.onAnswerDelta(text);
            }
        }

        private static int trailingTagPrefix(String text, String tag) {
            int max = Math.min(tag.length() - 1, text.length());
            for (int len = max; len > 0; len--) {
                if (tag.regionMatches(true, 0, text, text.length() - len, len)) return len;
            }
            return 0;
        }
    }

    private AiClient() {}

    static String interpret(AiSettingsStore.Settings s, String prompt) throws Exception {
        return interpretStream(s, prompt, null);
    }

    /**
     * Streams answer text and provider-supplied reasoning as two separate channels.
     * Only answer deltas become the final result. Reasoning is optional and is intended
     * for an expandable UI; OpenAI normally exposes a reasoning summary rather than raw CoT.
     */
    static String interpretStream(AiSettingsStore.Settings s, String prompt, StreamListener listener) throws Exception {
        String path = AiSettingsStore.MODE_CHAT.equals(s.mode) ? "/chat/completions" : "/responses";
        URL url = new URL(apiUrl(s.endpoint, path));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        configure(conn, s.apiKey, true);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("model", s.model);
        body.put("stream", true);
        if (AiSettingsStore.MODE_CHAT.equals(s.mode)) {
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            body.put("messages", messages);
            // Keep compatible chat endpoints from producing pages of text. DeepSeek counts
            // reasoning_content toward max_tokens, so leave enough room for a concise answer.
            body.put("max_tokens", 2000);
            if (AiSettingsStore.PROVIDER_GEMINI.equals(s.provider)) {
                body.put("reasoning_effort", "low");
            }
        } else {
            body.put("input", SYSTEM_PROMPT + "\n\n" + prompt);
            body.put("max_output_tokens", 1400);
            if (AiSettingsStore.PROVIDER_OPENAI.equals(s.provider) && isOpenAiReasoningModel(s.model)) {
                body.put("reasoning", new JSONObject().put("effort", "low").put("summary", "auto"));
                body.put("text", new JSONObject().put("verbosity", "low"));
            }
        }

        writeJson(conn, body);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String raw = readAll(conn.getErrorStream());
            conn.disconnect();
            throw new Exception(errorMessage(code, raw));
        }

        StringBuilder answer = new StringBuilder();
        InlineThinkingRouter inlineRouter = new InlineThinkingRouter();
        boolean sawSse = false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(":")) continue;
                if (trimmed.startsWith("event:")) {
                    sawSse = true;
                    continue;
                }

                String payload = trimmed;
                if (trimmed.startsWith("data:")) {
                    sawSse = true;
                    payload = trimmed.substring(5).trim();
                }
                if (payload.isEmpty() || "[DONE]".equals(payload)) continue;

                try {
                    JSONObject event = new JSONObject(payload);
                    JSONObject apiError = event.optJSONObject("error");
                    if (apiError != null) {
                        String message = apiError.optString("message", "流式请求失败");
                        throw new Exception(message);
                    }

                    ChunkParts parts = AiSettingsStore.MODE_CHAT.equals(s.mode)
                            ? chatParts(event) : responsesParts(event);
                    if (!parts.reasoning.isEmpty() && listener != null) {
                        listener.onReasoningDelta(parts.reasoning, parts.reasoningSummary);
                    }
                    if (!parts.answer.isEmpty()) {
                        inlineRouter.accept(parts.answer, answer, listener);
                        continue;
                    }

                    // Some compatible endpoints ignore stream=true and return one normal JSON response.
                    if (!sawSse) {
                        ChunkParts finalParts = AiSettingsStore.MODE_CHAT.equals(s.mode)
                                ? parseChatParts(payload) : parseResponsesParts(payload);
                        if (!finalParts.reasoning.isEmpty() && listener != null) {
                            listener.onReasoningDelta(finalParts.reasoning, finalParts.reasoningSummary);
                        }
                        inlineRouter.accept(finalParts.answer, answer, listener);
                    }
                } catch (org.json.JSONException ignored) {
                    // Ignore non-JSON SSE metadata. Actual API errors remain surfaced above.
                }
            }
            inlineRouter.finish(answer, listener);
        } finally {
            conn.disconnect();
        }

        String result = answer.toString().trim();
        if (result.isEmpty()) throw new Exception("API 返回成功，但没有可显示的最终解读");
        return result;
    }

    static List<String> listModels(AiSettingsStore.Settings s) throws Exception {
        URL url = new URL(apiUrl(s.endpoint, "/models"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        configure(conn, s.apiKey, false);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        String raw = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();
        if (code < 200 || code >= 300) throw new Exception(errorMessage(code, raw));

        JSONArray data = new JSONObject(raw).optJSONArray("data");
        ArrayList<String> ids = new ArrayList<>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                String id = data.optJSONObject(i) == null ? "" : data.optJSONObject(i).optString("id", "");
                if (!id.isEmpty() && looksLikeTextModel(id)) ids.add(id);
            }
        }
        Collections.sort(ids, String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    private static ChunkParts chatParts(JSONObject root) {
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) return new ChunkParts("", "", false);
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) return new ChunkParts("", "", false);
        JSONObject delta = choice.optJSONObject("delta");
        if (delta == null) return new ChunkParts(choice.optString("text", ""), "", false);

        String reasoning = firstNonEmpty(delta.optString("reasoning_content", ""),
                delta.optString("reasoning", ""), delta.optString("thinking", ""));
        String answer = contentText(delta.opt("content"));
        return new ChunkParts(answer, reasoning, false);
    }

    private static ChunkParts responsesParts(JSONObject event) {
        String type = event.optString("type", "");
        if ("response.output_text.delta".equals(type)) {
            return new ChunkParts(event.optString("delta", ""), "", false);
        }
        if ("response.reasoning_summary_text.delta".equals(type)) {
            return new ChunkParts("", event.optString("delta", ""), true);
        }
        if ("response.reasoning_text.delta".equals(type)) {
            return new ChunkParts("", event.optString("delta", ""), false);
        }
        String lower = type.toLowerCase(Locale.ROOT);
        if ((lower.contains("reasoning") || lower.contains("thinking"))
                && lower.endsWith(".delta") && event.has("delta")) {
            return new ChunkParts("", event.optString("delta", ""), lower.contains("summary"));
        }
        // Deliberately do not treat arbitrary *.delta events as answer text.
        return new ChunkParts("", "", false);
    }

    private static ChunkParts parseChatParts(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONArray choices = root.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg != null) {
                String answer = contentText(msg.opt("content"));
                String reasoning = firstNonEmpty(msg.optString("reasoning_content", ""),
                        msg.optString("reasoning", ""), msg.optString("thinking", ""));
                return new ChunkParts(answer, reasoning, false);
            }
        }
        throw new Exception("API 返回成功，但没有可显示的文本内容");
    }

    private static ChunkParts parseResponsesParts(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        String direct = root.optString("output_text", "");
        StringBuilder answer = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        boolean summaryOnly = true;
        if (!direct.trim().isEmpty()) answer.append(direct.trim());

        JSONArray output = root.optJSONArray("output");
        if (output != null) {
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.optJSONObject(i);
                if (item == null) continue;
                String type = item.optString("type", "");
                if ("reasoning".equals(type)) {
                    JSONArray summary = item.optJSONArray("summary");
                    if (summary != null) {
                        for (int j = 0; j < summary.length(); j++) {
                            JSONObject part = summary.optJSONObject(j);
                            if (part != null) appendSeparated(reasoning, part.optString("text", ""));
                        }
                    }
                    String rawReasoning = item.optString("reasoning_content", "");
                    if (!rawReasoning.isEmpty()) {
                        appendSeparated(reasoning, rawReasoning);
                        summaryOnly = false;
                    }
                    continue;
                }
                JSONArray content = item.optJSONArray("content");
                if (content == null) continue;
                for (int j = 0; j < content.length(); j++) {
                    JSONObject part = content.optJSONObject(j);
                    if (part == null) continue;
                    if ("output_text".equals(part.optString("type")) || part.has("text")) {
                        appendSeparated(answer, part.optString("text", ""));
                    }
                }
            }
        }
        if (answer.length() == 0) throw new Exception("API 返回成功，但没有可显示的文本内容");
        return new ChunkParts(answer.toString().trim(), reasoning.toString().trim(), summaryOnly);
    }

    private static String contentText(Object content) {
        if (content instanceof String) return (String) content;
        if (content instanceof JSONArray) {
            StringBuilder sb = new StringBuilder();
            JSONArray arr = (JSONArray) content;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) continue;
                String text = item.optString("text", "");
                if (!text.isEmpty()) sb.append(text);
            }
            return sb.toString();
        }
        return "";
    }

    private static boolean looksLikeTextModel(String id) {
        String x = id.toLowerCase();
        return !x.contains("embedding") && !x.contains("whisper") && !x.contains("tts")
                && !x.contains("image") && !x.contains("audio") && !x.contains("realtime")
                && !x.contains("moderation") && !x.contains("sora") && !x.contains("transcribe");
    }

    private static boolean isOpenAiReasoningModel(String model) {
        String x = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return x.startsWith("gpt-5") || x.startsWith("o1") || x.startsWith("o3") || x.startsWith("o4");
    }

    private static String apiUrl(String endpoint, String path) {
        String base = AiSettingsStore.normalizeEndpoint(endpoint);
        if (base.endsWith("/v1") || base.endsWith("/v1beta/openai") || base.endsWith("/openai")) return base + path;
        return base + "/v1" + path;
    }

    private static void configure(HttpURLConnection conn, String apiKey, boolean streaming) {
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(120000);
        conn.setUseCaches(false);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", streaming ? "text/event-stream" : "application/json");
    }

    private static void writeJson(HttpURLConnection conn, JSONObject body) throws Exception {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) if (value != null && !value.isEmpty()) return value;
        return "";
    }

    private static void appendSeparated(StringBuilder sb, String text) {
        if (text == null || text.isEmpty()) return;
        if (sb.length() > 0) sb.append('\n');
        sb.append(text);
    }

    private static int indexOfIgnoreCase(String text, String target, int from) {
        int max = text.length() - target.length();
        for (int i = Math.max(0, from); i <= max; i++) {
            if (text.regionMatches(true, i, target, 0, target.length())) return i;
        }
        return -1;
    }

    private static String errorMessage(int code, String raw) {
        try {
            JSONObject error = new JSONObject(raw).optJSONObject("error");
            if (error != null) {
                String m = error.optString("message", "");
                if (!m.isEmpty()) return "HTTP " + code + "：" + m;
            }
        } catch (Exception ignored) {}
        String compact = raw == null ? "" : raw.trim();
        if (compact.length() > 280) compact = compact.substring(0, 280) + "…";
        return "HTTP " + code + (compact.isEmpty() ? "" : "：" + compact);
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
