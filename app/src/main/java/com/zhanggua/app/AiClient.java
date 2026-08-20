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

final class AiClient {
    interface StreamListener {
        void onDelta(String delta);
    }

    private AiClient() {}

    static String interpret(AiSettingsStore.Settings s, String prompt) throws Exception {
        return interpretStream(s, prompt, null);
    }

    /**
     * Streams model text as it arrives. Chat Completions is parsed from choices[].delta,
     * while Responses API is parsed from response.output_text.delta SSE events.
     * The complete text is also returned after the stream finishes.
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
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "你是《周易》传统文化解读助手。解释应克制、具体、清楚，不故弄玄虚，不把卦象描述为确定的现实预测；遇到医疗、法律、投资等高风险事项，应明确卦象不能替代专业判断。"));
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            body.put("messages", messages);
        } else {
            body.put("input", "你是《周易》传统文化解读助手。解释应克制、具体、清楚，不故弄玄虚，不把卦象描述为确定的现实预测；遇到医疗、法律、投资等高风险事项，应明确卦象不能替代专业判断。\n\n" + prompt);
        }

        writeJson(conn, body);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String raw = readAll(conn.getErrorStream());
            conn.disconnect();
            throw new Exception(errorMessage(code, raw));
        }

        StringBuilder full = new StringBuilder();
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

                    String delta = AiSettingsStore.MODE_CHAT.equals(s.mode)
                            ? chatDelta(event) : responsesDelta(event);
                    if (!delta.isEmpty()) {
                        full.append(delta);
                        if (listener != null) listener.onDelta(delta);
                        continue;
                    }

                    // Some compatible endpoints ignore stream=true and return one normal
                    // JSON response. Handle that without forcing the user to change mode.
                    if (!sawSse) {
                        String finalText = AiSettingsStore.MODE_CHAT.equals(s.mode)
                                ? parseChat(payload) : parseResponses(payload);
                        if (!finalText.isEmpty()) {
                            full.append(finalText);
                            if (listener != null) listener.onDelta(finalText);
                        }
                    }
                } catch (org.json.JSONException ignored) {
                    // Ignore non-JSON SSE metadata. Actual API errors remain surfaced above.
                }
            }
        } finally {
            conn.disconnect();
        }

        String result = full.toString().trim();
        if (result.isEmpty()) throw new Exception("API 返回成功，但没有可显示的文本内容");
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

    private static String chatDelta(JSONObject root) {
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) return "";
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) return "";
        JSONObject delta = choice.optJSONObject("delta");
        if (delta == null) return choice.optString("text", "");
        Object content = delta.opt("content");
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

    private static String responsesDelta(JSONObject event) {
        String type = event.optString("type", "");
        if ("response.output_text.delta".equals(type)) return event.optString("delta", "");
        // A few OpenAI-compatible implementations emit a generic text delta while still
        // using the Responses-shaped event envelope.
        if (type.endsWith(".delta") && event.has("delta")) return event.optString("delta", "");
        return "";
    }

    private static boolean looksLikeTextModel(String id) {
        String x = id.toLowerCase();
        return !x.contains("embedding") && !x.contains("whisper") && !x.contains("tts")
                && !x.contains("image") && !x.contains("audio") && !x.contains("realtime")
                && !x.contains("moderation") && !x.contains("sora") && !x.contains("transcribe");
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

    private static String parseChat(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONArray choices = root.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg != null) {
                Object content = msg.opt("content");
                if (content instanceof String && !((String) content).trim().isEmpty()) return ((String) content).trim();
                if (content instanceof JSONArray) {
                    StringBuilder sb = new StringBuilder();
                    JSONArray arr = (JSONArray) content;
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.optJSONObject(i);
                        if (item != null) {
                            String t = item.optString("text", "");
                            if (!t.isEmpty()) sb.append(t);
                        }
                    }
                    if (sb.length() > 0) return sb.toString().trim();
                }
            }
        }
        throw new Exception("API 返回成功，但没有可显示的文本内容");
    }

    private static String parseResponses(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        String direct = root.optString("output_text", "");
        if (!direct.trim().isEmpty()) return direct.trim();
        JSONArray output = root.optJSONArray("output");
        StringBuilder sb = new StringBuilder();
        if (output != null) {
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.optJSONObject(i);
                if (item == null) continue;
                JSONArray content = item.optJSONArray("content");
                if (content == null) continue;
                for (int j = 0; j < content.length(); j++) {
                    JSONObject part = content.optJSONObject(j);
                    if (part == null) continue;
                    if ("output_text".equals(part.optString("type")) || part.has("text")) {
                        String t = part.optString("text", "");
                        if (!t.isEmpty()) {
                            if (sb.length() > 0) sb.append('\n');
                            sb.append(t);
                        }
                    }
                }
            }
        }
        if (sb.length() == 0) throw new Exception("API 返回成功，但没有可显示的文本内容");
        return sb.toString().trim();
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
