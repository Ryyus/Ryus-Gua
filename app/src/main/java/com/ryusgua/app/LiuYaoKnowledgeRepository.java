package com.ryusgua.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Structured, fully local index distilled from liuyao-divination references (MIT). */
final class LiuYaoKnowledgeRepository {
    static final class Topic {
        final String id, title, summary, body;
        final List<String> keywords;
        Topic(String id,String title,String summary,String body,List<String>keywords){this.id=id;this.title=title;this.summary=summary;this.body=body;this.keywords=keywords;}
        String displayTitle(){return id+" · "+title;}
    }

    private final List<Topic> topics=new ArrayList<>();

    LiuYaoKnowledgeRepository(Context context){
        try(BufferedReader br=new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.liuyao_knowledge), StandardCharsets.UTF_8))){
            StringBuilder s=new StringBuilder(); String line; while((line=br.readLine())!=null)s.append(line);
            JSONArray arr=new JSONArray(s.toString());
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i); JSONArray k=o.optJSONArray("keywords"); List<String>ks=new ArrayList<>();
                if(k!=null)for(int j=0;j<k.length();j++)ks.add(k.optString(j,""));
                topics.add(new Topic(o.optString("id"),o.optString("title"),o.optString("summary"),o.optString("body"),ks));
            }
        }catch(Exception ignored){}
    }

    List<Topic> all(){return new ArrayList<>(topics);}
    Topic at(int index){return index>=0&&index<topics.size()?topics.get(index):null;}

    List<Topic> relevant(LiuYaoBoard.Board b){
        List<Topic> out=new ArrayList<>();
        for(Topic t:topics)if(isRelevant(t.id,b))out.add(t);
        return out;
    }

    String relevantDigest(LiuYaoBoard.Board b){
        StringBuilder s=new StringBuilder("【本地术理索引】\n");
        for(Topic t:relevant(b)){
            if(s.length()>2600)break;
            s.append(t.displayTitle()).append("：").append(t.summary).append("\n");
        }
        return s.toString();
    }

    private boolean isRelevant(String id,LiuYaoBoard.Board b){
        if("01".equals(id)||"03".equals(id)||"08".equals(id)||"11".equals(id))return true;
        if("02".equals(id)||"09".equals(id))return b!=null&&b.yongshenQin!=null&&!b.yongshenQin.isEmpty();
        if(b==null)return false;
        if("04".equals(id)){
            for(LiuYaoBoard.Line l:b.lines)if(l.void||l.monthBreak||l.dayBreak||l.darkMove)return true;
        }
        if("05".equals(id))for(LiuYaoBoard.Line l:b.lines)if(l.moving)return true;
        if("06".equals(id))return !b.sanhe.isEmpty()||b.liuheGua||b.liuchongGua||b.changedLiuhe||b.changedLiuchong;
        if("07".equals(id))for(LiuYaoBoard.Line l:b.lines)if(!l.graves.isEmpty())return true;
        if("10".equals(id))return b.fanYin||b.fuYin||(b.guaBian!=null&&!b.guaBian.isEmpty());
        return false;
    }
}
