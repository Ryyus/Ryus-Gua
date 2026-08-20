from pathlib import Path
import re

p = Path('app/src/main/java/com/ryusgua/app/MainActivity.java')
s = p.read_text(encoding='utf-8')

old = '''        private static final String ANIM_CLASSIC = "classic";
        private static final String ANIM_PHYSICS = "physics";
        private String coinAnimationMode = ANIM_CLASSIC;
'''
new = '''        private static final String ANIM_CLASSIC = "classic";
        private static final String ANIM_PHYSICS = "physics";
        private static final String BOOT_CLASSIC = "classic";
        private static final String BOOT_A = "electronic";
        private static final String BOOT_B = "ancient";
        private static final String BOOT_C = "zhouyi";
        private static final String BOOT_D = "ritual";
        private static final String BOOT_E = "terminal";
        private static final String BOOT_F = "sixline";
        private static final String[] BOOT_STYLES = {BOOT_CLASSIC, BOOT_A, BOOT_B, BOOT_C, BOOT_D, BOOT_E, BOOT_F};
        private static final String[] BOOT_STYLE_LABELS = {
                "经典初版 · 柳/卦 + 进度条",
                "A · 电子法器启动",
                "B · 古意极简",
                "C · 周易原典",
                "D · 卜筮仪式",
                "E · 终端自检",
                "F · 六爻启动序列"
        };
        private String coinAnimationMode = ANIM_CLASSIC;
        private String bootStyle = BOOT_CLASSIC;
'''
assert old in s
s = s.replace(old, new, 1)

old = '''            coinAnimationMode = pref.getString("coin_animation", ANIM_CLASSIC);
            if (!ANIM_PHYSICS.equals(coinAnimationMode)) coinAnimationMode = ANIM_CLASSIC;
'''
new = '''            coinAnimationMode = pref.getString("coin_animation", ANIM_CLASSIC);
            if (!ANIM_PHYSICS.equals(coinAnimationMode)) coinAnimationMode = ANIM_CLASSIC;
            bootStyle = pref.getString("boot_style", BOOT_CLASSIC);
            boolean validBoot = false;
            for (String style : BOOT_STYLES) if (style.equals(bootStyle)) { validBoot = true; break; }
            if (!validBoot) bootStyle = BOOT_CLASSIC;
'''
assert old in s
s = s.replace(old, new, 1)

old = '''                    .putBoolean("vertical_flip", verticalFlipEnabled)
                    .putString("coin_animation", coinAnimationMode)
                    .apply();
'''
new = '''                    .putBoolean("vertical_flip", verticalFlipEnabled)
                    .putString("coin_animation", coinAnimationMode)
                    .putString("boot_style", bootStyle)
                    .apply();
'''
assert old in s
s = s.replace(old, new, 1)

pattern = re.compile(r'''        private void startBootAnimation\(\) \{.*?\n        \}\n\n        private WindowInsets applySafeInsets''', re.S)
replacement = '''        private void startBootAnimation() {
            final long started = SystemClock.uptimeMillis();
            final float durationMs = bootDurationMs();
            handler.postDelayed(() -> {
                if (soundEnabled) audio.boot();
                pulse(18, 90);
            }, 120L);
            Runnable animator = new Runnable() {
                @Override public void run() {
                    float t = Math.min(1f, (SystemClock.uptimeMillis() - started) / durationMs);
                    float inv = 1f - t;
                    bootSweep = 1f - inv * inv * inv;
                    bootStep = Math.min(6, (int) (t * 7f));
                    postInvalidateOnAnimation();
                    if (t < 1f) {
                        postOnAnimation(this);
                    } else {
                        state = State.IDLE;
                        pulse(28, 120);
                        postInvalidateOnAnimation();
                    }
                }
            };
            postOnAnimation(animator);
        }

        private float bootDurationMs() {
            if (BOOT_CLASSIC.equals(bootStyle)) return 1250f;
            if (BOOT_A.equals(bootStyle)) return 2300f;
            if (BOOT_B.equals(bootStyle)) return 2050f;
            if (BOOT_C.equals(bootStyle)) return 2450f;
            if (BOOT_D.equals(bootStyle)) return 2250f;
            if (BOOT_E.equals(bootStyle)) return 2600f;
            if (BOOT_F.equals(bootStyle)) return 2400f;
            return 1250f;
        }

        private int bootStyleIndex() {
            for (int i = 0; i < BOOT_STYLES.length; i++) if (BOOT_STYLES[i].equals(bootStyle)) return i;
            return 0;
        }

        private String bootStyleShortLabel() {
            switch (bootStyle) {
                case BOOT_A: return "电子法器";
                case BOOT_B: return "古意极简";
                case BOOT_C: return "周易原典";
                case BOOT_D: return "卜筮仪式";
                case BOOT_E: return "终端自检";
                case BOOT_F: return "六爻序列";
                default: return "经典初版";
            }
        }

        private WindowInsets applySafeInsets'''
