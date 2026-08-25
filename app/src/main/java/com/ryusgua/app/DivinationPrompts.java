package com.ryusgua.app;

/** Keeps Liuyao and Tarot AI identities, output policy and user facts separate. */
final class DivinationPrompts {
    static final int TARGET_MIN_CHARS = 700;
    static final int TARGET_MAX_CHARS = 1000;
    static final int HARD_MAX_CHARS = 1200;

    private static final String SAFETY =
            "最终回答只输出面向用户的结论，不得展示、复述或暗示内部思考过程、分析草稿、reasoning、thinking 或 <think> 标签。"
            + "语言自然、克制、具体，不故弄玄虚，不使用绝对预言式措辞。医疗、法律、投资等高风险事项只作文化与娱乐参考，不替代专业判断。"
            + lengthPolicy();

    private static final String LIUYAO_SYSTEM =
            "你是“柳之卦”的《周易》与六爻解卦助手。只解释用户已经完成的本卦、动爻、之卦和纳甲盘面，不重新起卦，不改动排盘，不在用户未指定时擅自断定用神。"
            + "回答依次包含：占问回应、卦意、六爻盘面、动变关系、可执行建议；没有占问时明确按整体卦意阅读。避免逐段复述原始盘面。"
            + SAFETY;

    private static final String TAROT_SYSTEM =
            "你是“柳之卦”的塔罗牌阵解读助手。只解释用户已经抽定的牌、正逆位与牌位，不重新抽牌、不替换牌阵，也不把塔罗包装成确定未来的预言。"
            + "回答依次包含：占问回应、整体主题、逐位关系、牌间呼应或张力、可执行建议。逆位表示受阻、过量、内化或需要调整，不一律解释成坏结果。"
            + SAFETY;

    private DivinationPrompts() {}

    static String system(boolean tarot) {
        return tarot ? TAROT_SYSTEM : LIUYAO_SYSTEM;
    }

    static String liuYaoUser(String question, String result, String board,
                             String knowledge, String offline) {
        return questionBlock(question)
                + result + "\n\n" + board + "\n\n" + knowledge
                + "\n\n【本机离线解卦】\n" + offline
                + "\n\n请以上述占问（如有）、卦象、六爻排盘事实与本机术理摘要为依据。"
                + "占问只是解读语境，不得据此篡改卦象或擅自补充不存在的事实。";
    }

    static String tarotUser(String question, String facts, String offline) {
        return questionBlock(question)
                + "【柳之卦 · 塔罗】\n" + facts
                + "\n【本机离线合参】\n" + offline
                + "\n\n请围绕占问（如有）解释牌位之间的关系，核对大牌比例、花色重心和正逆位张力。"
                + "不要把关键词机械串联，也不要添加本局未出现的牌。";
    }

    private static String questionBlock(String question) {
        String safe = question == null ? "" : question.trim();
        return safe.isEmpty()
                ? "【占问】未写下具体问题；按整体主题解读。\n\n"
                : "【占问】" + safe + "\n\n";
    }

    private static String lengthPolicy() {
        return "正文建议控制在" + TARGET_MIN_CHARS + "至" + TARGET_MAX_CHARS
                + "个中文字符，信息确有需要时最多不超过" + HARD_MAX_CHARS + "个中文字符。";
    }
}
