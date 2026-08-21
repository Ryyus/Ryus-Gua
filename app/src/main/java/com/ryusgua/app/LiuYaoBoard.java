package com.ryusgua.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fully local 六爻纳甲排盘引擎。
 *
 * Rules/static tables are a Java reimplementation of the MIT-licensed
 * Johnson-Jia/liuyao-divination project. No Python, network or model is used.
 */
final class LiuYaoBoard {
    private static final String GAN="甲乙丙丁戊己庚辛壬癸", ZHI="子丑寅卯辰巳午未申酉戌亥";
    private static final String[] SHEN={"青龙","朱雀","勾陈","螣蛇","白虎","玄武"};
    private static final String[] CS={"长生","沐浴","冠带","临官","帝旺","衰","病","死","墓","绝","胎","养"};
    private static final Set<String> CS_STRONG=new HashSet<>(Arrays.asList("长生","冠带","临官","帝旺"));
    private static final Map<String,String> ZWX=new HashMap<>(), CHONG=new HashMap<>(), LIUHE=new HashMap<>(), SHENG=new HashMap<>(), KE=new HashMap<>(), MU=new HashMap<>(), JIN=new HashMap<>(), TUI=new HashMap<>(), SHEN_START=new HashMap<>(), BWX=new HashMap<>(), CS_START=new HashMap<>();
    private static final Map<String,String[]> BGAN=new HashMap<>(), BZHI=new HashMap<>();
    private static final String[][] XK={{"戌","亥"},{"申","酉"},{"午","未"},{"辰","巳"},{"寅","卯"},{"子","丑"}};
    private static final Map<String,Hex> HEX=new LinkedHashMap<>();

    private static final class Hex {
        final String name,up,lo,palace,pwx,stage; final int world;
        Hex(String n,String u,String l,String p,String w,int s,String st){name=n;up=u;lo=l;palace=p;pwx=w;world=s;stage=st;}
    }

