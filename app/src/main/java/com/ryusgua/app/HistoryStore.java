package com.ryusgua.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Persistent divination history. Pinned entries never count toward the 30-entry rolling limit. */
final class HistoryStore {
    private static final String PREFS = "ryusgua_history_v1";
    private static final String KEY = "entries";
    private static final int UNPINNED_LIMIT = 30;
    private static final int MAX_NOTE = 300;
    private static final int MAX_AI_TEXT = 12000;
    private static final int MAX_REASONING = 12000;

    static final class Entry {
        final String id;
        final long timeMillis;
        final int[] lines;
        final boolean formal;
        final boolean pinned;
        final String note;
        final String aiText;
        final String aiReasoning;
        final boolean aiReasoningSummaryOnly;
        final String aiModel;

        Entry(String id, long timeMillis, int[] lines, boolean formal, boolean pinned,
              String note, String aiText, String aiReasoning,
              boolean aiReasoningSummaryOnly, String aiModel) {
            this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
            this.timeMillis = timeMillis;
            this.lines = lines.clone();
            this.formal = formal;
            this.pinned = pinned;
            this.note = safe(note, MAX_NOTE);
            this.aiText = safe(aiText, MAX_AI_TEXT);
            this.aiReasoning = safe(aiReasoning, MAX_REASONING);
            this.aiReasoningSummaryOnly = aiReasoningSummaryOnly;
            this.aiModel = safe(aiModel, 160);
        }

        boolean hasAi() { return aiText != null && !aiText.trim().isEmpty(); }
        boolean hasNote() { return note != null && !note.trim().isEmpty(); }
    }

    private HistoryStore() {}

    static Entry add(Context context, int[] lines, boolean formal) {
        List<Entry> entries = load(context);
        Entry entry = new Entry(UUID.randomUUID().toString(), System.currentTimeMillis(), lines,
                formal, false, "", "", "", true, "");
        entries.add(0, entry);
        prune(entries);
        save(context, entries);
        return entry;
    }


    static Entry find(Context context, String id) {
        if (id == null || id.isEmpty()) return null;
        for (Entry e : load(context)) if (id.equals(e.id)) return e;
        return null;
    }

    static void updateAi(Context context, String id, String aiText, String reasoning,
                         boolean reasoningSummaryOnly, String model) {
        if (id == null || id.isEmpty()) return;
        List<Entry> entries = load(context);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!id.equals(e.id)) continue;
            entries.set(i, new Entry(e.id, e.timeMillis, e.lines, e.formal, e.pinned,
                    e.note, aiText, reasoning, reasoningSummaryOnly, model));
            break;
        }
        save(context, entries);
    }

    static boolean togglePin(Context context, String id) {
        List<Entry> entries = load(context);
        boolean pinned = false;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!id.equals(e.id)) continue;
            pinned = !e.pinned;
            entries.set(i, new Entry(e.id, e.timeMillis, e.lines, e.formal, pinned,
                    e.note, e.aiText, e.aiReasoning, e.aiReasoningSummaryOnly, e.aiModel));
            break;
        }
        prune(entries);
        sort(entries);
        save(context, entries);
        return pinned;
    }

    static void updateNote(Context context, String id, String note) {
        List<Entry> entries = load(context);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!id.equals(e.id)) continue;
            entries.set(i, new Entry(e.id, e.timeMillis, e.lines, e.formal, e.pinned,
                    note, e.aiText, e.aiReasoning, e.aiReasoningSummaryOnly, e.aiModel));
            break;
        }
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
                if (!ok) continue;
                long time = o.optLong("time", 0L);
                String id = o.optString("id", "");
                if (id.isEmpty()) continue;
                out.add(new Entry(id, time, lines,
                        o.optBoolean("formal", false),
                        o.optBoolean("pinned", false),
                        o.optString("note", ""),
                        o.optString("aiText", ""),
                        o.optString("aiReasoning", ""),
                        o.optBoolean("aiReasoningSummaryOnly", true),
                        o.optString("aiModel", "")));
            }
        } catch (Exception ignored) {}
        sort(out);
        return out;
    }

    /** Clear ordinary history while preserving pinned entries forever. */
    static void clearUnpinned(Context context) {
        List<Entry> entries = load(context);
        List<Entry> kept = new ArrayList<>();
        for (Entry e : entries) if (e.pinned) kept.add(e);
        save(context, kept);
    }


    private static void prune(List<Entry> entries) {
        sort(entries);
        int unpinned = 0;
        for (int i = 0; i < entries.size();) {
            Entry e = entries.get(i);
            if (e.pinned) { i++; continue; }
            unpinned++;
            if (unpinned > UNPINNED_LIMIT) entries.remove(i);
            else i++;
        }
    }

    private static void sort(List<Entry> entries) {
        Collections.sort(entries, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                return Long.compare(b.timeMillis, a.timeMillis);
            }
        });
    }

    private static void save(Context context, List<Entry> entries) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject o = new JSONObject();
                o.put("id", entry.id);
                o.put("time", entry.timeMillis);
                JSONArray l = new JSONArray();
                for (int v : entry.lines) l.put(v);
                o.put("lines", l);
                o.put("formal", entry.formal);
                o.put("pinned", entry.pinned);
                o.put("note", entry.note);
                o.put("aiText", entry.aiText);
                o.put("aiReasoning", entry.aiReasoning);
                o.put("aiReasoningSummaryOnly", entry.aiReasoningSummaryOnly);
                o.put("aiModel", entry.aiModel);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
    }

    private static String safe(String value, int max) {
        String v = value == null ? "" : value;
        return v.length() <= max ? v : v.substring(0, max);
    }

}
