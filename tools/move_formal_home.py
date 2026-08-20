from pathlib import Path

p = Path('app/src/main/java/com/ryusgua/app/MainActivity.java')
s = p.read_text(encoding='utf-8')

def once(old, new, label):
    global s
    if old not in s:
        raise SystemExit(f'missing expected snippet: {label}')
    s = s.replace(old, new, 1)

once('        private final RectF experienceButton = new RectF();\n',
     '        private final RectF experienceButton = new RectF();\n        private final RectF formalButton = new RectF();\n',
     'formal button field')

once('            primaryButton.set(dp(28), h - dp(122), w - dp(28), h - dp(64));\n            button(c, primaryButton, manualCasting ? "按下 · 掷第一爻" : "按下成卦", GOLD, true, 13);',
     '            formalButton.set(dp(28), h - dp(174), w - dp(28), h - dp(132));\n            button(c, formalButton, "正式起卦 · 等待整分", GOLD, false, 10.5f);\n            primaryButton.set(dp(28), h - dp(122), w - dp(28), h - dp(64));\n            button(c, primaryButton, manualCasting ? "按下 · 掷第一爻" : "按下成卦", GOLD, true, 13);',
     'home buttons')

once('            if (state == State.IDLE) {\n                if (primaryButton.contains(x, y)) { haptic(HapticFeedbackConstants.CONFIRM); pulse(22, 120); startCasting(); return true; }',
     '            if (state == State.IDLE) {\n                if (formalButton.contains(x, y)) { haptic(HapticFeedbackConstants.CONFIRM); pulse(22, 120); startFormalCasting(); return true; }\n                if (primaryButton.contains(x, y)) { haptic(HapticFeedbackConstants.CONFIRM); pulse(22, 120); startCasting(); return true; }',
     'home touch')

once('            interaction.setOnClickListener(v -> showInteractionSettingsDialog(\n                    () -> interactionSummaryView.setText(interactionSummary()),\n                    () -> { if (parentDialog[0] != null) parentDialog[0].dismiss(); }));',
     '            interaction.setOnClickListener(v -> showInteractionSettingsDialog(\n                    () -> interactionSummaryView.setText(interactionSummary())));',
     'settings card listener')

once('        private void showInteractionSettingsDialog() { showInteractionSettingsDialog(null, null); }\n\n        private void showInteractionSettingsDialog(Runnable onSaved) { showInteractionSettingsDialog(onSaved, null); }\n\n        private void showInteractionSettingsDialog(Runnable onSaved, Runnable onFormalStarted) {',
     '        private void showInteractionSettingsDialog() { showInteractionSettingsDialog(null); }\n\n        private void showInteractionSettingsDialog(Runnable onSaved) {',
     'settings overloads')

start = s.find('            TextView formalTitle = new TextView(ctx);')
end = s.find('            AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle("交互与动画").setView(box)', start)
if start < 0 or end < 0:
    raise SystemExit('formal settings block not found')
s = s[:start] + s[end:]
s = s.replace('                clockHandler.post(clockUpdate[0]);\n', '', 1)
start = s.find('            formalStart.setOnClickListener(v -> {')
end_token = '            dialog.setOnDismissListener(d -> clockHandler.removeCallbacksAndMessages(null));\n'
end = s.find(end_token, start)
if start < 0 or end < 0:
    raise SystemExit('formal settings click block not found')
s = s[:start] + s[end + len(end_token):]

for stale in ('formalStart', 'formalClock', 'clockHandler', 'onFormalStarted'):
    if stale in s:
        raise SystemExit(f'stale formal-settings code remains: {stale}')
p.write_text(s, encoding='utf-8')

c = Path('CHANGELOG.md')
t = c.read_text(encoding='utf-8')
marker = '- 中文：新增完全离线的规则解卦页；点击“解卦”先显示本地结果，再由用户选择是否调用 AI。\n'
if marker in t and '正式起卦入口移至主页' not in t:
    t = t.replace(marker, marker + '- 中文：正式起卦入口移至主页，普通起卦与正式起卦分开显示；交互设置不再重复放置正式起卦按钮。\n', 1)
c.write_text(t, encoding='utf-8')

r = Path('README.md')
t = r.read_text(encoding='utf-8')
cn = '- “解卦”现在首先打开**离线解卦**：不需要网络、账号、API Key 或云端模型。\n'
if cn in t and '正式起卦入口直接放在主页' not in t:
    t = t.replace(cn, cn + '- **正式起卦入口直接放在主页**，与普通起卦明确分开；设置页仅保留交互与动画参数。\n', 1)
en = '- **Reading** now opens a fully offline deterministic interpretation first; no network, account, API key, or cloud model is required.\n'
if en in t and 'Formal Casting now has a dedicated home-screen button' not in t:
    t = t.replace(en, en + '- **Formal Casting now has a dedicated home-screen button**, separate from normal casting; Settings only contains interaction/animation preferences.\n', 1)
r.write_text(t, encoding='utf-8')
