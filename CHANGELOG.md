# Changelog

## 1.4.0
- 中文：新增 Java 原生、完全离线的六爻纳甲排盘：八宫世应、纳甲、六亲六神、月建日辰、旬空、旺衰、空破暗动、动爻化变、墓库、三合六合六冲、卦变与反吟伏吟。
- 中文：结果页新增“排盘”，支持可选用神、复制完整盘面与打开 11 专题本地术理索引。
- 中文：离线解卦追加六爻盘面摘要；可选 AI 解卦会同时使用当前盘面事实与相关本地术理摘要，不再只依赖卦辞/动爻。
- 中文：《周易》卦辞爻辞继续复用既有本地 `zhouyi.json`；六爻规则与静态表参考并重新实现自 MIT 授权的 Johnson-Jia/liuyao-divination，并保留第三方许可声明。
- 中文：版本提升至 1.4.0 / versionCode 140。
- English: Added a Java-native, fully offline Liuyao Najia board engine covering Eight Palaces, Shi/Ying, Najia, Six Relations/Spirits, calendar strength, void/break/dark movement, transformations, tombs, combinations/clashes, palace transformation, Fanyin and Fuyin.
- English: Added a Board page with optional Yongshen selection, full-board copy, and an 11-topic local rule index.
- English: Offline and optional AI readings now receive computed board facts and only the relevant local rule digest.
- English: Reused the existing local Zhouyi text and preserved the MIT attribution for Johnson-Jia/liuyao-divination.

## 1.3.0
- 中文：设置中心统一为深色圆角卡片，并将交互项改为现代 Switch；同类设置标题统一四字、说明统一八字。
- 中文：新增“开屏进度”开关；新安装默认开启音效反馈、动效呈现、垂直翻转，其余交互项默认关闭。
- 中文：正式起卦加入五分钟节制规则；已完成正式起卦五分钟内不可再次正式起卦，取消不计入；最近五分钟已有两次普通起卦时同样暂缓正式起卦。
- 中文：受限状态使用《蹇》“往蹇来誉”，提示引用《蒙》“初筮告，再三渎，渎则不告”。
- 中文：更新检查改为 jsDelivr 国内友好 CDN 优先、GitHub 兜底，并为 Standard / Legacy 增加可回退的镜像 APK 地址。
- English: Modernized Settings with dark rounded cards and native switches, plus consistent four-character titles and eight-character descriptions for comparable Chinese UI copy.
- English: Added a boot-progress toggle and changed fresh-install defaults to Sound, Animation, and Vertical Flip enabled.
- English: Added a five-minute Formal Casting guard; cancelled casts do not count, and two completed ordinary casts within the rolling five-minute window also pause Formal Casting.
- English: Update checks now prefer jsDelivr with GitHub fallback, including mirror/fallback APK URLs for both Standard and Legacy variants.

## 1.2.0
- 中文：主页普通起卦主按钮由“按下成卦”改为“一念既起 · 六爻将成”；手动逐爻模式仍保留“按下 · 掷第一爻”。
- 中文：应用图标重绘为无边框现代扁平风格，采用深绿底、淡色流线、米白六爻与灰绿柳枝，延续主页淡白色条纹的视觉语言。
- 中文：Legacy 启动图标与 Android 8.0+ Adaptive Icon 同步更新；版本提升至 1.2.0 / versionCode 120。
- English: Changed the normal home casting CTA to “一念既起 · 六爻将成” while preserving the manual first-line prompt.
- English: Reworked the launcher icon into a borderless modern flat willow + six-line design with subtle pale stripes, shared across legacy and adaptive icon paths.
- English: Bumped the app to versionName 1.2.0 / versionCode 120.

## 1.1.2
- 中文：新增 7 套可切换开屏样式：经典初版、A 电子法器启动、B 古意极简、C 周易原典、D 卜筮仪式、E 终端自检、F 六爻启动序列。
- 中文：当前经典初版完整保留并继续作为默认；开屏样式在“设置 → 交互与动画”中选择，保存后下次启动生效。
- 中文：不同样式使用独立文字节奏与启动时长；经典初版约 1.25 秒，A–F 约 2.05–2.60 秒。
- 中文：开屏样式选择仅保存在本机，不需要网络、账号或额外权限。
- English: Added seven selectable boot-screen styles: Classic, A Electronic Ritual Device, B Ancient Minimal, C Zhou Yi Classical, D Divination Ritual, E Terminal Self-Test, and F Six-Line Boot Sequence.
- English: The existing Classic boot is preserved and remains the default; the selected style is saved in Interaction & Animation settings and takes effect on the next launch.
- English: Each rich style has its own text sequence and timing while Classic remains a fast ~1.25 s boot.

## 1.1.1
- 中文：统一所有起卦页面的底部状态区为手动正式起卦的安全布局，红色“正式起卦/爻位”提示统一上移并为爻象保留相同空间，修复文字与爻象重叠。
- 中文：普通起卦、正式起卦均新增与历史页一致的右上角“返回”按钮；返回会安全取消当前起卦并回到主页。
- 中文：系统返回键同样统一为取消当前起卦；为经典铜钱动画增加取消守卫，避免退出后残留动画继续落爻。
- English: Unified the bottom status area across all casting modes to the safe manual Formal Casting layout, preventing red status text from overlapping the hexagram stack.
- English: Added the same top-right Back control used by History to both normal and Formal Casting; Back safely cancels the current cast and returns home.
- English: System Back now follows the same cancellation behavior, and classic animation stops cleanly when a cast is cancelled.

