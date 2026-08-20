from pathlib import Path

p = Path('app/src/main/java/com/ryusgua/app/MainActivity.java')
s = p.read_text(encoding='utf-8')
old = 'manualCasting ? "按下 · 掷第一爻" : "按下成卦"'
new = 'manualCasting ? "按下 · 掷第一爻" : "一念既起 · 六爻将成"'
if old not in s:
    raise SystemExit('target home casting label not found')
s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')
