package com.zhanggua.app;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ZhouYiRepository {
    static final class TextEntry {
        final String guaCi;
        final LinkedHashMap<String, String> yao;
        TextEntry(String guaCi, LinkedHashMap<String, String> yao) {
            this.guaCi = guaCi;
            this.yao = yao;
        }
    }

    private final Map<String, TextEntry> entries = new LinkedHashMap<>();

    ZhouYiRepository(Context context) {
        try (InputStream in = context.getResources().openRawResource(R.raw.zhouyi)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONObject root = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            Iterator<String> names = root.keys();
            while (names.hasNext()) {
                String name = names.next();
                JSONObject object = root.getJSONObject(name);
                LinkedHashMap<String, String> yao = new LinkedHashMap<>();
                JSONObject yaoObject = object.optJSONObject("yao");
                if (yaoObject != null) {
                    Iterator<String> labels = yaoObject.keys();
                    while (labels.hasNext()) {
                        String label = labels.next();
                        yao.put(label, yaoObject.optString(label, ""));
                    }
                }
                entries.put(name, new TextEntry(object.optString("gua_ci", ""), yao));
            }
            applyCorrections();
        } catch (Exception ignored) {}
    }

    TextEntry get(String name) {
        TextEntry e = entries.get(name);
        if (e != null) return e;
        return new TextEntry("暂无经文。", new LinkedHashMap<>());
    }

    String lineText(String hexagramName, int lineIndex, boolean yang) {
        TextEntry e = get(hexagramName);
        String label = traditionalLineLabel(lineIndex, yang);
        String value = e.yao.get(label);
        if (value != null) return value;
        // Some source editions contain mislabeled keys; use position as a safe fallback.
        List<String> values = new ArrayList<>(e.yao.values());
        if (lineIndex >= 0 && lineIndex < values.size()) return values.get(lineIndex);
        return "暂无爻辞。";
    }

    static String traditionalLineLabel(int i, boolean yang) {
        String prefix = yang ? "九" : "六";
        switch (i) {
            case 0: return "初" + prefix;
            case 1: return prefix + "二";
            case 2: return prefix + "三";
            case 3: return prefix + "四";
            case 4: return prefix + "五";
            case 5: return "上" + prefix;
            default: return "";
        }
    }

    private void applyCorrections() {
        // Correct a few obvious transcription/OCR issues in the upstream public-domain dataset.
        TextEntry qian = entries.get("地山谦");
        if (qian != null && !qian.yao.containsKey("六二") && qian.yao.containsKey("六三")) {
            LinkedHashMap<String, String> fixed = new LinkedHashMap<>();
            fixed.put("初六", qian.yao.get("初六"));
            fixed.put("六二", "鸣谦，贞吉。");
            fixed.put("九三", "劳谦，君子有终，吉。");
            fixed.put("六四", qian.yao.get("六四"));
            fixed.put("六五", qian.yao.get("六五"));
            fixed.put("上六", qian.yao.get("上六"));
            entries.put("地山谦", new TextEntry(qian.guaCi, fixed));
        }
        TextEntry shi = entries.get("地水师");
        if (shi != null) {
            LinkedHashMap<String, String> fixed = new LinkedHashMap<>();
            fixed.put("初六", "师出以律，否臧凶。");
            fixed.put("九二", "在师中，吉，无咎，王三锡命。");
            fixed.put("六三", "师或舆尸，凶。");
            fixed.put("六四", "师左次，无咎。");
            fixed.put("六五", "田有禽，利执言，无咎。长子帅师，弟子舆尸，贞凶。");
            fixed.put("上六", "大君有命，开国承家，小人勿用。");
            entries.put("地水师", new TextEntry("贞，丈人吉，无咎。", fixed));
        }
        TextEntry kun = entries.get("泽水困");
        if (kun != null) entries.put("泽水困", new TextEntry("亨，贞，大人吉，无咎，有言不信。", kun.yao));
    }
}
