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

/** Independent local Tarot history. v1 three-card entries remain readable as the default spread. */
final class TarotHistoryStore {
    private static final String PREFS="ryusgua_tarot_history_v1",KEY="entries"; private static final int UNPINNED_LIMIT=30;
    static final class Entry {
        final String id; final long timeMillis; final String spreadId; final int[] cardIds; final boolean[] reversed; final boolean pinned;
        final String note,aiText,aiReasoning,aiModel; final boolean aiReasoningSummaryOnly;
        Entry(String id,long time,String spread,int[] cards,boolean[] reversed,boolean pinned,String note,String ai,String reasoning,boolean summaryOnly,String model){
            this.id=id==null||id.isEmpty()?UUID.randomUUID().toString():id;timeMillis=time;spreadId=spread==null||spread.isEmpty()?"three":spread;
            cardIds=cards.clone();this.reversed=reversed.clone();this.pinned=pinned;this.note=cap(note,300);aiText=cap(ai,12000);aiReasoning=cap(reasoning,12000);aiReasoningSummaryOnly=summaryOnly;aiModel=cap(model,200);
        }
        boolean hasNote(){return !note.trim().isEmpty();} boolean hasAi(){return !aiText.trim().isEmpty();}
        TarotDeck.Draw[] draws(TarotDeck deck){TarotDeck.Draw[] out=new TarotDeck.Draw[cardIds.length];for(int i=0;i<out.length;i++)out[i]=new TarotDeck.Draw(deck.byId(cardIds[i]),reversed[i]);return out;}
    }
    private TarotHistoryStore(){}
    static Entry add(Context context,String spreadId,TarotDeck.Draw[] draws){
        int[] ids=new int[draws.length];boolean[] rev=new boolean[draws.length];for(int i=0;i<draws.length;i++){ids[i]=draws[i].card.id;rev[i]=draws[i].reversed;}
        List<Entry> entries=load(context);Entry e=new Entry(UUID.randomUUID().toString(),System.currentTimeMillis(),spreadId,ids,rev,false,"","","",true,"");entries.add(0,e);prune(entries);save(context,entries);return e;
    }
    static List<Entry> load(Context context){
        ArrayList<Entry> out=new ArrayList<>();SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        try{JSONArray arr=new JSONArray(p.getString(KEY,"[]"));for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);JSONArray idsJson=o.getJSONArray("cards"),revJson=o.getJSONArray("reversed");int count=idsJson.length();if(count<1||count>6||revJson.length()!=count)continue;int[] ids=new int[count];boolean[] rev=new boolean[count];for(int j=0;j<count;j++){ids[j]=idsJson.getInt(j);rev[j]=revJson.getBoolean(j);}out.add(new Entry(o.optString("id",""),o.optLong("time",0L),o.optString("spread","three"),ids,rev,o.optBoolean("pinned",false),o.optString("note",""),o.optString("ai",""),o.optString("aiReasoning",""),o.optBoolean("aiReasoningSummaryOnly",true),o.optString("aiModel","")));}}catch(Exception ignored){}
        sort(out);return out;
    }
    static Entry find(Context context,String id){for(Entry e:load(context))if(e.id.equals(id))return e;return null;}
    static boolean togglePin(Context context,String id){List<Entry> entries=load(context);boolean pinned=false;for(int i=0;i<entries.size();i++){Entry e=entries.get(i);if(!e.id.equals(id))continue;pinned=!e.pinned;entries.set(i,copy(e,pinned,e.note,e.aiText,e.aiReasoning,e.aiReasoningSummaryOnly,e.aiModel));break;}prune(entries);save(context,entries);return pinned;}
    static void updateNote(Context context,String id,String note){List<Entry> entries=load(context);for(int i=0;i<entries.size();i++){Entry e=entries.get(i);if(e.id.equals(id)){entries.set(i,copy(e,e.pinned,note,e.aiText,e.aiReasoning,e.aiReasoningSummaryOnly,e.aiModel));break;}}save(context,entries);}
    static void updateAi(Context context,String id,String ai,String reasoning,boolean summaryOnly,String model){List<Entry> entries=load(context);for(int i=0;i<entries.size();i++){Entry e=entries.get(i);if(e.id.equals(id)){entries.set(i,copy(e,e.pinned,e.note,ai,reasoning,summaryOnly,model));break;}}save(context,entries);}
    static void clearUnpinned(Context context){List<Entry> kept=new ArrayList<>();for(Entry e:load(context))if(e.pinned)kept.add(e);save(context,kept);}
    private static Entry copy(Entry e,boolean pinned,String note,String ai,String reasoning,boolean summaryOnly,String model){return new Entry(e.id,e.timeMillis,e.spreadId,e.cardIds,e.reversed,pinned,note,ai,reasoning,summaryOnly,model);}
    private static String cap(String v,int limit){String safe=v==null?"":v;return safe.length()>limit?safe.substring(0,limit):safe;}
    private static void prune(List<Entry> entries){sort(entries);int ordinary=0;for(int i=0;i<entries.size();){if(entries.get(i).pinned){i++;continue;}ordinary++;if(ordinary>UNPINNED_LIMIT)entries.remove(i);else i++;}}
    private static void sort(List<Entry> entries){Collections.sort(entries,new Comparator<Entry>(){@Override public int compare(Entry a,Entry b){if(a.pinned!=b.pinned)return a.pinned?-1:1;return Long.compare(b.timeMillis,a.timeMillis);}});}
    private static void save(Context context,List<Entry> entries){JSONArray arr=new JSONArray();try{for(Entry e:entries){JSONObject o=new JSONObject();o.put("id",e.id);o.put("time",e.timeMillis);o.put("spread",e.spreadId);JSONArray ids=new JSONArray(),rev=new JSONArray();for(int i=0;i<e.cardIds.length;i++){ids.put(e.cardIds[i]);rev.put(e.reversed[i]);}o.put("cards",ids);o.put("reversed",rev);o.put("pinned",e.pinned);o.put("note",e.note);o.put("ai",e.aiText);o.put("aiReasoning",e.aiReasoning);o.put("aiReasoningSummaryOnly",e.aiReasoningSummaryOnly);o.put("aiModel",e.aiModel);arr.put(o);}}catch(Exception ignored){}context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,arr.toString()).apply();}
}
