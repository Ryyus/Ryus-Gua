from pathlib import Path

readme = Path('README.md')
s = readme.read_text(encoding='utf-8')
s = s.replace('> **正式版 / Stable release: 1.3.0**', '> **正式版 / Stable release: 1.4.0**', 1)
anchor = '### 1.3.0 交互与更新\n'
section = '''### 1.4.0 本地六爻排盘\n\n- 新增完全离线的 **六爻纳甲排盘**：八宫、世应、纳甲、六亲、六神、月建日辰、旬空、旺衰、空破暗动、动爻化变、墓库、三合六合六冲、卦变与反伏。\n- 结果页新增 **排盘** 页面，可选择父母/兄弟/子孙/妻财/官鬼为用神，并复制完整盘面。\n- 新增 **11 专题本地术理索引**，排盘与规则检索均不需要网络；《周易》卦辞爻辞继续复用既有本地数据，不重复引入。\n- 离线解卦会附加当前六爻盘面摘要；可选 AI 解卦会使用当前排盘事实与相关本地术理摘要。\n- 六爻规则与静态表参考并重新实现自 MIT 授权的 `Johnson-Jia/liuyao-divination`，许可与版权声明保留在 `THIRD_PARTY_NOTICES.md`。\n- 版本号提升至 **1.4.0 / versionCode 140**。\n\n'''
if anchor not in s:
    raise SystemExit('README anchor missing')
s = s.replace(anchor, section + anchor, 1)
# English top section if present.
eng_anchor = "### What's new in 1.3.0\n"
eng = '''### What's new in 1.4.0\n\n- Added a fully local Java-native Liuyao Najia board: Eight Palaces, Shi/Ying, Najia stems/branches, Six Relations, Six Spirits, month/day influence, Xunkong, strength, breaks/dark movement, moving-line transformations, tombs, combinations/clashes, palace transformation, Fanyin and Fuyin.\n- Added a dedicated **Board** page with optional Yongshen selection and full-board copy.\n- Added an **11-topic offline Liuyao rule index**; the existing local Zhouyi text remains the canonical hexagram/line text source.\n- Offline readings now include the computed board facts, while optional AI readings receive only the relevant local rule digest plus the board.\n- Liuyao rules/static tables were reimplemented from the MIT-licensed `Johnson-Jia/liuyao-divination`; attribution is preserved in `THIRD_PARTY_NOTICES.md`.\n- Version bumped to **1.4.0 / versionCode 140**.\n\n'''
if eng_anchor in s:
    s = s.replace(eng_anchor, eng + eng_anchor, 1)
readme.write_text(s, encoding='utf-8')

ch = Path('CHANGELOG.md')
s = ch.read_text(encoding='utf-8')
anchor = '## 1.3.0\n'
section = '''## 1.4.0\n- 中文：新增 Java 原生、完全离线的六爻纳甲排盘：八宫世应、纳甲、六亲六神、月建日辰、旬空、旺衰、空破暗动、动爻化变、墓库、三合六合六冲、卦变与反吟伏吟。\n- 中文：结果页新增“排盘”，支持可选用神、复制完整盘面与打开 11 专题本地术理索引。\n- 中文：离线解卦追加六爻盘面摘要；可选 AI 解卦会同时使用当前盘面事实与相关本地术理摘要，不再只依赖卦辞/动爻。\n- 中文：《周易》卦辞爻辞继续复用既有本地 `zhouyi.json`；六爻规则与静态表参考并重新实现自 MIT 授权的 Johnson-Jia/liuyao-divination，并保留第三方许可声明。\n- 中文：版本提升至 1.4.0 / versionCode 140。\n- English: Added a Java-native, fully offline Liuyao Najia board engine covering Eight Palaces, Shi/Ying, Najia, Six Relations/Spirits, calendar strength, void/break/dark movement, transformations, tombs, combinations/clashes, palace transformation, Fanyin and Fuyin.\n- English: Added a Board page with optional Yongshen selection, full-board copy, and an 11-topic local rule index.\n- English: Offline and optional AI readings now receive computed board facts and only the relevant local rule digest.\n- English: Reused the existing local Zhouyi text and preserved the MIT attribution for Johnson-Jia/liuyao-divination.\n\n'''
if anchor not in s:
    raise SystemExit('CHANGELOG anchor missing')
s = s.replace(anchor, section + anchor, 1)
ch.write_text(s, encoding='utf-8')
print('finalized README and CHANGELOG for v1.4.0')
