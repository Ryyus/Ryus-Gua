from pathlib import Path
# Normalize Java escape sequences before applying the generated patch.
p=Path('tools/apply_v140.py')
s=p.read_text(encoding='utf-8')
s=s.replace("rep('''", "rep(r'''")
s=s.replace(",\n'''", ",\nr'''")
s=s.replace("old_ai='''", "old_ai=r'''")
s=s.replace("new_ai='''", "new_ai=r'''")
p.write_text(s,encoding='utf-8')
print('normalized Java string patch anchors')
