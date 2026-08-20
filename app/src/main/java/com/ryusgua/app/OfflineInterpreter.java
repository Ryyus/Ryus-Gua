package com.ryusgua.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, fully offline I Ching reading.
 *
 * The app deliberately keeps this layer independent from any network/model provider:
 * it selects the relevant classical text from the cast itself and adds a small amount
 * of transparent rule-based guidance. This is a fallback/first-pass reading, not a
 * claim that one divination school is uniquely authoritative.
 */
final class OfflineInterpreter {
    private OfflineInterpreter() {}

    static String interpret(int[] lines, ZhouYiRepository zhouYi) {
        HexagramEngine.Hexagram base = HexagramEngine.lookup(lines, false);
        HexagramEngine.Hexagram changed = HexagramEngine.lookup(lines, true);
        ZhouYiRepository.TextEntry baseText = zhouYi.get(base.name);
        ZhouYiRepository.TextEntry changedText = zhouYi.get(changed.name);

        List<Integer> moving = movingIndexes(lines);
        StringBuilder out = new StringBuilder(1200);

        out.append("【卦意】\n");
        out.append("本卦为第").append(base.number).append("卦·").append(base.name)
                .append("（上").append(base.upper).append("下").append(base.lower).append("）。")
                .append("下卦").append(base.lower).append("偏向").append(trigramMeaning(base.lower))
                .append("，上卦").append(base.upper).append("偏向").append(trigramMeaning(base.upper))
                .append("。可把下卦看作事情的起点与内在条件，上卦看作外在表现与后续方向。\n");
        out.append("卦辞：").append(baseText.guaCi).append("\n\n");

        out.append("【取用】\n");
        appendSelection(out, lines, moving, base, changed, zhouYi, baseText, changedText);
        out.append("\n");

        if (!moving.isEmpty()) {
            out.append("【之卦】\n");
            out.append("变化后为第").append(changed.number).append("卦·").append(changed.name)
                    .append("（上").append(changed.upper).append("下").append(changed.lower).append("）。")
                    .append("之卦更适合当作变化后的趋势提示，而不是已经发生的确定结果。\n")
                    .append("之卦卦辞：").append(changedText.guaCi).append("\n\n");
        }

        out.append("【建议】\n");
        out.append(adviceForMovingCount(moving.size())).append(" ")
                .append("同时留意“").append(base.lower).append("”所提示的")
                .append(trigramAction(base.lower)).append("，以及“").append(base.upper).append("”所提示的")
                .append(trigramAction(base.upper)).append("。\n\n");
        out.append("离线解卦由本机规则生成，无需网络。取用规则采用常见传统变爻法以保证结果稳定，")
                .append("不同易学流派可能有不同解释；内容仅供传统文化阅读与娱乐参考。");
        return out.toString();
    }

    private static void appendSelection(StringBuilder out,
                                        int[] lines,
                                        List<Integer> moving,
                                        HexagramEngine.Hexagram base,
                                        HexagramEngine.Hexagram changed,
                                        ZhouYiRepository zhouYi,
                                        ZhouYiRepository.TextEntry baseText,
                                        ZhouYiRepository.TextEntry changedText) {
        int count = moving.size();
        if (count == 0) {
            out.append("本卦无动爻，离线规则以本卦卦辞为主。当前更适合把重点放在本卦整体主题，而不是寻找额外的变化信号。\n");
            return;
        }

        if (count == 1) {
            int i = moving.get(0);
            out.append("一爻动，以本卦这一动爻为主要提示：\n");
            appendBaseLine(out, lines, i, base, zhouYi, true);
            return;
        }

        if (count == 2) {
            out.append("两爻动，同时参考本卦两条动爻；离线规则以上位动爻作为后续变化的重点提醒：\n");
            for (int i : moving) appendBaseLine(out, lines, i, base, zhouYi, i == moving.get(moving.size() - 1));
            return;
        }

        if (count == 3) {
            out.append("三爻动，变化与稳定各半，因此本卦与之卦并看：本卦偏现状，之卦偏趋势。\n")
                    .append("本卦卦辞：").append(baseText.guaCi).append("\n")
                    .append("之卦卦辞：").append(changedText.guaCi).append("\n");
            return;
        }

        List<Integer> unchanged = new ArrayList<>();
        for (int i = 0; i < 6; i++) if (!HexagramEngine.isMoving(lines[i])) unchanged.add(i);

        if (count == 4) {
            out.append("四爻动，变化已占多数，离线规则转而参考之卦中两条不变爻，并以下位不变爻为主要落点：\n");
            for (int i : unchanged) appendChangedLine(out, lines, i, changed, zhouYi, i == unchanged.get(0));
            return;
        }

        if (count == 5) {
            int i = unchanged.get(0);
            out.append("五爻动，整体格局接近转换，离线规则以之卦中唯一不变爻为主要提示：\n");
            appendChangedLine(out, lines, i, changed, zhouYi, true);
            return;
        }

        // Six moving lines.
        if ("乾为天".equals(base.name) && baseText.yao.containsKey("用九")) {
            out.append("六爻皆动且本卦为乾，取“用九”：").append(baseText.yao.get("用九")).append("\n");
        } else if ("坤为地".equals(base.name) && baseText.yao.containsKey("用六")) {
            out.append("六爻皆动且本卦为坤，取“用六”：").append(baseText.yao.get("用六")).append("\n");
        } else {
            out.append("六爻皆动，整体转换最强，离线规则以之卦卦辞为主：")
                    .append(changedText.guaCi).append("\n");
        }
    }

