package com.ryusgua.app;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Three-coin I Ching casting engine.
 * Lines are stored bottom-to-top, matching traditional six-line notation.
 */
public final class HexagramEngine {
    private HexagramEngine() {}

    private static final SecureRandom RNG = new SecureRandom();

    // Trigram order used by the King Wen lookup matrix below.
    // 0 乾, 1 兑, 2 离, 3 震, 4 巽, 5 坎, 6 艮, 7 坤
    private static final int[] TRIGRAM_CODES = {7, 3, 5, 1, 6, 2, 4, 0};
    private static final String[] TRIGRAM_NAMES = {"乾", "兑", "离", "震", "巽", "坎", "艮", "坤"};

    // Rows = lower trigram, columns = upper trigram, both in TRIGRAM_NAMES order.
    private static final int[][] KING_WEN = {
            {1, 43, 14, 34, 9, 5, 26, 11},
            {10, 58, 38, 54, 61, 60, 41, 19},
            {13, 49, 30, 55, 37, 63, 22, 36},
            {25, 17, 21, 51, 42, 3, 27, 24},
            {44, 28, 50, 32, 57, 48, 18, 46},
            {6, 47, 64, 40, 59, 29, 4, 7},
            {33, 31, 56, 62, 53, 39, 52, 15},
            {12, 45, 35, 16, 20, 8, 23, 2}
    };

    // Index 0 intentionally blank; index = King Wen number.
    private static final String[] NAMES = {
            "",
            "乾为天", "坤为地", "水雷屯", "山水蒙", "水天需", "天水讼", "地水师", "水地比",
            "风天小畜", "天泽履", "地天泰", "天地否", "天火同人", "火天大有", "地山谦", "雷地豫",
            "泽雷随", "山风蛊", "地泽临", "风地观", "火雷噬嗑", "山火贲", "山地剥", "地雷复",
            "天雷无妄", "山天大畜", "山雷颐", "泽风大过", "坎为水", "离为火", "泽山咸", "雷风恒",
            "天山遁", "雷天大壮", "火地晋", "地火明夷", "风火家人", "火泽睽", "水山蹇", "雷水解",
            "山泽损", "风雷益", "泽天夬", "天风姤", "泽地萃", "地风升", "泽水困", "水风井",
            "泽火革", "火风鼎", "震为雷", "艮为山", "风山渐", "雷泽归妹", "雷火丰", "火山旅",
            "巽为风", "兑为泽", "风水涣", "水泽节", "风泽中孚", "雷山小过", "水火既济", "火水未济"
    };

    /** Returns 6, 7, 8, or 9 using three fair coins (2/3 points per coin). */
    public static int castLine() {
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += RNG.nextBoolean() ? 3 : 2;
        }
        return sum;
    }

    public static boolean isYang(int lineValue) {
        return lineValue == 7 || lineValue == 9;
    }

    public static boolean isMoving(int lineValue) {
        return lineValue == 6 || lineValue == 9;
    }

    public static boolean changedYang(int lineValue) {
        if (lineValue == 6) return true;
        if (lineValue == 9) return false;
        return isYang(lineValue);
    }

    public static Hexagram lookup(int[] lines, boolean changed) {
        if (lines == null || lines.length != 6) {
            throw new IllegalArgumentException("Six line values are required");
        }
        int lowerCode = 0;
        int upperCode = 0;
        for (int i = 0; i < 3; i++) {
            boolean yang = changed ? changedYang(lines[i]) : isYang(lines[i]);
            if (yang) lowerCode |= (1 << i);
        }
        for (int i = 3; i < 6; i++) {
            boolean yang = changed ? changedYang(lines[i]) : isYang(lines[i]);
            if (yang) upperCode |= (1 << (i - 3));
        }
        int lowerIndex = trigramIndex(lowerCode);
        int upperIndex = trigramIndex(upperCode);
        int number = KING_WEN[lowerIndex][upperIndex];
        return new Hexagram(number, NAMES[number], TRIGRAM_NAMES[upperIndex], TRIGRAM_NAMES[lowerIndex]);
    }

    public static List<String> movingLineLabels(int[] lines) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (!isMoving(lines[i])) continue;
            String yinYang = isYang(lines[i]) ? "九" : "六";
            switch (i) {
                case 0: labels.add("初" + yinYang); break;
                case 1: labels.add(yinYang + "二"); break;
                case 2: labels.add(yinYang + "三"); break;
                case 3: labels.add(yinYang + "四"); break;
                case 4: labels.add(yinYang + "五"); break;
                case 5: labels.add("上" + yinYang); break;
            }
        }
        return labels;
    }

    public static String lineText(int value) {
        switch (value) {
            case 6: return "老阴 · 动";
            case 7: return "少阳";
            case 8: return "少阴";
            case 9: return "老阳 · 动";
            default: return "";
        }
    }

    private static int trigramIndex(int code) {
        for (int i = 0; i < TRIGRAM_CODES.length; i++) {
            if (TRIGRAM_CODES[i] == code) return i;
        }
        throw new IllegalArgumentException("Invalid trigram code: " + code);
    }

    public static final class Hexagram {
        public final int number;
        public final String name;
        public final String upper;
        public final String lower;

        Hexagram(int number, String name, String upper, String lower) {
            this.number = number;
            this.name = name;
            this.upper = upper;
            this.lower = lower;
        }

        public String compact() {
            return "第" + number + "卦 · " + name;
        }
    }
}