    static {
        String[][] zw={{"子","水"},{"丑","土"},{"寅","木"},{"卯","木"},{"辰","土"},{"巳","火"},{"午","火"},{"未","土"},{"申","金"},{"酉","金"},{"戌","土"},{"亥","水"}};
        for(String[] a:zw) ZWX.put(a[0],a[1]);
        pairs(CHONG,"子午 丑未 寅申 卯酉 辰戌 巳亥");
        pairs(LIUHE,"子丑 寅亥 卯戌 辰酉 巳申 午未");
        cycle(SHENG,new String[]{"木","火","土","金","水"});
        KE.put("木","土");KE.put("土","水");KE.put("水","火");KE.put("火","金");KE.put("金","木");
        MU.put("木","未");MU.put("火","戌");MU.put("金","丑");MU.put("水","辰");MU.put("土","辰");
        JIN.put("寅","卯");JIN.put("巳","午");JIN.put("申","酉");JIN.put("亥","子");JIN.put("丑","辰");JIN.put("辰","未");JIN.put("未","戌");
        for(Map.Entry<String,String> e:JIN.entrySet()) TUI.put(e.getValue(),e.getKey());
        for(String g:new String[]{"甲","乙"})SHEN_START.put(g,"青龙"); for(String g:new String[]{"丙","丁"})SHEN_START.put(g,"朱雀"); SHEN_START.put("戊","勾陈");SHEN_START.put("己","螣蛇"); for(String g:new String[]{"庚","辛"})SHEN_START.put(g,"白虎"); for(String g:new String[]{"壬","癸"})SHEN_START.put(g,"玄武");
        bagua("乾","金","甲","壬","子寅辰午申戌"); bagua("兑","金","丁","丁","巳卯丑亥酉未"); bagua("离","火","己","己","卯丑亥酉未巳"); bagua("震","木","庚","庚","子寅辰午申戌");
        bagua("巽","木","辛","辛","丑亥酉未巳卯"); bagua("坎","水","戊","戊","寅辰午申戌子"); bagua("艮","土","丙","丙","辰午申戌子寅"); bagua("坤","土","乙","癸","未巳卯丑亥酉");
        CS_START.put("木","亥");CS_START.put("火","寅");CS_START.put("金","巳");CS_START.put("水","申");CS_START.put("土","寅");
        addPalace("乾","金",new String[][]{{"乾为天","乾","乾","6","本宫"},{"天风姤","乾","巽","1","一世"},{"天山遁","乾","艮","2","二世"},{"天地否","乾","坤","3","三世"},{"风地观","巽","坤","4","四世"},{"山地剥","艮","坤","5","五世"},{"火地晋","离","坤","4","游魂"},{"火天大有","离","乾","3","归魂"}});
        addPalace("兑","金",new String[][]{{"兑为泽","兑","兑","6","本宫"},{"泽水困","兑","坎","1","一世"},{"泽地萃","兑","坤","2","二世"},{"泽山咸","兑","艮","3","三世"},{"水山蹇","坎","艮","4","四世"},{"地山谦","坤","艮","5","五世"},{"雷山小过","震","艮","4","游魂"},{"雷泽归妹","震","兑","3","归魂"}});
        addPalace("离","火",new String[][]{{"离为火","离","离","6","本宫"},{"火山旅","离","艮","1","一世"},{"火风鼎","离","巽","2","二世"},{"火水未济","离","坎","3","三世"},{"山水蒙","艮","坎","4","四世"},{"风水涣","巽","坎","5","五世"},{"天水讼","乾","坎","4","游魂"},{"天火同人","乾","离","3","归魂"}});
        addPalace("震","木",new String[][]{{"震为雷","震","震","6","本宫"},{"雷地豫","震","坤","1","一世"},{"雷水解","震","坎","2","二世"},{"雷风恒","震","巽","3","三世"},{"地风升","坤","巽","4","四世"},{"水风井","坎","巽","5","五世"},{"泽风大过","兑","巽","4","游魂"},{"泽雷随","兑","震","3","归魂"}});
        addPalace("巽","木",new String[][]{{"巽为风","巽","巽","6","本宫"},{"风天小畜","巽","乾","1","一世"},{"风火家人","巽","离","2","二世"},{"风雷益","巽","震","3","三世"},{"天雷无妄","乾","震","4","四世"},{"火雷噬嗑","离","震","5","五世"},{"山雷颐","艮","震","4","游魂"},{"山风蛊","艮","巽","3","归魂"}});
        addPalace("坎","水",new String[][]{{"坎为水","坎","坎","6","本宫"},{"水泽节","坎","兑","1","一世"},{"水雷屯","坎","震","2","二世"},{"水火既济","坎","离","3","三世"},{"泽火革","兑","离","4","四世"},{"雷火丰","震","离","5","五世"},{"地火明夷","坤","离","4","游魂"},{"地水师","坤","坎","3","归魂"}});
        addPalace("艮","土",new String[][]{{"艮为山","艮","艮","6","本宫"},{"山火贲","艮","离","1","一世"},{"山天大畜","艮","乾","2","二世"},{"山泽损","艮","兑","3","三世"},{"火泽睽","离","兑","4","四世"},{"天泽履","乾","兑","5","五世"},{"风泽中孚","巽","兑","4","游魂"},{"风山渐","巽","艮","3","归魂"}});
        addPalace("坤","土",new String[][]{{"坤为地","坤","坤","6","本宫"},{"地雷复","坤","震","1","一世"},{"地泽临","坤","兑","2","二世"},{"地天泰","坤","乾","3","三世"},{"雷天大壮","震","乾","4","四世"},{"泽天夬","兑","乾","5","五世"},{"水天需","坎","乾","4","游魂"},{"水地比","坎","坤","3","归魂"}});
    }

    static final class Line {
        final int pos; String yy,gan,zhi,wx,qin,shen,cGan,cZhi,cWx,cQin,state,cs,change,dayMove,role,effectiveReason=""; boolean world,response,moving,xunVoid,trueVoid,monthBreak,dayStrike,darkMove,dayBreak,onMonth,onDay,graveVoid; Boolean effective; final List<String> graves=new ArrayList<>();
        Line(int p){pos=p;}
        String posName(){return new String[]{"初","二","三","四","五","上"}[pos-1];}
        String tags(){List<String>a=new ArrayList<>();if(world)a.add("世");if(response)a.add("应");if(moving)a.add("动");if(xunVoid)a.add(trueVoid?"真空":"空");if(monthBreak)a.add("月破");if(darkMove)a.add("暗动");if(dayBreak)a.add("日破");if(onMonth)a.add("临月");if(onDay)a.add("临日");if(dayMove!=null)a.add(dayMove);if(moving&&change!=null)a.add(change);a.addAll(graves);if(graveVoid)a.add("空墓");if(role!=null)a.add(role);if(effective!=null&&("元神".equals(role)||"忌神".equals(role)))a.add(effective?"有力":"无力");return join(a,"·");}
    }

