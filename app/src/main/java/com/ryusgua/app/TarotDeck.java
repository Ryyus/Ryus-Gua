package com.ryusgua.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline Tarot model. All card, spread and reference copy lives in tarot_cards.json. */
final class TarotDeck {
    static final class Card {
        final int id; final String name; final String nameEn; final String family; final String suit; final String glyph;
        final String uprightKeywords; final String uprightMeaning; final String reversedKeywords; final String reversedMeaning; final String advice;
        Card(JSONObject o) {
            id=o.optInt("id",0); name=o.optString("name","未命名牌"); nameEn=o.optString("name_en","Tarot Card");
            family=o.optString("family","小阿卡那"); suit=o.optString("suit",""); glyph=o.optString("glyph","◇");
            JSONObject up=o.optJSONObject("upright"), rev=o.optJSONObject("reversed");
            uprightKeywords=up==null?"观察、选择":up.optString("keywords","观察、选择");
            uprightMeaning=up==null?"留意这张牌照见的当下主题。":up.optString("meaning","留意这张牌照见的当下主题。");
            reversedKeywords=rev==null?"阻滞、调整":rev.optString("keywords","阻滞、调整");
            reversedMeaning=rev==null?"相同力量可能受阻、过度或转向内在。":rev.optString("meaning","相同力量可能受阻、过度或转向内在。");
            advice=o.optString("advice","先确认事实，再做一个可复核的小行动。");
        }
        String keywords(boolean reversed){return reversed?reversedKeywords:uprightKeywords;}
        String meaning(boolean reversed){return reversed?reversedMeaning:uprightMeaning;}
    }
    static final class Draw {
        final Card card; final boolean reversed;
        Draw(Card card,boolean reversed){this.card=card;this.reversed=reversed;}
        String orientation(){return reversed?"逆位":"正位";}
    }
    static final class Spread {
        final String id; final String name; final String nameEn; final String subtitle; final String[] positions; final String[] hints;
        Spread(JSONObject o){
            id=o.optString("id","three"); name=o.optString("name","三牌成阵"); nameEn=o.optString("name_en","THREE-CARD SPREAD"); subtitle=o.optString("subtitle","缘起 · 此刻 · 趋向");
            JSONArray p=o.optJSONArray("positions"); int count=p==null?0:p.length(); positions=new String[count]; hints=new String[count];
            for(int i=0;i<count;i++){JSONObject item=p.optJSONObject(i);positions[i]=item==null?"牌位":item.optString("name","牌位");hints[i]=item==null?"观察此位置与问题的关系。":item.optString("hint","观察此位置与问题的关系。");}
        }
        int size(){return positions.length;}
    }
    static final class Topic {
        final String title; final String summary; final String body;
        Topic(JSONObject o){title=o.optString("title","塔罗术理");summary=o.optString("summary","本地塔罗参考");body=o.optString("body","塔罗牌用于组织观察与提问，不宣称给出不可改变的结局。");}
    }

    private final List<Card> cards; private final Map<Integer,Card> cardsById;
    private final List<Spread> spreads; private final Map<String,Spread> spreadsById; private final List<Topic> topics;
    private final SecureRandom random=new SecureRandom();

    TarotDeck(Context context){
        ArrayList<Card> loadedCards=new ArrayList<>(); ArrayList<Spread> loadedSpreads=new ArrayList<>(); ArrayList<Topic> loadedTopics=new ArrayList<>();
        try{
            JSONObject root=new JSONObject(readRaw(context)); JSONArray ca=root.getJSONArray("cards");
            for(int i=0;i<ca.length();i++)loadedCards.add(new Card(ca.getJSONObject(i)));
            JSONArray sa=root.getJSONArray("spreads"); for(int i=0;i<sa.length();i++)loadedSpreads.add(new Spread(sa.getJSONObject(i)));
            JSONArray ta=root.optJSONArray("topics"); if(ta!=null)for(int i=0;i<ta.length();i++)loadedTopics.add(new Topic(ta.getJSONObject(i)));
        }catch(Exception ignored){loadedCards.clear();loadedSpreads.clear();loadedTopics.clear();}
        if(loadedCards.size()!=78)throw new IllegalStateException("tarot_cards.json must contain 78 cards");
        if(loadedSpreads.isEmpty())throw new IllegalStateException("tarot_cards.json has no spreads");
        cards=Collections.unmodifiableList(loadedCards);spreads=Collections.unmodifiableList(loadedSpreads);topics=Collections.unmodifiableList(loadedTopics);
        LinkedHashMap<Integer,Card> byId=new LinkedHashMap<>();for(Card card:cards)byId.put(card.id,card);cardsById=Collections.unmodifiableMap(byId);
        LinkedHashMap<String,Spread> bySpread=new LinkedHashMap<>();for(Spread spread:spreads)bySpread.put(spread.id,spread);spreadsById=Collections.unmodifiableMap(bySpread);
    }
    private String readRaw(Context context)throws Exception{
        InputStream in=context.getResources().openRawResource(R.raw.tarot_cards);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))>=0)out.write(buffer,0,n);in.close();return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
    List<Spread> spreads(){return spreads;} List<Topic> topics(){return topics;}
    Spread spread(String id){Spread found=spreadsById.get(id);if(found!=null)return found;Spread three=spreadsById.get("three");return three==null?spreads.get(0):three;}
    Card byId(int id){Card found=cardsById.get(id);return found==null?cards.get(0):found;}
    Draw[] draw(Spread spread){int count=Math.max(1,Math.min(6,spread==null?3:spread.size()));List<Card> shuffled=new ArrayList<>(cards);Collections.shuffle(shuffled,random);Draw[] out=new Draw[count];for(int i=0;i<count;i++)out[i]=new Draw(shuffled.get(i),random.nextBoolean());return out;}

    String reading(Draw[] draws,Spread spread){return reading(draws,spread,"");}
    String reading(Draw[] draws,Spread spread,String question){return TarotReadingComposer.compose(draws,spread,question);}
    String meaningText(Draw[] draws,Spread spread){
        if(draws==null||spread==null||draws.length!=spread.size())return "牌阵尚未完成。";StringBuilder out=new StringBuilder();
        for(int i=0;i<draws.length;i++){Draw d=draws[i];out.append("【").append(spread.positions[i]).append(" · ").append(d.card.name).append(" · ").append(d.orientation()).append("】\n").append(d.card.nameEn).append(" / ").append(d.card.family);if(!d.card.suit.isEmpty())out.append(" · ").append(d.card.suit);out.append("\n关键词：").append(d.card.keywords(d.reversed)).append("\n牌义：").append(d.card.meaning(d.reversed)).append("\n牌位：").append(spread.hints[i]).append("\n建议：").append(d.card.advice).append("\n\n");}
        return out.append("正逆位描述的是同一主题的不同状态。请结合问题、牌位与现实事实阅读，不以关键词替代判断。").toString();
    }
    String aiFacts(Draw[] draws,Spread spread){
        if(draws==null||spread==null||draws.length!=spread.size())return "";StringBuilder out=new StringBuilder("牌阵：").append(spread.name).append("（").append(spread.subtitle).append("）\n");
        for(int i=0;i<draws.length;i++){Draw d=draws[i];out.append(i+1).append(". ").append(spread.positions[i]).append("：").append(d.card.name).append(" / ").append(d.orientation()).append("；关键词：").append(d.card.keywords(d.reversed)).append("；本地牌义：").append(d.card.meaning(d.reversed)).append("\n");}return out.toString();
    }
}