s, n = pattern.subn(replacement, s, count=1)
assert n == 1

pattern = re.compile(r'''        private void drawBoot\(Canvas c, float w, float h\) \{.*?\n        \}\n\n        private void drawHeader''', re.S)
replacement = '''        private void drawBoot(Canvas c, float w, float h) {
            switch (bootStyle) {
                case BOOT_A: drawBootElectronic(c, w, h); break;
                case BOOT_B: drawBootAncient(c, w, h); break;
                case BOOT_C: drawBootZhouYi(c, w, h); break;
                case BOOT_D: drawBootRitual(c, w, h); break;
                case BOOT_E: drawBootTerminal(c, w, h); break;
                case BOOT_F: drawBootSixLine(c, w, h); break;
                default: drawBootClassic(c, w, h); break;
            }
        }

        private void drawBootClassic(Canvas c, float w, float h) {
            float cx = w / 2f;
            text(c, "M5://STICKS3", dp(22), dp(44), 8.5f, MUTED, Paint.Align.LEFT, true);
            text(c, "HEX TERMINAL", w-dp(22), dp(44), 8.5f, MUTED, Paint.Align.RIGHT, false);
            line(c, dp(22), dp(58), w-dp(22), dp(58), GRID, 1);
            float y = h * .37f;
            if (bootStep >= 1) text(c, "柳", cx-dp(22), y, 42, FG, Paint.Align.CENTER, true);
            if (bootStep >= 2) text(c, "卦", cx+dp(22), y, 42, GOLD, Paint.Align.CENTER, true);
            if (bootStep >= 3) {
                text(c, "RYU'S GUA", cx, y+dp(31), 10, MUTED, Paint.Align.CENTER, true);
                text(c, "电子蓍筮终端", cx, y+dp(52), 9.5f, FG, Paint.Align.CENTER, false);
            }
            float barL=dp(30), barR=w-dp(30), barY=h*.70f;
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(GRID);
            c.drawRect(barL, barY, barR, barY+dp(8), paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(GOLD);
            c.drawRect(barL+dp(2), barY+dp(2), barL+dp(2)+(barR-barL-dp(4))*bootSweep, barY+dp(6), paint);
            text(c, bootStep < 6 ? "INITIALIZING..." : "READY", cx, barY+dp(28), 8, bootStep<6?MUTED:GOLD, Paint.Align.CENTER, true);
            text(c, "v" + appVersion() + " / Ryu's Gua", cx, h-dp(36), 7.5f, MUTED, Paint.Align.CENTER, false);
        }

        private void drawBootElectronic(Canvas c, float w, float h) {
            float cx = w / 2f;
            text(c, "RYU'S GUA", dp(22), dp(44), 8.5f, MUTED, Paint.Align.LEFT, true);
            text(c, "ELECTRONIC YARROW TERMINAL", w-dp(22), dp(44), 7.1f, MUTED, Paint.Align.RIGHT, false);
            line(c, dp(22), dp(58), w-dp(22), dp(58), GRID, 1);
            if (bootStep >= 1) text(c, "柳 之 卦", cx, h*.31f, 31, FG, Paint.Align.CENTER, true);
            if (bootStep >= 2) text(c, "天地未判", cx, h*.44f, 13, MUTED, Paint.Align.CENTER, false);
            if (bootStep >= 3) text(c, "六爻待启", cx, h*.44f+dp(28), 13, GOLD, Paint.Align.CENTER, true);
            float y = h*.66f;
            line(c, dp(48), y, w-dp(48), y, GRID, 1);
            text(c, bootStep < 6 ? "INITIALIZING..." : "READY", cx, y+dp(27), 8.5f, bootStep<6?MUTED:GOLD, Paint.Align.CENTER, true);
            line(c, dp(48), y+dp(42), w-dp(48), y+dp(42), GRID, 1);
            if (bootStep >= 5) text(c, "一念既起 · 六爻将成", cx, h*.79f, 10.5f, FG, Paint.Align.CENTER, false);
            text(c, "v"+appVersion(), cx, h-dp(30), 7.3f, MUTED, Paint.Align.CENTER, false);
        }

        private void drawBootAncient(Canvas c, float w, float h) {
            float cx = w/2f;
            if (bootStep >= 1) text(c, "柳 之 卦", cx, h*.28f, 32, FG, Paint.Align.CENTER, true);
            if (bootStep >= 2) line(c, dp(82), h*.36f, w-dp(82), h*.36f, GOLD, 1);
            if (bootStep >= 2) text(c, "虚一而静", cx, h*.46f, 15, FG, Paint.Align.CENTER, false);
            if (bootStep >= 3) text(c, "以待来者", cx, h*.46f+dp(32), 15, GOLD, Paint.Align.CENTER, false);
            if (bootStep >= 4) text(c, "一事既念", cx, h*.65f, 11.5f, MUTED, Paint.Align.CENTER, false);
            if (bootStep >= 5) text(c, "六爻乃成", cx, h*.65f+dp(25), 11.5f, FG, Paint.Align.CENTER, true);
            if (bootStep >= 6) text(c, "起", cx, h*.80f, 21, RED, Paint.Align.CENTER, true);
            text(c, "RYU'S GUA", cx, h-dp(31), 7.5f, MUTED, Paint.Align.CENTER, true);
        }

        private void drawBootZhouYi(Canvas c, float w, float h) {
            float cx=w/2f;
            text(c, "柳 之 卦", cx, h*.22f, 28, FG, Paint.Align.CENTER, true);
            line(c, dp(70), h*.28f, w-dp(70), h*.28f, GRID, 1);
            if (bootStep >= 1) text(c, "易有太极", cx, h*.38f, 14, FG, Paint.Align.CENTER, false);
            if (bootStep >= 2) text(c, "是生两仪", cx, h*.38f+dp(28), 14, FG, Paint.Align.CENTER, false);
            if (bootStep >= 3) text(c, "两仪生四象", cx, h*.38f+dp(56), 13, MUTED, Paint.Align.CENTER, false);
            if (bootStep >= 4) text(c, "四象生八卦", cx, h*.38f+dp(84), 13, GOLD, Paint.Align.CENTER, false);
            if (bootStep >= 5) text(c, "六爻待成", cx, h*.68f, 12, GOLD, Paint.Align.CENTER, true);
            if (bootStep >= 6) text(c, "默 念 一 事", cx, h*.77f, 15, FG, Paint.Align.CENTER, true);
            text(c, "RYU'S GUA / ZHOU YI", cx, h-dp(30), 7.2f, MUTED, Paint.Align.CENTER, false);
        }

        private void drawBootRitual(Canvas c, float w, float h) {
            float cx=w/2f;
            if (bootStep >= 1) text(c, "静", cx, h*.28f, 44, GOLD, Paint.Align.CENTER, true);
            if (bootStep >= 2) {
                line(c, dp(74), h*.39f, w-dp(74), h*.39f, GRID, 1);
                text(c, "正心 · 诚意 · 持念", cx, h*.48f, 12.5f, FG, Paint.Align.CENTER, false);
                line(c, dp(74), h*.54f, w-dp(74), h*.54f, GRID, 1);
            }
            if (bootStep >= 3) text(c, "一事不二占", cx, h*.64f, 11, MUTED, Paint.Align.CENTER, false);
            if (bootStep >= 4) text(c, "念起", cx, h*.72f, 16, RED, Paint.Align.CENTER, true);
            if (bootStep >= 5) text(c, "柳 之 卦", cx, h*.82f, 20, FG, Paint.Align.CENTER, true);
            if (bootStep >= 6) text(c, "RYU'S GUA", cx, h*.82f+dp(24), 8, GOLD, Paint.Align.CENTER, true);
        }

        private void drawBootTerminal(Canvas c, float w, float h) {
            float x=dp(26), y=dp(57);
            text(c, "RYU'S GUA SYSTEM", x, dp(35), 9, GOLD, Paint.Align.LEFT, true);
            text(c, "BOOT / SELF TEST", w-dp(26), dp(35), 7.5f, MUTED, Paint.Align.RIGHT, false);
            line(c, x, dp(47), w-dp(26), dp(47), GRID, 1);
            String[] rows = {
                    "[01] YIN / YANG CORE ........ OK",
                    "[02] SIX-LINE REGISTER ..... OK",
                    "[03] ZHOUYI TEXT ........... OK",
                    "[04] HISTORY STORAGE ....... OK",
                    "[05] DIVINATION ENGINE ..... READY"
            };
            for (int i=0;i<rows.length;i++) if (bootStep >= i+1) text(c, rows[i], x, y+dp(i*29), 8.2f, i==4?GOLD:FG, Paint.Align.LEFT, i==4);
            if (bootStep >= 5) {
                line(c, x, h*.62f, w-dp(26), h*.62f, GRID, 1);
                text(c, "柳之卦 · 电子蓍筮终端", x, h*.68f, 10.5f, FG, Paint.Align.LEFT, true);
                text(c, "QUESTION CHANNEL : READY", x, h*.74f, 8.2f, GOLD, Paint.Align.LEFT, true);
                text(c, "CASTING MODULE    : STANDBY", x, h*.78f, 8.2f, MUTED, Paint.Align.LEFT, false);
            }
            if (bootStep >= 6) text(c, "默念一事", x, h*.88f, 13, GOLD, Paint.Align.LEFT, true);
            text(c, "v"+appVersion(), w-dp(26), h-dp(25), 7.2f, MUTED, Paint.Align.RIGHT, false);
        }

        private void drawBootSixLine(Canvas c, float w, float h) {
            float cx=w/2f;
            text(c, "柳 之 卦", cx, h*.18f, 27, FG, Paint.Align.CENTER, true);
            text(c, "RYU'S GUA / SIX-LINE BOOT", cx, h*.18f+dp(24), 7.5f, MUTED, Paint.Align.CENTER, true);
            String[] names={"初","二","三","四","五","上"};
            float start=h*.34f;
            for (int i=0;i<6;i++) {
                int displayOrder=5-i;
                float yy=start+dp(i*35);
                boolean ready=bootStep >= displayOrder+1;
                text(c, names[displayOrder]+" · "+(ready?"READY":"WAIT"), dp(58), yy, 9, ready?GOLD:MUTED, Paint.Align.LEFT, ready);
                line(c, dp(142), yy-dp(3), w-dp(55), yy-dp(3), ready?GOLD:GRID, ready?2:1);
            }
            if (bootStep >= 6) {
                text(c, "六爻已备", cx, h*.77f, 13, GOLD, Paint.Align.CENTER, true);
                text(c, "一念既起 · 静候成卦", cx, h*.82f, 10, FG, Paint.Align.CENTER, false);
            }
            text(c, "BOOT SEQUENCE "+Math.min(6, bootStep)+"/6", cx, h-dp(27), 7.5f, MUTED, Paint.Align.CENTER, true);
        }

        private void drawHeader'''
