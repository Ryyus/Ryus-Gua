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
    private AiClient() {}

    static String interpret(AiSettingsStore.Settings s, String prompt) throws Exception {
        String path = AiSettingsStore.MODE_CHAT.equals(s.mode) ? "/chat/completions" : "/responses";
        URL url = new URL(apiUrl(s.endpoint, path));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        configure(conn, s.apiKey);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("model", s.model);
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
        String raw = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) throw new Exception(errorMessage(code, raw));
        return AiSettingsStore.MODE_CHAT.equals(s.mode) ? parseChat(raw) : parseResponses(raw);
    }

    static List<String> listModels(AiSettingsStore.Settings s) throws Exception {
        URL url = new URL(apiUrl(s.endpoint, "/models"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        configure(conn, s.apiKey);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        String raw = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
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

    private static void configure(HttpURLConnection conn, String apiKey) {
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(90000);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
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