## 1.1.0
- 中文：新增完全离线的规则解卦页；点击“解卦”先显示本地结果，再由用户选择是否调用 AI。
- 中文：正式起卦入口移至主页，普通起卦与正式起卦分开显示；交互设置不再重复放置正式起卦按钮。
- 中文：离线解卦按本卦/之卦、上下卦象意和 0–6 动爻取用规则生成；AI 提示词改为基于本地已计算结果，避免重复算卦。
- 中文：离线结果支持一键复制；若历史中已有 AI 解读，离线页可直接进入已保存的 AI 结果。
- 中文：修复应用内仍硬编码显示 v0.9.3 的问题；启动页、顶部栏、设置与更新卡片统一读取构建版本。
- English: Added a fully offline deterministic reading page. The Reading action now shows local results first, with AI available as an optional second step.
- English: Offline readings can be copied, and saved AI interpretations remain accessible from the offline page.
- English: Fixed stale hard-coded v0.9.3 UI labels by reading the build version dynamically.

## 1.0.0 / 正式版
- 中文：启用全新包名 `com.ryusgua.app`，应用名称统一为“柳之卦”，不再提供 0.9.x 覆盖安装/数据迁移兼容。
- 中文：保留 Standard（Android 8.0+）与 Legacy（Android 6.0+）双 APK，并清理旧命名空间与历史迁移代码。
- English: Introduced the new `com.ryusgua.app` application identity and unified the app name as 柳之卦 / Ryu's Gua. In-place/data migration from 0.9.x is intentionally not supported.
- English: Retained Standard (Android 8.0+) and Legacy (Android 6.0+) APKs while removing the pre-1.0 namespace and migration helpers.

## 0.9.3
- 中文：经典浮动动画延长到与物理飞出一致的约 1.08 秒；正式起卦会关闭设置并返回主界面；预设 AI 服务商 Endpoint 改为只读显示。
- 中文：历史记录新增秒级时间、正式起卦标记、AI 解卦持久化、PIN 固定与备注；30 条上限仅统计未固定记录。
- 中文：新增 Android 6.0+ Legacy APK，标准 APK 继续支持 Android 8.0+；两者保持相同包名、版本号与正式签名身份。
- English: Matched classic animation duration to the ~1.08 s physics animation; Formal Casting now exits Settings; preset AI endpoints are read-only.
- English: History now stores second-level timestamps, Formal Casting metadata, AI results, pins, and notes; the 30-entry rolling limit applies only to unpinned entries.
- English: Added an Android 6.0+ Legacy APK alongside the Android 8.0+ Standard APK; both keep the same package, version code, and release-signing identity.

## 0.9.2
- Separated AI reasoning/thinking from the final interpretation stream; reasoning no longer leaks into the answer body.
- Added an expandable **思考过程** section in the AI page. It is collapsed by default and streams independently when the provider exposes reasoning.
- OpenAI Responses requests use low reasoning effort, low verbosity, and reasoning summaries when supported; Gemini OpenAI-compatible requests use low reasoning effort.
- Added reasoning adapters for OpenAI Responses events, DeepSeek-style `reasoning_content`, compatible `thinking` fields, and inline `<think>...</think>` output.
- Tightened the divination prompt to roughly 300–450 Chinese characters, with a maximum target around 500 characters.
- Removed the unsafe generic `*.delta` fallback that could misclassify reasoning events as final answer text.

## 0.9.1
- Settings card summaries refresh immediately after AI or interaction settings are saved.
- Added **正式起卦**: first line is cast at the next whole minute, then subsequent lines at +10/+20/+30/+40/+50 seconds.
- When manual line casting is enabled during 正式起卦, each scheduled slot prompts the user first and force-casts after 3 seconds without input.
- Added optional **vertical coin flip** rendering for both classic and physics animations.
- Changed visible English branding from ZhangGua to **Ryu's Gua** while keeping the Chinese app name 掌卦 and package ID `com.zhanggua.app` unchanged.
- Added author credit in Settings.

## 0.9.0
- Fixed AI settings persistence. Saving now writes synchronously and performs an immediate read-back verification instead of only showing a success message.
- Added provider-scoped AI configuration: OpenAI, DeepSeek, Gemini, 通义千问, Kimi and Custom each retain independent encrypted API keys, endpoints, models and protocol modes.
- Added automatic migration of the v0.8 global AI configuration into the provider that was active before upgrade.
- Added streaming AI interpretation output for Responses API and Chat Completions compatible endpoints, so generated text appears progressively.
- Switched the in-app update checker to the standalone `Ryyus/Ryus-Gua` update channel.

## 0.8.0
- Added selectable **classic floating** and **physics launch** coin animations.
- Physics mode launches three coins from the casting-button area with independent velocity, gravity, rotation, simple coin collision and damped spring settling.
- Reworked Settings into compact grouped cards instead of a fully expanded form.

## 0.7.0
- Added gesture-navigation and bottom safe-area handling for edge-to-edge Android devices.
- Renamed short UI labels to clearer two-character labels such as 经文 / 历史.
- Unified experience and AI settings under 设置.
- Fixed malformed coin rendering; all three coins keep correct circular proportions.
- Added AI provider presets: OpenAI, DeepSeek, Gemini, 通义千问, Kimi and Custom.

## 0.6.0
- Started long-term release signing.
- Hardened startup/fullscreen/audio/haptic paths to fail open instead of crashing.
- Added automatic and manual update checking.

## 0.5.0 and earlier
- Established manual casting, shake casting, history, sharing, full ZhouYi text, AI interpretation, SoundPool audio, haptics and the initial ritual UI.