    static final class Board {
        String hexagram,changedHexagram,upper,lower,cUpper,cLower,palace,palaceWx,palaceStage,monthZhi,monthWx,dayGz,dayGan,dayZhi,dayWx,yongshenQin="",yongshenWx,guaBian=""; int worldPos,responsePos; String[] xunkong; LiuYaoCalendar.Pillars pillars; final Line[] lines={new Line(1),new Line(2),new Line(3),new Line(4),new Line(5),new Line(6)}; final List<Line> yongshenLines=new ArrayList<>(); final Map<String,String> sanhe=new LinkedHashMap<>(); boolean fanYin,fuYin,liuheGua,liuchongGua,changedLiuhe,changedLiuchong;
        List<Line> movingLines(){List<Line>o=new ArrayList<>();for(Line l:lines)if(l.moving)o.add(l);return o;}
        String summary(){StringBuilder s=new StringBuilder();s.append(hexagram).append(" → ").append(changedHexagram).append("\n").append(palace).append("宫·").append(palaceWx).append("·").append(palaceStage).append("  世").append(worldPos).append("应").append(responsePos).append("\n").append("月建 ").append(monthZhi).append(" · 日辰 ").append(dayGz).append(" · 旬空 ").append(xunkong[0]).append(xunkong[1]);if(!yongshenQin.isEmpty())s.append("\n用神 ").append(yongshenQin).append(yongshenWx==null?"":"·"+yongshenWx);return s.toString();}
        String toPlainText(){StringBuilder s=new StringBuilder("【六爻排盘】\n").append(pillars.dateTimeText()).append("\n").append(summary()).append("\n\n");for(int i=5;i>=0;i--){Line l=lines[i];s.append(l.posName()).append(l.yy).append("  ").append(l.shen).append("  ").append(l.qin).append("  ").append(l.gan).append(l.zhi).append(l.wx);String t=l.tags();if(!t.isEmpty())s.append("  [").append(t).append("]");if(l.moving)s.append(" → ").append(l.cGan).append(l.cZhi).append(l.cWx).append(" ").append(l.cQin);s.append("\n");}s.append("\n卦变：").append(guaBian);if(fanYin)s.append(" · 反吟");if(fuYin)s.append(" · 伏吟");if(liuheGua)s.append(" · 主卦六合");if(liuchongGua)s.append(" · 主卦六冲");if(changedLiuhe)s.append(" · 变卦六合");if(changedLiuchong)s.append(" · 变卦六冲");for(Map.Entry<String,String>e:sanhe.entrySet())s.append("\n三合：").append(e.getKey()).append(" · ").append(e.getValue());return s.toString();}
        String digest(){StringBuilder s=new StringBuilder();s.append("排盘：").append(palace).append("宫").append(palaceWx).append("，世").append(worldPos).append("应").append(responsePos).append("；月建").append(monthZhi).append("，日辰").append(dayGz).append("，旬空").append(xunkong[0]).append(xunkong[1]).append("。\n");for(Line l:lines){List<String>a=new ArrayList<>();if(l.world)a.add("世");if(l.response)a.add("应");if(l.moving)a.add("动");if(l.darkMove)a.add("暗动");if(l.monthBreak)a.add("月破");if(l.dayBreak)a.add("日破");if(l.xunVoid)a.add(l.trueVoid?"真空":"旬空");a.addAll(l.graves);if(l.moving&&l.change!=null)a.add(l.change);if(!a.isEmpty())s.append(l.posName()).append(l.qin).append(l.gan).append(l.zhi).append("：").append(join(a,"、")).append("；").append(l.state).append("/ ").append(l.cs).append("。\n");}s.append("卦变").append(guaBian).append("。");return s.toString();}
    }

