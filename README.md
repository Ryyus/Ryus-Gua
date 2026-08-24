# 柳之卦 · Ryu's Gua

> **正式版 / Stable release: 1.6.0**  
> **开发版本 / Next build: 1.7.0**  
> **Android package:** `com.ryusgua.app`

## 中文

**柳之卦（Ryu's Gua）** 是一款以三钱六爻为核心、兼具本地塔罗牌阵的 Android 占卜文化应用。1.0 起项目与 Android 应用统一使用“柳之卦”这一名称，并启用全新的包名 `com.ryusgua.app`。

### 1.7.0 牌阵、AI 与体系分层

- 塔罗主页新增 **一牌照见 / 三牌成阵 / 四象观局** 三种牌阵，抽牌、结果与历史均按实际牌数自适应。
- 78 张塔罗牌、三种牌阵与七篇塔罗索引迁移至独立 `tarot_cards.json`；牌义由柳之卦独立编写，不复制未明确授权的第三方解释文本。
- 离线解牌新增独立 **牌义** 页面；可选 **AI 解牌**只使用本局既定牌阵、正逆位和本地牌义，不会重新抽牌。
- 六爻与塔罗分别展示适用的交互设置和术理索引：塔罗不再显示铜钱翻转、手动逐爻等六爻专属选项。
- 塔罗牌面增加花色、正逆位、几何纹样与层级信息；新增依次翻牌、流光显影、轻叠展开三种呈现。
- 中央印章拖动时外框缓缓旋转，右上角模式入口改为无边框文字按钮；经典浮动与星轨回旋获得更清晰的波纹、轨道和拖尾效果。
- 版本号提升至 **1.7.0 / versionCode 170**。

### 1.6.0 六爻 / 塔罗双占卜

- 右上角由静态“易”改为可点击的 **六爻 / 塔罗** 模式按钮；选择页沿用深绿、米白与朱红的既有视觉语言。
- 新增完整 **78 张塔罗牌**与三牌阵：**缘起 / 此刻 / 趋向**。抽牌不重复，包含正逆位、逐牌释义、三牌合参和独立本地历史。
- 六爻主页中央“卦”印章支持拖动；松手后会以缓慢柔和的回弹动画自动归于正中，塔罗“牌”印章使用相同交互。
- 铜钱动画新增 **星轨回旋**，三枚铜钱沿衰减轨道回旋并落定，同时保留经典浮动与物理飞出。
- **开屏演示**结束后会回到原设置层，未保存的开屏样式与进度开关保持不变。
- 塔罗计算、释义与历史均可完全离线使用；它不会改写六爻卦象，也不把抽牌包装成确定性预言。
- 版本号提升至 **1.6.0 / versionCode 160**。

### 1.5.0 结果页与开屏优化

- 精简起卦完成后的卦象页面：顶部保留 **设置 / 历史**，主操作只保留 **查看解卦**。
- **经文 / 排盘** 移入离线解卦页，让卦象页先呈现结果，再进入需要的内容层。
- 返回主页文案改为 **“此卦已成 · 再起一卦”**，延续四字·四字的界面节奏。
- 优化设置中心的弹窗呈现，减少打开复杂设置时的瞬间卡顿。
- **开屏进度** 现已覆盖全部七套开屏样式；非经典样式分别提供与主题相符的进度效果。
- 交互设置新增 **开屏演示**，可即时测试当前选择的开屏样式与进度效果，演示不会保存草稿设置。
- 版本号提升至 **1.5.0 / versionCode 150**。

### 1.4.0 本地六爻排盘

- 新增完全离线的 **六爻纳甲排盘**：八宫、世应、纳甲、六亲、六神、月建日辰、旬空、旺衰、空破暗动、动爻化变、墓库、三合六合六冲、卦变与反伏。
- 结果页新增 **排盘** 页面，可选择父母/兄弟/子孙/妻财/官鬼为用神，并复制完整盘面。
- 新增 **11 专题本地术理索引**，排盘与规则检索均不需要网络；《周易》卦辞爻辞继续复用既有本地数据，不重复引入。
- 离线解卦会附加当前六爻盘面摘要；可选 AI 解卦会使用当前排盘事实与相关本地术理摘要。
- 六爻规则与静态表参考并重新实现自 MIT 授权的 `Johnson-Jia/liuyao-divination`，许可与版权声明保留在 `THIRD_PARTY_NOTICES.md`。
- 版本号提升至 **1.4.0 / versionCode 140**。

### 1.3.0 交互与更新

