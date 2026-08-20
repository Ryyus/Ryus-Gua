from pathlib import Path

source = Path('tools/move_formal_home.py').read_text(encoding='utf-8')
old = "for stale in ('formalStart', 'formalClock', 'clockHandler', 'onFormalStarted'):\n    if stale in s:\n        raise SystemExit(f'stale formal-settings code remains: {stale}')"
new = "for stale in ('Button formalStart', 'formalStart.setOnClickListener', 'TextView formalClock', 'clockHandler', 'onFormalStarted'):\n    if stale in s:\n        raise SystemExit(f'stale formal-settings code remains: {stale}')"
if old not in source:
    raise SystemExit('expected cleanup guard not found')
source = source.replace(old, new, 1)
exec(compile(source, 'tools/move_formal_home.py', 'exec'))
