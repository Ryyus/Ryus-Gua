from pathlib import Path

p = Path('app/src/main/java/com/ryusgua/app/MainActivity.java')
s = p.read_text(encoding='utf-8')

def once(old, new, label):
    global s
    if old not in s:
        raise SystemExit(f'missing expected snippet: {label}')
    s = s.replace(old, new, 1)

once(
'''            if (state == State.CASTING && formalCastingActive) {
                cancelFormalCasting();
                state = State.IDLE;
                postInvalidateOnAnimation();
                Toast.makeText(getContext(), "正式起卦已取消", Toast.LENGTH_SHORT).show();
                return true;
            }
''',
'''            if (state == State.CASTING) {
                cancelCurrentCasting();
                return true;
            }
''',
'handleBack casting')

once(
'''            float cx = w / 2f;
            int shown = Math.min(castCount + 1, 6);
            if (formalCastingActive) {
''',
'''            float cx = w / 2f;
            int shown = Math.min(castCount + 1, 6);
            backButton.set(w - dp(78), dp(86), w - dp(20), dp(116));
            button(c, backButton, "返回", MUTED, false, 9);
            if (formalCastingActive) {
''',
'casting back button')

once('            float reserved = manualCasting ? dp(145) : dp(80);\n',
     '            float reserved = dp(145);\n',
     'casting reserved space')

once(
'''            if (manualCasting || ANIM_PHYSICS.equals(coinAnimationMode)) {
                if (!toastLine.isEmpty()) text(c, toastLine, cx, h - dp(94), 10.5f, RED, Paint.Align.CENTER, true);
                primaryButton.set(dp(28), h - dp(72), w - dp(28), h - dp(20));
                String label;
                if (castCount >= 6) label = "成卦中…";
                else if (lineAnimating) label = "投掷中…";
                else if (formalCastingActive && manualCasting) label = formalAwaitingManual ? "点击 · 掷此爻" : "等待定时…";
                else label = manualCasting ? "点击 · 下一爻" : "自动 · 下一爻";
                button(c, primaryButton, label, lineAnimating ? MUTED : GOLD, true, 11.5f);
            } else {
                if (!toastLine.isEmpty()) text(c, toastLine, cx, h - dp(42), 11, RED, Paint.Align.CENTER, true);
            }
''',
'''            if (!toastLine.isEmpty()) text(c, toastLine, cx, h - dp(94), 10.5f, RED, Paint.Align.CENTER, true);
            if (manualCasting || ANIM_PHYSICS.equals(coinAnimationMode)) {
                primaryButton.set(dp(28), h - dp(72), w - dp(28), h - dp(20));
                String label;
                if (castCount >= 6) label = "成卦中…";
                else if (lineAnimating) label = "投掷中…";
                else if (formalCastingActive && manualCasting) label = formalAwaitingManual ? "点击 · 掷此爻" : "等待定时…";
                else label = manualCasting ? "点击 · 下一爻" : "自动 · 下一爻";
                button(c, primaryButton, label, lineAnimating ? MUTED : GOLD, true, 11.5f);
            }
''',
'casting status layout')

once(
'''            if (state == State.CASTING && manualCasting) {
                if (primaryButton.contains(x, y) && !lineAnimating && castCount < 6) {
                    if (formalCastingActive && !formalAwaitingManual) {
                        Toast.makeText(getContext(), "正式起卦 · 请等待定时提示", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (formalCastingActive) formalAwaitingManual = false;
                    haptic(HapticFeedbackConstants.CLOCK_TICK);
                    pulse(15, 90);
                    castNext();
                }
                return true;
            }
''',
'''            if (state == State.CASTING) {
                if (backButton.contains(x, y)) {
                    haptic(HapticFeedbackConstants.CLOCK_TICK);
                    cancelCurrentCasting();
                    return true;
                }
                if (manualCasting && primaryButton.contains(x, y) && !lineAnimating && castCount < 6) {
                    if (formalCastingActive && !formalAwaitingManual) {
                        Toast.makeText(getContext(), "正式起卦 · 请等待定时提示", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (formalCastingActive) formalAwaitingManual = false;
                    haptic(HapticFeedbackConstants.CLOCK_TICK);
                    pulse(15, 90);
                    castNext();
                }
                return true;
            }
''',
'touch casting back')

once(
'''        private void cancelFormalCasting() {
            formalCastingActive = false;
            formalAwaitingManual = false;
            formalStartEpoch = 0L;
            handler.removeCallbacksAndMessages(null);
        }
''',
'''        private void cancelFormalCasting() {
            formalCastingActive = false;
            formalAwaitingManual = false;
            formalStartEpoch = 0L;
            handler.removeCallbacksAndMessages(null);
        }

        private void cancelCurrentCasting() {
            boolean wasFormal = formalCastingActive;
            cancelFormalCasting();
            lineAnimating = false;
            physicsActive = false;
            toastLine = "";
            state = State.IDLE;
            postInvalidateOnAnimation();
            Toast.makeText(getContext(), wasFormal ? "正式起卦已取消" : "起卦已取消", Toast.LENGTH_SHORT).show();
        }
''',
'cancel current casting')

once(
'''            Runnable flip = new Runnable() {
                @Override public void run() {
                    float t = Math.min(1f, (SystemClock.uptimeMillis() - started) / 1080f);
''',
'''            Runnable flip = new Runnable() {
                @Override public void run() {
                    if (state != State.CASTING || !lineAnimating) return;
                    float t = Math.min(1f, (SystemClock.uptimeMillis() - started) / 1080f);
''',
'classic cancellation guard')

# Sanity checks.
for required in [
    'button(c, backButton, "返回", MUTED, false, 9);',
    'float reserved = dp(145);',
    'cancelCurrentCasting();',
    'if (state != State.CASTING || !lineAnimating) return;'
]:
    if required not in s:
        raise SystemExit(f'missing required result: {required}')

p.write_text(s, encoding='utf-8')
