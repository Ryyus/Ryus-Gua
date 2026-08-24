package com.ryusgua.app;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete offline 78-card tarot deck and a restrained three-card reading. */
final class TarotDeck {
    static final class Card {
        final int id;
        final String name;
        final String family;
        final String upright;
        final String reversed;

        Card(int id, String name, String family, String upright, String reversed) {
            this.id = id;
            this.name = name;
            this.family = family;
            this.upright = upright;
            this.reversed = reversed;
        }

        String keyword(boolean isReversed) { return isReversed ? reversed : upright; }
    }

    static final class Draw {
        final Card card;
        final boolean reversed;

        Draw(Card card, boolean reversed) {
            this.card = card;
            this.reversed = reversed;
        }

        String orientation() { return reversed ? "逆位" : "正位"; }
    }

    private static final String[] POSITIONS = {"缘起", "此刻", "趋向"};
    private static final String[] POSITION_HINTS = {
            "回看事情如何走到这里，以及仍在影响你的旧线索。",
            "辨认当下最需要正视的力量、情绪与选择。",
            "观察延续当前倾向时可能展开的方向，而非固定结局。"
    };
    private static final List<Card> CARDS = buildCards();
    private static final SecureRandom RANDOM = new SecureRandom();

    private TarotDeck() {}

    static Draw[] drawThree() {
        List<Card> shuffled = new ArrayList<>(CARDS);
        Collections.shuffle(shuffled, RANDOM);
        Draw[] out = new Draw[3];
        for (int i = 0; i < out.length; i++) out[i] = new Draw(shuffled.get(i), RANDOM.nextBoolean());
        return out;
    }

    static Card byId(int id) {
        if (id < 0 || id >= CARDS.size()) return CARDS.get(0);
        return CARDS.get(id);
    }

    static String reading(Draw[] draws) {
        if (draws == null || draws.length != 3) return "尚未完成三牌之阵。";
        StringBuilder out = new StringBuilder();
        out.append("【三牌之阵】\n");
        for (int i = 0; i < 3; i++) {
            Draw draw = draws[i];
            out.append("\n").append(POSITIONS[i]).append(" · ")
                    .append(draw.card.name).append(" · ").append(draw.orientation()).append("\n")
                    .append(draw.card.keyword(draw.reversed)).append("。\n")
                    .append(POSITION_HINTS[i]).append("\n");
        }
        out.append("\n【合参】\n");
        out.append("这组三牌从“").append(draws[0].card.keyword(draws[0].reversed))
                .append("”走向“").append(draws[1].card.keyword(draws[1].reversed))
                .append("”，并把注意力引向“").append(draws[2].card.keyword(draws[2].reversed))
                .append("”。先处理当下能确认的一件事，再观察局势是否随之改变。")
                .append("\n\n塔罗用于自我观察与娱乐参考，不替代医疗、法律、财务或其他专业判断。")
                .append("牌面呈现的是一种可能的叙事，不是不可改变的结局。");
        return out.toString();
    }

    private static List<Card> buildCards() {
        ArrayList<Card> cards = new ArrayList<>(78);
        String[] majorNames = {
                "0 愚者", "I 魔术师", "II 女祭司", "III 皇后", "IV 皇帝", "V 教皇",
                "VI 恋人", "VII 战车", "VIII 力量", "IX 隐者", "X 命运之轮", "XI 正义",
                "XII 倒吊人", "XIII 死神", "XIV 节制", "XV 恶魔", "XVI 高塔", "XVII 星星",
                "XVIII 月亮", "XIX 太阳", "XX 审判", "XXI 世界"
        };
        String[] majorUp = {
                "自由启程、信任未知", "主动创造、资源到位", "静观直觉、秘密浮现", "丰盛滋养、关系生长",
                "秩序边界、承担责任", "传统指引、共同信念", "价值选择、真诚连结", "意志推进、掌握方向",
                "温柔勇气、内在稳定", "独处求索、沉静洞察", "周期转动、机会更替", "公平衡量、因果清晰",
                "暂停换位、放下执念", "结束转化、告别旧章", "调和节奏、适度修复", "欲望束缚、看见依赖",
                "结构震动、真相破壁", "希望疗愈、重新信任", "迷雾梦境、情绪暗潮", "坦然明亮、生命舒展",
                "回应召唤、重新评估", "完成整合、阶段圆满"
        };
        String[] majorRev = {
                "冒进失序、逃避后果", "能力分散、操控表象", "忽略直觉、信息遮蔽", "过度付出、成长受阻",
                "控制僵化、边界失衡", "教条束缚、质疑旧规", "关系失衡、价值摇摆", "方向分裂、急于求成",
                "自我怀疑、情绪失控", "封闭退缩、孤立过久", "抗拒变化、重复旧环", "偏见失衡、责任回避",
                "无效牺牲、停滞拖延", "抗拒结束、旧事未放", "节奏失调、消耗过度", "识破束缚、尝试松绑",
                "余震未止、勉强维系", "希望微弱、信心待修", "焦虑投射、真相未明", "热情受阻、过度乐观",
                "拒绝回应、旧账未清", "尚差一步、整合未竟"
        };
        for (int i = 0; i < majorNames.length; i++) cards.add(new Card(i, majorNames[i], "大阿卡那", majorUp[i], majorRev[i]));

        String[] suits = {"权杖", "圣杯", "宝剑", "星币"};
        String[] suitThemes = {"行动、创造与意志", "情感、关系与直觉", "思考、沟通与冲突", "现实、资源与身体"};
        String[] ranks = {"王牌", "二", "三", "四", "五", "六", "七", "八", "九", "十", "侍从", "骑士", "王后", "国王"};
        String[] rankUp = {
                "新的种子正在出现", "需要权衡并做出选择", "协作让成果开始成形", "稳定之后需要停看",
                "摩擦暴露真正的缺口", "交换、回归与重新平衡", "守住立场并接受检验", "节奏加快、事情持续推进",
                "接近成果也需保持清醒", "阶段抵达承载上限", "消息、好奇与学习开启", "直接行动并追随目标",
                "成熟地照料并承接能量", "稳定掌舵并承担结果"
        };
        String[] rankRev = {
                "开端迟疑或能量尚未落地", "摇摆过久使选择失焦", "协作不顺或成果被高估", "停滞、防御或不愿松手",
                "冲突内耗且重点偏移", "给予失衡或旧事难回", "防线松动或过度戒备", "延误、急躁或方向错位",
                "焦虑放大了临门压力", "负担过重需要主动减量", "消息含混或经验不足", "冲动冒进、承诺难以持续",
                "照料失衡、情绪或资源透支", "控制过度、责任使用失当"
        };
        int id = 22;
        for (int suit = 0; suit < suits.length; suit++) {
            for (int rank = 0; rank < ranks.length; rank++) {
                cards.add(new Card(id++, suits[suit] + ranks[rank], suits[suit],
                        suitThemes[suit] + "：" + rankUp[rank],
                        suitThemes[suit] + "：" + rankRev[rank]));
            }
        }
        return Collections.unmodifiableList(cards);
    }
}
