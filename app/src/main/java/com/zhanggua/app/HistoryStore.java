package com.zhanggua.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class HistoryStore {
    private static final String PREFS = "zhang_gua_history";
    private static final String KEY = "entries";
    private static final int LIMIT = 30;

    static final class Entry {
        final long timeMillis;
        final int[] lines;
        Entry(long timeMillis, int[] lines) {
            this.timeMillis = timeMillis;
            this.lines = lines.clone();
        }
    }

    private HistoryStore() {}

    static void add(Context context, int[] lines) {
        List<Entry> entries = load(context);
        entries.add(0, new Entry(System.currentTimeMillis(), lines));
        while (entries.size() > LIMIT) entries.remove(entries.size() - 1);
        save(context, entries);
    }

    static List<Entry> load(Context context) {
        List<Entry> out = new ArrayList<>();
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = p.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray l = o.getJSONArray("lines");
                if (l.length() != 6) continue;
                int[] lines = new int[6];
                boolean ok = true;
                for (int j = 0; j < 6; j++) {
                    lines[j] = l.getInt(j);
                    if (lines[j] < 6 || lines[j] > 9) ok = false;
                }
                if (ok) out.add(new Entry(o.optLong("time", 0L), lines));
            }
        } catch (Exception ignored) {}
        return out;
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, "[]").apply();
    }

    private static void save(Context context, List<Entry> entries) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject o = new JSONObject();
                o.put("time", entry.timeMillis);
                JSONArray l = new JSONArray();
                for (int v : entry.lines) l.put(v);
                o.put("lines", l);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
    }
}
