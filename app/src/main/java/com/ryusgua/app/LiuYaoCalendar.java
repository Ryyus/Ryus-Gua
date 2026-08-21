package com.ryusgua.app;

import java.util.Calendar;
import java.util.Locale;

/**
 * Local calendar helpers for 六爻排盘.
 *
 * Day pillar is exact for Gregorian dates by anchoring 2000-01-07 = 甲子日.
 * Month branch follows the traditional jie-month boundary (小寒/立春/惊蛰/...)
 * using the common 21st-century solar-term approximation. No network is used.
 */
final class LiuYaoCalendar {
    private static final String GAN = "甲乙丙丁戊己庚辛壬癸";
    private static final String ZHI = "子丑寅卯辰巳午未申酉戌亥";
    private static final int ANCHOR_JDN = gregorianJdn(2000, 1, 7); // 甲子日

    static final class Pillars {
        final int year, month, day, hour, minute;
        final String monthZhi;
        final String dayGanZhi;

        Pillars(int year, int month, int day, int hour, int minute, String monthZhi, String dayGanZhi) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.monthZhi = monthZhi;
            this.dayGanZhi = dayGanZhi;
        }

        String dateTimeText() {
            return String.format(Locale.CHINA, "%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute);
        }
    }

    private LiuYaoCalendar() {}

    static Pillars at(long epochMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMs > 0L ? epochMs : System.currentTimeMillis());
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        int h = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        return new Pillars(y, m, d, h, min, monthBranch(y, m, d), dayGanzhi(y, m, d));
    }

    static String dayGanzhi(int year, int month, int day) {
        int delta = gregorianJdn(year, month, day) - ANCHOR_JDN;
        int idx = floorMod(delta, 60);
        return "" + GAN.charAt(idx % 10) + ZHI.charAt(idx % 12);
    }

    /**
     * Month branch by 节 boundary. 寅月 starts at 立春; subsequent months start at
     * 惊蛰、清明、立夏、芒种、小暑、立秋、白露、寒露、立冬、大雪、小寒.
     */
    static String monthBranch(int year, int month, int day) {
        // Jan before 小寒 remains 子月, Jan after 小寒 is 丑月.
        if (month == 1) return day >= termDay(year, 1) ? "丑" : "子";
        if (month == 2) return day >= termDay(year, 2) ? "寅" : "丑";
        if (month == 3) return day >= termDay(year, 3) ? "卯" : "寅";
        if (month == 4) return day >= termDay(year, 4) ? "辰" : "卯";
        if (month == 5) return day >= termDay(year, 5) ? "巳" : "辰";
        if (month == 6) return day >= termDay(year, 6) ? "午" : "巳";
        if (month == 7) return day >= termDay(year, 7) ? "未" : "午";
        if (month == 8) return day >= termDay(year, 8) ? "申" : "未";
        if (month == 9) return day >= termDay(year, 9) ? "酉" : "申";
        if (month == 10) return day >= termDay(year, 10) ? "戌" : "酉";
        if (month == 11) return day >= termDay(year, 11) ? "亥" : "戌";
        return day >= termDay(year, 12) ? "子" : "亥";
    }

    // Common 2000-2099 approximation: day = floor(Y*0.2422 + C) - floor((Y-1)/4).
    private static int termDay(int year, int month) {
        int yy = floorMod(year, 100);
        double c;
        switch (month) {
            case 1: c = 5.4055; break; // 小寒
            case 2: c = 3.87; break;   // 立春
            case 3: c = 5.63; break;   // 惊蛰
            case 4: c = 4.81; break;   // 清明
            case 5: c = 5.52; break;   // 立夏
            case 6: c = 5.678; break;  // 芒种
            case 7: c = 7.108; break;  // 小暑
            case 8: c = 7.5; break;    // 立秋
            case 9: c = 7.646; break;  // 白露
            case 10: c = 8.318; break; // 寒露
            case 11: c = 7.438; break; // 立冬
            default: c = 7.18; break;  // 大雪
        }
        return (int) Math.floor(yy * 0.2422 + c) - (int) Math.floor((yy - 1) / 4.0);
    }

    private static int gregorianJdn(int y, int m, int d) {
        int a = (14 - m) / 12;
        int yy = y + 4800 - a;
        int mm = m + 12 * a - 3;
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045;
    }

    private static int floorMod(int a, int b) {
        int r = a % b;
        return r < 0 ? r + b : r;
    }
}