    private LiuYaoBoard(){}

    static Board cast(int[] values,long epochMs,String yongshenQin){
        if(values==null||values.length!=6)throw new IllegalArgumentException("需要六爻");
        HexagramEngine.Hexagram base=HexagramEngine.lookup(values,false), changed=HexagramEngine.lookup(values,true); Hex r=HEX.get(base.name), cr=HEX.get(changed.name); if(r==null||cr==null)throw new IllegalStateException("六十四卦索引缺失");
        Board b=new Board();b.hexagram=r.name;b.changedHexagram=cr.name;b.upper=r.up;b.lower=r.lo;b.cUpper=cr.up;b.cLower=cr.lo;b.palace=r.palace;b.palaceWx=r.pwx;b.palaceStage=r.stage;b.worldPos=r.world;b.responsePos=((r.world-1+3)%6)+1;b.yongshenQin=yongshenQin==null?"":yongshenQin;
        b.pillars=LiuYaoCalendar.at(epochMs);b.monthZhi=b.pillars.monthZhi;b.monthWx=ZWX.get(b.monthZhi);b.dayGz=b.pillars.dayGanZhi;b.dayGan=b.dayGz.substring(0,1);b.dayZhi=b.dayGz.substring(1,2);b.dayWx=ZWX.get(b.dayZhi);b.xunkong=xunkong(b.dayGz);
        String[] lg=BGAN.get(r.lo),ug=BGAN.get(r.up),lz=BZHI.get(r.lo),uz=BZHI.get(r.up);int shen0=indexOf(SHEN,SHEN_START.get(b.dayGan));
        for(int i=0;i<6;i++){Line l=b.lines[i];l.yy=HexagramEngine.isYang(values[i])?"阳":"阴";l.gan=i<3?lg[0]:ug[1];l.zhi=i<3?lz[i]:uz[i];l.wx=ZWX.get(l.zhi);l.qin=liuqin(l.wx,b.palaceWx);l.shen=SHEN[(shen0+i)%6];l.world=l.pos==b.worldPos;l.response=l.pos==b.responsePos;l.moving=HexagramEngine.isMoving(values[i]);l.state=wuxingState(l.wx,b.monthWx);l.cs=changsheng(l.wx,l.zhi);boolean prosperous=isProsperous(l.wx,l.zhi,b.monthZhi,b.dayZhi);l.xunVoid=contains(b.xunkong,l.zhi);l.trueVoid=l.xunVoid&&isTrueVoid(l.wx,b.monthZhi);l.monthBreak=isChong(b.monthZhi,l.zhi);l.dayStrike=isChong(b.dayZhi,l.zhi);l.onMonth=l.zhi.equals(b.monthZhi);l.onDay=l.zhi.equals(b.dayZhi);if(l.dayStrike&&!l.moving){l.darkMove=prosperous;l.dayBreak=!prosperous;}if(l.dayStrike&&l.moving)l.dayMove=prosperous?"动而愈动":"动而冲散";}
        String[] clg=BGAN.get(cr.lo),cug=BGAN.get(cr.up),clz=BZHI.get(cr.lo),cuz=BZHI.get(cr.up);
        for(Line l:b.movingLines()){int i=l.pos-1;l.cGan=i<3?clg[0]:cug[1];l.cZhi=i<3?clz[i]:cuz[i];l.cWx=ZWX.get(l.cZhi);l.cQin=liuqin(l.cWx,b.palaceWx);if(l.wx.equals(l.cWx)){if(l.cZhi.equals(JIN.get(l.zhi)))l.change="化进神";else if(l.cZhi.equals(TUI.get(l.zhi)))l.change="化退神";else l.change="化比和";}else if(l.wx.equals(SHENG.get(l.cWx)))l.change="化回头生";else if(l.wx.equals(KE.get(l.cWx)))l.change="化回头克";else l.change="化他";}
        for(Line l:b.lines){String mu=MU.get(l.wx);if(mu.equals(b.monthZhi))l.graves.add("月墓");if(mu.equals(b.dayZhi))l.graves.add("日墓");for(Line o:b.lines)if(o.pos!=l.pos&&o.moving&&mu.equals(o.zhi))l.graves.add("动爻墓");if(l.moving&&mu.equals(l.cZhi))l.graves.add("化墓");l.graveVoid=!l.graves.isEmpty()&&contains(b.xunkong,mu);}
        applyYongshen(b);analyzeGuaBian(b,cr);detectFanFu(b);detectSanhe(b);detectLiuhe(b);return b;
    }

