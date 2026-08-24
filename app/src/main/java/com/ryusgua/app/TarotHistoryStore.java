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

/** Independent local tarot history. Pinned entries do not count toward the rolling limit. */
final class TarotHistoryStore {
    private static final String PREFS = "ryusgua_tarot_history_v1";
    private static final String KEY = "entries";
    private static final int UNPINNED_LIMIT = 30;

    static final class Entry {
        final String id;
        final long timeMillis;
        final int[] cardIds;
        final boolean[] reversed;
        final boolean pinned;
        final String note;

        Entry(String id, long timeMillis, int[] cardIds, boolean[] reversed, boolean pinned, String note) {
            this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
            this.timeMillis = timeMillis;
            this.cardIds = cardIds.clone();
            this.reversed = reversed.clone();
            this.pinned = pinned;
            String safe = note == null ? "" : note;
            this.note = safe.length() > 300 ? safe.substring(0, 300) : safe;
        }

        boolean hasNote() { return !note.trim().isEmpty(); }
        TarotDeck.Draw[] draws() {
            TarotDeck.Draw[] out = new TarotDeck.Draw[3];
            for (int i = 0; i < 3; i++) out[i] = new TarotDeck.Draw(TarotDeck.byId(cardIds[i]), reversed[i]);
            return out;
        }
    }

    private TarotHistoryStore() {}

    static Entry add(Context context, TarotDeck.Draw[] draws) {
        int[] ids = new int[3]; boolean[] rev = new boolean[3];
        for (int i = 0; i < 3; i++) { ids[i] = draws[i].card.id; rev[i] = draws[i].reversed; }
        List<Entry> entries = load(context);
        Entry entry = new Entry(UUID.randomUUID().toString(), System.currentTimeMillis(), ids, rev, false, "");
        entries.add(0, entry); prune(entries); save(context, entries); return entry;
    }

    static List<Entry> load(Context context) {
        ArrayList<Entry> out = new ArrayList<>();
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(p.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray idsJson = o.getJSONArray("cards");
                JSONArray revJson = o.getJSONArray("reversed");
                if (idsJson.length() != 3 || revJson.length() != 3) continue;
                int[] ids = new int[3]; boolean[] rev = new boolean[3];
                for (int j = 0; j < 3; j++) { ids[j] = idsJson.getInt(j); rev[j] = revJson.getBoolean(j); }
                out.add(new Entry(o.optString("id", ""), o.optLong("time", 0L), ids, rev,
                        o.optBoolean("pinned", false), o.optString("note", "")));
            }
        } catch (Exception ignored) {}
        sort(out); return out;
    }

    static boolean togglePin(Context context, String id) {
        List<Entry> entries = load(context); boolean pinned = false;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i); if (!e.id.equals(id)) continue;
            pinned = !e.pinned;
            entries.set(i, new Entry(e.id, e.timeMillis, e.cardIds, e.reversed, pinned, e.note)); break;
        }
        prune(entries); save(context, entries); return pinned;
    }

    static void updateNote(Context context, String id, String note) {
        List<Entry> entries = load(context);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i); if (!e.id.equals(id)) continue;
            entries.set(i, new Entry(e.id, e.timeMillis, e.cardIds, e.reversed, e.pinned, note)); break;
        }
        save(context, entries);
    }

    static void clearUnpinned(Context context) {
        List<Entry> kept = new ArrayList<>();
        for (Entry e : load(context)) if (e.pinned) kept.add(e);
        save(context, kept);
    }

    private static void prune(List<Entry> entries) {
        sort(entries); int ordinary = 0;
        for (int i = 0; i < entries.size();) {
            if (entries.get(i).pinned) { i++; continue; }
            ordinary++; if (ordinary > UNPINNED_LIMIT) entries.remove(i); else i++;
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
            for (Entry e : entries) {
                JSONObject o = new JSONObject(); o.put("id", e.id); o.put("time", e.timeMillis);
                JSONArray ids = new JSONArray(); JSONArray rev = new JSONArray();
                for (int i = 0; i < 3; i++) { ids.put(e.cardIds[i]); rev.put(e.reversed[i]); }
                o.put("cards", ids); o.put("reversed", rev); o.put("pinned", e.pinned); o.put("note", e.note); arr.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply();
    }
}