- 设置页同步主界面的深色卡片视觉，交互项改用现代 Switch 开关，并统一同类四字标题与八字说明。
- 新增 **开屏进度** 开关；新安装默认开启 **音效反馈、动效呈现、垂直翻转**，触感、摇动和手动逐爻默认关闭。
- **正式起卦** 增加五分钟节制规则：完成一次正式起卦后五分钟内不可再次正式起卦；取消不计入；五分钟内完成两次普通起卦也会暂缓正式起卦。
- 受限提示采用《蒙》“初筮告，再三渎，渎则不告”，主页受限按钮使用《蹇》“往蹇来誉”。
- 应用内更新改为 **jsDelivr 国内优先 + GitHub 兜底**，版本元数据与 Standard / Legacy APK 都具备多源下载路径。
- 版本号提升至 **1.3.0 / versionCode 130**。

### 1.2.0 视觉更新

- 主页普通起卦主按钮由 **“按下成卦”** 改为 **“一念既起 · 六爻将成”**，保留手动逐爻模式原有“按下 · 掷第一爻”提示。
- 应用图标全面重绘为无边框现代扁平风格：深绿底、淡色流线、米白六爻与灰绿柳枝。
- 新图标沿用主页淡白色条纹的视觉语言，并同时适配 Legacy 图标与 Android 8.0+ Adaptive Icon。
- 版本号提升至 **1.2.0 / versionCode 120**。

### 1.1.2 开屏样式

- 新增 **7 套可切换开屏**，当前经典初版完整保留并继续作为默认样式。
- 7 套分别为：**经典初版、A 电子法器启动、B 古意极简、C 周易原典、D 卜筮仪式、E 终端自检、F 六爻启动序列**。
- 在 **设置 → 交互与动画 → 开屏样式** 中选择，保存后下次启动生效。
- 经典初版保持约 1.25 秒的快速启动；A–F 根据文字节奏使用约 2.05–2.60 秒的仪式化启动时长。
- 开屏选择保存在本机，不需要网络、账号或额外权限。

### 1.1.1 维护更新

- 所有起卦页面统一使用手动正式起卦的底部安全布局，红色状态提示不再与爻象重叠。
- 普通起卦与正式起卦均使用与历史页一致的右上角 **返回** 按钮。
- 返回会安全取消当前起卦并回到主页；系统返回键执行同样逻辑。
- 经典铜钱动画在取消起卦后会立即停止，不会继续后台落爻。

### 1.1 重要变化

- “解卦”现在首先打开**离线解卦**：不需要网络、账号、API Key 或云端模型。
- **正式起卦入口直接放在主页**，与普通起卦明确分开；设置页仅保留交互与动画参数。
- 离线结果根据本卦/之卦、上下卦象意与动爻数量动态生成，并采用透明的传统常见变爻取用规则。
- 离线结果页提供 **AI 解卦** 按钮；AI 仅作为用户主动选择的增强层，并基于本机已经计算好的卦象与取用结果继续解释。
- 修复应用内版本号仍显示 0.9.3 的问题；所有版本展示改为读取当前安装包的 `versionName`，以后升级无需逐处修改。

### 1.0 重要变化

- 全新 Android 应用身份：`com.ryusgua.app`
- 应用名称统一为 **柳之卦**
- 1.0 不再承担 0.9.x 的数据或覆盖安装兼容；旧版与 1.0 可以并存，安装 1.0 不会覆盖旧包
- 保留 Standard / Legacy 双 APK：Standard 面向 Android 8.0+；Legacy 支持 Android 6.0+
- 清理旧版本 AI 设置迁移、旧历史兼容入口和旧命名空间

### 主要功能

- 六爻 / 塔罗双占卜入口，可随时在主页右上角切换
- 三钱六爻起卦：6 / 7 / 8 / 9、动爻与变卦
- 完整 78 张塔罗牌与三种牌阵：一牌、三牌、四象，含正逆位、本地牌义、AI 解牌与独立历史
- 普通起卦与“正式起卦”定时模式，正式模式含五分钟节制规则
- **7 套可切换开屏样式**，包括经典、古意、原典、仪式、终端与六爻启动方向
- 经典浮动 / 物理飞出 / 星轨回旋三套铜钱动画与可选垂直翻转
- 经文、历史、复制、分享、备注与固定（PIN）
- 历史记录保存 `HH:mm:ss`、正式起卦标记与 AI 解卦结果；普通历史保留最近 30 条，PIN 项永久保留
- AI 解卦 / 解牌支持 OpenAI、DeepSeek、Gemini、通义千问、Kimi 与自定义兼容端点
- AI 流式输出、思考过程独立折叠、服务商独立 API Key / Endpoint / 模型设置
- API Key 使用 Android Keystore AES/GCM 加密保存
- 更新检查与 APK 下载支持 jsDelivr 国内优先、GitHub 兜底
- 全面屏、刘海/挖孔与 Android 手势导航安全区适配

### APK 选择

