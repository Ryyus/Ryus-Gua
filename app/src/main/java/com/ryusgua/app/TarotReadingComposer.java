package com.ryusgua.app;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Deterministic local synthesis across positions, suits, arcana and orientations. */
final class TarotReadingComposer {
    private TarotReadingComposer() {}

    static String compose(TarotDeck.Draw[] draws, TarotDeck.Spread spread, String question) {
        if (draws == null || spread == null || draws.length != spread.size()) return "牌阵尚未完成。";
        StringBuilder out = new StringBuilder("【").append(spread.name).append("】\n");
        String safeQuestion = question == null ? "" : question.trim();
        if (!safeQuestion.isEmpty()) out.append("占问：").append(safeQuestion).append("\n");

        int majors = 0, reversed = 0;
        LinkedHashMap<String, Integer> suits = new LinkedHashMap<>();
        suits.put("权杖", 0); suits.put("圣杯", 0); suits.put("宝剑", 0); suits.put("星币", 0);
        for (int i = 0; i < draws.length; i++) {
            TarotDeck.Draw draw = draws[i];
            if (draw.reversed) reversed++;
            if (draw.card.suit.isEmpty()) majors++;
            else if (suits.containsKey(draw.card.suit)) suits.put(draw.card.suit, suits.get(draw.card.suit) + 1);
            out.append("\n").append(spread.positions[i]).append(" · ")
                    .append(draw.card.name).append(" · ").append(draw.orientation()).append("\n")
                    .append(draw.card.keywords(draw.reversed)).append("。").append(draw.card.meaning(draw.reversed)).append("\n")
                    .append(positionLink(spread, i, draw)).append("\n");
        }

        out.append("\n【合参】\n");
        if (draws.length == 1) {
            TarotDeck.Draw only = draws[0];
            out.append("这张牌把重心收在“").append(only.card.keywords(only.reversed)).append("”。")
                    .append(only.reversed ? "先辨认哪里受阻、过量或被压回内在，再决定是否行动。" : "它更适合被当作当下的观察镜面，而非事件结论。")
                    .append("\n行动落点：").append(only.card.advice);
            return disclaimer(out);
        }

        if (majors > 0) {
            out.append("本局有").append(majors).append("张大阿卡那，说明问题不只停留在一件小事，也牵动较整体的阶段、选择或价值取向；")
                    .append(majors == draws.length ? "现实行动仍需另行落地验证。" : "其余小阿卡那则指出可观察的现实入口。").append("\n");
        } else {
            out.append("本局全部为小阿卡那，重心更接近日常行动、关系互动、思考方式与现实资源，可从具体事件验证。\n");
        }

        String dominant = dominantSuit(suits, draws.length - majors);
        if (!dominant.isEmpty()) out.append(suitSentence(dominant)).append("\n");
        else out.append("四种现实力量没有形成单一压倒性重心，应优先比较各牌位之间的差异，而不是只抓住某一张牌。\n");

        if (reversed == 0) out.append("牌面均为正位，能量较易外显，但顺畅不等于无需核对边界与后果。\n");
        else if (reversed == draws.length) out.append("牌面均为逆位，当前更像整理内在阻滞与失衡，不宜急着把压力解释成外界定论。\n");
        else out.append("正逆位并存：有些力量能够直接使用，另一些仍在受阻、过量或内化；先处理最影响行动的那一处。\n");

        out.append(sequenceSentence(draws, spread));
        out.append("\n行动落点：").append(actionCard(draws, spread).card.advice);
        return disclaimer(out);
    }

    private static String positionLink(TarotDeck.Spread spread, int index, TarotDeck.Draw draw) {
        String hint = spread.hints[index];
        return hint + (draw.reversed
                ? " 此处先检查延迟、回避、过量或内在化的表现。"
                : " 此处可观察这股力量怎样落实为选择与行动。");
    }

    private static String dominantSuit(Map<String, Integer> suits, int minorCount) {
        if (minorCount < 2) return "";
        String best = ""; int max = 0, second = 0;
        for (Map.Entry<String, Integer> entry : suits.entrySet()) {
            int value = entry.getValue();
            if (value > max) { second = max; max = value; best = entry.getKey(); }
            else if (value > second) second = value;
        }
        return max >= 2 && max > second ? best : "";
    }

    private static String suitSentence(String suit) {
        switch (suit) {
            case "权杖": return "权杖形成重心：关键在主动性、创造冲动与行动节奏，需区分热情和冒进。";
            case "圣杯": return "圣杯形成重心：情感、关系与接纳方式正在主导判断，需同时核对真实感受与边界。";
            case "宝剑": return "宝剑形成重心：信息、沟通与冲突结构最值得检查，先澄清事实再处理立场。";
            case "星币": return "星币形成重心：资源、身体、时间与长期可持续性比一时情绪更具决定性。";
            default: return "";
        }
    }

    private static String sequenceSentence(TarotDeck.Draw[] draws, TarotDeck.Spread spread) {
        if (draws.length == 3) {
            return "三个位次应连成一条变化线：从“" + draws[0].card.keywords(draws[0].reversed)
                    + "”经过“" + draws[1].card.keywords(draws[1].reversed)
                    + "”，正趋向“" + draws[2].card.keywords(draws[2].reversed)
                    + "”。趋向描述当前惯性，不是不可改变的结果。";
        }
        if (draws.length == 4) {
            return "四象不按时间先后排列。把“" + spread.positions[0] + "”与“" + spread.positions[2]
                    + "”对照行动和判断，再用“" + spread.positions[1] + "”与“" + spread.positions[3]
                    + "”核对感受和现实承载；最不协调的一象就是优先调整处。";
        }
        return String.format(Locale.CHINA, "本局共%d个牌位；先找重复主题，再检查相邻牌位是否互相支持或牵制。", draws.length);
    }

    private static TarotDeck.Draw actionCard(TarotDeck.Draw[] draws, TarotDeck.Spread spread) {
        if (draws.length == 3) return draws[1];
        if (draws.length == 4) return draws[3];
        return draws[draws.length - 1];
    }

    private static String disclaimer(StringBuilder out) {
        return out.append("\n\n塔罗用于自我观察与娱乐参考，不替代医疗、法律、财务等专业判断；牌面是可讨论的叙事，不是固定结局。")
                .toString();
    }
}