s, n = pattern.subn(replacement, s, count=1)
assert n == 1

old = '''            box.setPadding((int)(16*density), (int)(8*density), (int)(16*density), (int)(8*density));

            TextView modeTitle = new TextView(ctx); modeTitle.setText("投币动画"); modeTitle.setTextSize(14); modeTitle.setTextColor(GOLD); box.addView(modeTitle);
'''
new = '''            box.setPadding((int)(16*density), (int)(8*density), (int)(16*density), (int)(8*density));

            TextView bootTitle = new TextView(ctx); bootTitle.setText("开屏样式"); bootTitle.setTextSize(14); bootTitle.setTextColor(GOLD); box.addView(bootTitle);
            TextView bootHint = new TextView(ctx); bootHint.setText("共 7 套 · 切换后下次启动生效"); bootHint.setTextSize(10); bootHint.setTextColor(MUTED); box.addView(bootHint);
            Spinner bootSpinner = new Spinner(ctx);
            ArrayAdapter<String> bootAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, BOOT_STYLE_LABELS);
            bootAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            bootSpinner.setAdapter(bootAdapter);
            bootSpinner.setSelection(bootStyleIndex());
            box.addView(bootSpinner);

            TextView modeTitle = new TextView(ctx); modeTitle.setText("投币动画"); modeTitle.setTextSize(14); modeTitle.setTextColor(GOLD); modeTitle.setPadding(0, (int)(12*density), 0, 0); box.addView(modeTitle);
'''
assert old in s
s = s.replace(old, new, 1)