- **Standard:** Android 8.0+（API 26+）
- **Legacy:** Android 6.0+（API 23+）

Legacy 版本仍保留 Android Keystore AES/GCM 的密钥保护，不通过降低安全性来换取旧系统兼容。

### 构建

需要 JDK 17、Android SDK 35、Gradle 8.9。

```bash
gradle assembleStandardDebug assembleLegacyDebug
```

正式签名密钥不存放在本 public repository 中。

---

## English

**Ryu's Gua (柳之卦)** is an Android divination culture app centered on the traditional three-coin Liuyao interaction and complemented by selectable offline Tarot spreads. Starting with 1.0, the project and the Android app share one formal identity and use the package name `com.ryusgua.app`.

### What's new in 1.7.0

- Added selectable one-card, three-card, and four-element Tarot spreads with adaptive casting, result, and history layouts.
- Moved all 78 cards, spread definitions, and seven Tarot reference topics into an independently authored `tarot_cards.json` resource.
- Added a dedicated local Card Meanings page and optional Tarot AI reading grounded in the already drawn cards; AI never redraws the spread.
- Split Interaction Settings and the reference index by divination system, so Tarot no longer exposes Liuyao-only coin/manual controls.
- Refined Tarot card faces and added Cascade, Luminous Reveal, and Gentle Fan presentation styles.
- Added a rotating seal frame while dragging, made the top-right mode selector borderless, and strengthened Classic Float / Orbital Spiral effects.
- Bumped the app to **1.7.0 / versionCode 170**.

### What's new in 1.6.0

- Replaced the static top-right “易” mark with a tappable **Liuyao / Tarot** mode selector styled to match the existing app.
- Added a complete **78-card Tarot deck** and a three-card **Origin / Present / Direction** spread with unique draws, upright/reversed meanings, combined offline interpretation, and separate local history.
- Made the central Liuyao seal draggable; releasing it triggers a slow, gentle return to the exact center. The Tarot seal follows the same interaction.
- Added a third coin animation, **Orbital Spiral**, alongside Classic Float and Physics Launch.
- Boot Preview now returns to the same Settings layer and preserves unsaved boot-style/progress choices.
- Tarot remains fully local, does not alter Liuyao calculations, and is presented as reflective guidance rather than deterministic prediction.
- Bumped the app to **1.6.0 / versionCode 160**.

### What's new in 1.5.0

- Simplified the completed-result screen: **Settings / History** stay at the top and **Offline Reading** is the single primary action.
- Moved **Zhouyi Text / Liuyao Board** into the Offline Reading page.
- Reworded the return action as **“此卦已成 · 再起一卦”** to match the established four-character rhythm.
- Smoothed settings presentation by replacing the janky system window transition with a short hardware-layer fade.
- Extended optional boot progress to all seven boot styles with theme-specific progress effects.
- Added **Boot Preview** so the selected boot style and progress can be tested instantly without saving the draft.
- Bumped the app to **1.5.0 / versionCode 150**.

### What's new in 1.4.0

- Added a fully local Java-native Liuyao Najia board: Eight Palaces, Shi/Ying, Najia stems/branches, Six Relations, Six Spirits, month/day influence, Xunkong, strength, breaks/dark movement, moving-line transformations, tombs, combinations/clashes, palace transformation, Fanyin and Fuyin.
- Added a dedicated **Board** page with optional Yongshen selection and full-board copy.
- Added an **11-topic offline Liuyao rule index**; the existing local Zhouyi text remains the canonical hexagram/line text source.
- Offline readings now include the computed board facts, while optional AI readings receive only the relevant local rule digest plus the board.
- Liuyao rules/static tables were reimplemented from the MIT-licensed `Johnson-Jia/liuyao-divination`; attribution is preserved in `THIRD_PARTY_NOTICES.md`.
- Version bumped to **1.4.0 / versionCode 140**.

### What's new in 1.3.0

- Modernized Settings to match the app's dark card visual language, replacing checkbox-style controls with modern switches and consistent copy lengths.
- Added a boot-progress toggle. Fresh installs now enable sound, animation, and vertical coin flipping by default, while haptics, shake casting, and manual line casting default to off.
- Added a five-minute Formal Casting guard: completed formal casts start a five-minute cooldown; cancelled casts do not count; two completed normal casts inside the same five-minute window also temporarily block Formal Casting.
- Uses classical Zhou Yi copy for the guard state, including “初筮告，再三渎，渎则不告” and “往蹇来誉”.
- Update metadata and APK delivery now use a jsDelivr-first path with GitHub fallback for better accessibility on mainland China networks.
- Version bumped to **1.3.0 / versionCode 130**.

### What's new in 1.3.0