    private static void applyYongshen(Board b){if(b.yongshenQin.isEmpty())return;for(Line l:b.lines)if(b.yongshenQin.equals(l.qin))b.yongshenLines.add(l);if(!b.yongshenLines.isEmpty()){Line best=b.yongshenLines.get(0);for(Line l:b.yongshenLines)if(strength(l)>strength(best))best=l;b.yongshenWx=best.wx;}if(b.yongshenWx==null)return;for(Line l:b.lines){l.role=role(l.wx,b.yongshenWx);if(b.yongshenLines.contains(l))l.role="用神";if("元神".equals(l.role)||"忌神".equals(l.role)){String x=effect(l);if(x.startsWith("+")){l.effective=true;l.effectiveReason=x.substring(1);}else if(x.startsWith("-")){l.effective=false;l.effectiveReason=x.substring(1);}}}}
    private static int strength(Line l){int s=0;if(l.onMonth||l.onDay)s+=100;if("旺".equals(l.state)||"相".equals(l.state))s+=40;if(l.moving||l.darkMove)s+=20;if(l.monthBreak||l.dayBreak||l.trueVoid)s-=60;if(!l.graves.isEmpty())s-=20;return s;}
    private static String effect(Line l){if(l.monthBreak)return"-月破无力";if(!l.graves.isEmpty())return"-入墓无力";if("化回头克".equals(l.change))return"-化回头克无力";if("化退神".equals(l.change))return"-化退神无力";if(l.dayBreak)return"-日破无力";if(l.trueVoid)return"-真空无力";if(l.onMonth||l.onDay)return"+临日月有力";if("旺".equals(l.state)||"相".equals(l.state))return"+旺相有力";if("化回头生".equals(l.change)||"化进神".equals(l.change))return"+"+l.change+"有力";if(l.moving||l.darkMove)return"+发动有力";return"";}
    private static void analyzeGuaBian(Board b,Hex c){String m=b.palaceWx,w=c.pwx;if(m.equals(w))b.guaBian="变比和(吉)";else if(m.equals(SHENG.get(w)))b.guaBian="变生主(吉)";else if(m.equals(KE.get(w)))b.guaBian="变克主(凶)";else if(w.equals(SHENG.get(m)))b.guaBian="主生变(泄)";else if(w.equals(KE.get(m)))b.guaBian="主克变(制)";else b.guaBian="无";}
    private static void detectFanFu(Board b){List<Line>m=b.movingLines();if(m.isEmpty())return;b.fuYin=true;for(Line l:b.lines)if(l.moving&&!l.zhi.equals(l.cZhi)){b.fuYin=false;break;}b.fanYin=true;for(Line l:m)if(!isChong(l.zhi,l.cZhi)){b.fanYin=false;break;}}
    private static void detectSanhe(Board b){String[][]g={{"申","子","辰","水"},{"亥","卯","未","木"},{"寅","午","戌","火"},{"巳","酉","丑","金"}};Set<String>all=new HashSet<>(),mov=new HashSet<>();for(Line l:b.lines){all.add(l.zhi);if(l.moving||l.darkMove)mov.add(l.zhi);}for(String[]x:g){int p=0,m=0;String miss="";for(int i=0;i<3;i++){if(all.contains(x[i]))p++;else miss+=x[i];if(mov.contains(x[i]))m++;}if(p>0)b.sanhe.put(x[0]+x[1]+x[2]+x[3]+"局",p==3?(m>=2?"实成之局":"虚合待用"):"虚合缺"+miss);}}
    private static void detectLiuhe(Board b){String[]m=new String[6],c=new String[6];for(int i=0;i<6;i++){m[i]=b.lines[i].zhi;c[i]=b.lines[i].moving?b.lines[i].cZhi:b.lines[i].zhi;}b.liuheGua=pairSet(m,LIUHE);b.liuchongGua=pairSet(m,CHONG);b.changedLiuhe=pairSet(c,LIUHE);b.changedLiuchong=pairSet(c,CHONG);}
    private static boolean pairSet(String[]z,Map<String,String>map){int[][]p={{0,3},{1,4},{2,5}};for(int[]q:p)if(!z[q[1]].equals(map.get(z[q[0]])))return false;return true;}
    private static String role(String w,String y){if(w.equals(y))return"用神";String yuan=shengMe(y),ji=keMe(y),chou=keMe(yuan);if(w.equals(yuan))return"元神";if(w.equals(ji))return"忌神";if(w.equals(chou))return"仇神";return"闲神";}
    private static String liuqin(String w,String p){if(w.equals(p))return"兄弟";if(w.equals(shengMe(p)))return"父母";if(w.equals(SHENG.get(p)))return"子孙";if(w.equals(keMe(p)))return"官鬼";if(w.equals(KE.get(p)))return"妻财";return"兄弟";}
    private static String shengMe(String w){for(Map.Entry<String,String>e:SHENG.entrySet())if(w.equals(e.getValue()))return e.getKey();return"";} private static String keMe(String w){for(Map.Entry<String,String>e:KE.entrySet())if(w.equals(e.getValue()))return e.getKey();return"";}
    private static String wuxingState(String w,String ling){if(w.equals(ling))return"旺";if(w.equals(shengMe(ling)))return"相";if(w.equals(SHENG.get(ling)))return"休";if(w.equals(keMe(ling)))return"囚";if(w.equals(KE.get(ling)))return"死";return"休";}
    private static String changsheng(String w,String z){String s=CS_START.get(w);return s==null?"—":CS[floorMod(ZHI.indexOf(z)-ZHI.indexOf(s),12)];}
    private static boolean isProsperous(String w,String z,String mz,String dz){String mw=ZWX.get(mz),dw=ZWX.get(dz),st=wuxingState(w,mw);return"旺".equals(st)||"相".equals(st)||w.equals(SHENG.get(mw))||w.equals(SHENG.get(dw))||w.equals(mw)||w.equals(dw)||CS_STRONG.contains(changsheng(w,z));}
    private static boolean isTrueVoid(String w,String mz){if("寅卯".contains(mz))return"土".equals(w);if("巳午".contains(mz))return"金".equals(w);if("申酉".contains(mz))return"木".equals(w);if("亥子".contains(mz))return"火".equals(w);return false;}
    private static String[] xunkong(String gz){int gi=GAN.indexOf(gz.substring(0,1)),zi=ZHI.indexOf(gz.substring(1,2)),n=gi;while(n%12!=zi)n+=10;return XK[(n/10)%6];}
    private static boolean isChong(String a,String b){return b!=null&&b.equals(CHONG.get(a));}
    private static void pairs(Map<String,String>m,String s){for(String p:s.split(" ")){String a=p.substring(0,1),b=p.substring(1,2);m.put(a,b);m.put(b,a);}} private static void cycle(Map<String,String>m,String[]a){for(int i=0;i<a.length;i++)m.put(a[i],a[(i+1)%a.length]);}
    private static void bagua(String n,String w,String ig,String og,String z){BWX.put(n,w);BGAN.put(n,new String[]{ig,og});String[]a=new String[6];for(int i=0;i<6;i++)a[i]=z.substring(i,i+1);BZHI.put(n,a);}
    private static void addPalace(String p,String w,String[][]a){for(String[]x:a)HEX.put(x[0],new Hex(x[0],x[1],x[2],p,w,Integer.parseInt(x[3]),x[4]));}
    private static int indexOf(String[]a,String s){for(int i=0;i<a.length;i++)if(a[i].equals(s))return i;return 0;} private static boolean contains(String[]a,String s){for(String x:a)if(x.equals(s))return true;return false;} private static int floorMod(int a,int b){int r=a%b;return r<0?r+b:r;}
    private static String join(List<String>a,String sep){StringBuilder s=new StringBuilder();for(int i=0;i<a.size();i++){if(i>0)s.append(sep);s.append(a.get(i));}return s.toString();}
}