old = '''                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    soundEnabled = sound.isChecked(); hapticEnabled = haptic.isChecked(); shakeEnabled = shake.isChecked(); manualCasting = manual.isChecked();
'''
new = '''                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    int bootIndex = Math.max(0, Math.min(BOOT_STYLES.length - 1, bootSpinner.getSelectedItemPosition()));
                    bootStyle = BOOT_STYLES[bootIndex];
                    soundEnabled = sound.isChecked(); hapticEnabled = haptic.isChecked(); shakeEnabled = shake.isChecked(); manualCasting = manual.isChecked();
'''
assert old in s
s = s.replace(old, new, 1)

old = '''            String base = (ANIM_PHYSICS.equals(coinAnimationMode) ? "物理飞出" : "经典浮动")
                    + " · " + (manualCasting ? "逐爻" : "自动")
                    + " · " + (shakeEnabled ? "可摇动" : "仅点击");
'''
new = '''            String base = bootStyleShortLabel() + " · "
                    + (ANIM_PHYSICS.equals(coinAnimationMode) ? "物理飞出" : "经典浮动")
                    + " · " + (manualCasting ? "逐爻" : "自动")
                    + " · " + (shakeEnabled ? "可摇动" : "仅点击");
'''
assert old in s
s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
print('v1.1.2 boot styles patch applied')
