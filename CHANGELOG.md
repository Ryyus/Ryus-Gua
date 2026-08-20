# Changelog

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
