# Changelog

## 1.0.0 / 正式版
- 中文：启用全新包名 `com.ryusgua.app`，应用名称统一为“柳之卦”，不再提供 0.9.x 覆盖安装/数据迁移兼容。
- 中文：保留 Standard（Android 8.0+）与 Legacy（Android 6.0+）双 APK，并清理旧命名空间与历史迁移代码。
- English: Introduced the new `com.ryusgua.app` application identity and unified the app name as 柳之卦 / Ryu's Gua. In-place/data migration from 0.9.x is intentionally not supported.
- English: Retained Standard (Android 8.0+) and Legacy (Android 6.0+) APKs while removing the pre-1.0 namespace and migration helpers.

## 0.9.3
- 中文：经典浮动动画延长到与物理飞出一致的约 1.08 秒；正式起卦会关闭设置并返回主界面；预设 AI 服务商 Endpoint 改为只读显示。
- 中文：历史记录新增秒级时间、正式起卦标记、AI 解读持久化、PIN 固定与备注；30 条上限仅统计未固定记录。
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
- Physics mode launches three coins from the casting-button area with independent velocity, gravity, drag, rotation, simple coin collision and damped spring settling.
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