    private static void appendBaseLine(StringBuilder out, int[] lines, int i,
                                       HexagramEngine.Hexagram base, ZhouYiRepository zhouYi,
                                       boolean primary) {
        boolean yang = HexagramEngine.isYang(lines[i]);
        String label = ZhouYiRepository.traditionalLineLabel(i, yang);
        out.append(primary ? "★ " : "· ").append(label).append("：")
                .append(zhouYi.lineText(base.name, i, yang)).append("\n");
    }

    private static void appendChangedLine(StringBuilder out, int[] lines, int i,
                                          HexagramEngine.Hexagram changed, ZhouYiRepository zhouYi,
                                          boolean primary) {
        boolean yang = HexagramEngine.changedYang(lines[i]);
        String label = ZhouYiRepository.traditionalLineLabel(i, yang);
        out.append(primary ? "★ " : "· ").append(label).append("：")
                .append(zhouYi.lineText(changed.name, i, yang)).append("\n");
    }

    private static List<Integer> movingIndexes(int[] lines) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) if (HexagramEngine.isMoving(lines[i])) out.add(i);
        return out;
    }

    private static String adviceForMovingCount(int count) {
        switch (count) {
            case 0:
                return "事势相对稳定，先理解并守住本卦的主轴，不必为了求变而强行制造变化。";
            case 1:
                return "把注意力集中在唯一动爻所指向的环节，先处理这一处，再观察事情是否自然转向。";
            case 2:
                return "同时存在两个变化点，可先处理较低层、较早出现的因素，再留意上位动爻代表的后续。";
            case 3:
                return "当前处在明显转折区间，不宜只看眼前；把本卦当作现状、之卦当作趋势相互校验。";
            case 4:
                return "变化已经占多数，判断重心应逐渐转向之卦，并把两条不变爻当作变化中的边界条件。";
            case 5:
                return "局势接近整体转换，最重要的是守住之卦唯一不变爻所代表的核心原则。";
            default:
                return "六爻皆动表示整体转换最强，旧格局的参考权重下降，应以变化后的整体方向为主。";
        }
    }

    private static String trigramMeaning(String trigram) {
        switch (trigram) {
            case "乾": return "刚健、主动与原则";
            case "兑": return "沟通、悦纳与交换";
            case "离": return "明辨、显现与依附";
            case "震": return "启动、行动与突然变化";
            case "巽": return "渐进、进入与柔性渗透";
            case "坎": return "风险、反复与谨慎涉险";
            case "艮": return "停止、边界与收束";
            case "坤": return "承载、顺势与配合";
            default: return "观察与调整";
        }
    }

    private static String trigramAction(String trigram) {
        switch (trigram) {
            case "乾": return "保持主动但不过度逞强";
            case "兑": return "把话说清楚并保留协商空间";
            case "离": return "先看清事实与关系再行动";
            case "震": return "为变化预留反应空间并及时启动";
            case "巽": return "用渐进方式推进而非硬碰硬";
            case "坎": return "先识别风险、留后手再前进";
            case "艮": return "知道何时停止并设清楚边界";
            case "坤": return "顺势承接、配合现实条件";
            default: return "保持观察并适时调整";
        }
    }
}