- Modernized Settings with dark cards and switch controls, including a dedicated boot-progress toggle.
- Fresh installs enable sound, animation, and vertical flip by default.
- Added a five-minute Formal Casting guard; cancelled casts do not count, and two completed normal casts within five minutes also pause Formal Casting.
- Added a jsDelivr-first update path with GitHub fallback and flavor-specific Standard / Legacy mirrors.
- Version bumped to **1.3.0 / versionCode 130**.

### What's new in 1.2.0

- Changed the normal home casting CTA from **“按下成卦”** to **“一念既起 · 六爻将成”** while keeping the manual first-line prompt unchanged.
- Reworked the launcher icon into a borderless modern flat design with a dark green field, subtle pale stripes, ivory six-line symbol, and muted willow branch.
- The new visual language is shared by legacy launcher icons and Android 8.0+ Adaptive Icons.
- Version bumped to **1.2.0 / versionCode 120**.

### What's new in 1.1.2

- Added **seven selectable boot-screen styles** while preserving the existing Classic boot as the default.
- Styles: **Classic**, **A Electronic Ritual Device**, **B Ancient Minimal**, **C Zhou Yi Classical**, **D Divination Ritual**, **E Terminal Self-Test**, and **F Six-Line Boot Sequence**.
- Choose the style in **Settings → Interaction & Animation → Boot style**; the saved choice takes effect on the next launch.
- Classic remains a fast ~1.25 s boot, while A–F use roughly 2.05–2.60 s to support their richer text sequences.
- The selected boot style is stored locally and does not require network access or extra permissions.

### What's new in 1.1.1

- Unified all casting screens to the safer manual Formal Casting bottom layout so red status text no longer overlaps the hexagram stack.
- Added the same top-right **Back** button used by History to both normal and Formal Casting screens.
- Back safely cancels the active cast and returns home; the system Back action now follows the same behavior.
- Classic coin animation stops cleanly when casting is cancelled instead of continuing to settle a line in the background.

### What's new in 1.1

- **Reading** now opens a fully offline deterministic interpretation first; no network, account, API key, or cloud model is required.
- **Formal Casting now has a dedicated home-screen button**, separate from normal casting; Settings only contains interaction/animation preferences.
- The offline page derives its result from the base/changed hexagrams, trigram symbolism, and moving-line selection rules.
- AI is an optional second step launched from the offline result page and is grounded in the locally computed reading.
- Fixed stale in-app version labels; all version displays now read the installed package `versionName`, so future releases do not need per-screen edits.

### What's new in 1.0

- New Android application identity: `com.ryusgua.app`
- App name unified as **柳之卦 / Ryu's Gua**
- 1.0 intentionally drops in-place/data migration compatibility with 0.9.x; old builds and 1.0 may coexist, and installing 1.0 does not overwrite the old package
- Standard and Legacy APKs remain available: Standard targets Android 8.0+, Legacy supports Android 6.0+
- Removed pre-1.0 AI migration helpers, legacy history compatibility entry points, and the old Java namespace

### Highlights

- Three-coin six-line casting with moving lines and changed hexagrams
- Normal casting and scheduled **Formal Casting** mode with a five-minute guard
- Seven selectable boot-screen styles spanning classic, ritual, classical, terminal, and six-line themes
- Classic floating / physics launch coin animations with optional vertical flipping
- Zhou Yi text, history, copy/share, notes, and PIN support
- History stores `HH:mm:ss`, formal-cast metadata, and AI interpretations; the latest 30 ordinary entries are retained while pinned entries never expire
- AI interpretation with OpenAI, DeepSeek, Gemini, Qwen, Kimi, and custom compatible endpoints
- Streaming AI output with a separately collapsible reasoning/thinking section
- Provider-scoped API keys, endpoints, models, and protocol settings
- API keys protected with Android Keystore AES/GCM
- jsDelivr-first update delivery with GitHub fallback
- Edge-to-edge, cutout, and gesture-navigation safe-area support

### APK variants

- **Standard:** Android 8.0+ (API 26+)
- **Legacy:** Android 6.0+ (API 23+)

The Legacy build keeps the same Android Keystore AES/GCM protection instead of weakening local credential security.

### Build

Requires JDK 17, Android SDK 35, and Gradle 8.9.

```bash
gradle assembleStandardDebug assembleLegacyDebug
```

Release signing material is intentionally kept outside this public repository.

## Reference / Attribution

This remains an **independent Android reconstruction** and does not claim to be an official port of any referenced implementation. See [docs/REFERENCES.md](docs/REFERENCES.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Disclaimer / 免责声明

本项目用于传统文化、交互设计与软件实现研究。卦象与 AI 解读不应替代医疗、法律、财务或其他专业意见。  
This project is for traditional-culture, interaction-design, and software research. Divination and AI-generated interpretations are not substitutes for medical, legal, financial, or other professional advice.
