# 柳之卦 · Ryu's Gua

> Android application name: **掌卦**  
> Package: `com.zhanggua.app`

**柳之卦（Ryu's Gua）** is the project/repository name. The Android application itself continues to use the name **掌卦** so existing installs, package identity, signing continuity, and upgrade compatibility are not broken.

掌卦是一款以三钱六爻为核心交互的 Android 应用，包含传统起卦、卦象/变卦展示、完整经文、历史记录、可选 AI 解读，以及强调仪式感的铜钱动画与触感反馈。

## Current release

- App: 掌卦
- Version: `0.8.0` (`versionCode 8`)
- Android package: `com.zhanggua.app`
- minSdk: 26
- targetSdk / compileSdk: 35
- Release signing: maintained privately; signing key is **not** stored in this repository

## Highlights

- 三钱法六爻起卦，6/7/8/9 与动爻、之卦逻辑
- 经典浮动 / 物理飞出两套铜钱动画
- 物理动画包含独立初速度、重力、阻尼、旋转、简单币间碰撞与弹簧收束
- 全面屏、刘海/挖孔、Android 手势导航底部安全区适配
- 经文、历史、复制、分享、重新起卦
- 逐爻手动投掷与摇动起卦
- 音效 / 震动 / 动画模式可配置
- 紧凑分组设置页
- AI 解卦：OpenAI、DeepSeek、Gemini、通义千问、Kimi、自定义兼容端点
- API Key 使用 Android Keystore 保护后保存在本机
- 应用内版本检查

## Repository layout

```text
app/                    Android application source
.github/workflows/      Public CI (debug build only)
docs/REFERENCES.md      References / attribution
THIRD_PARTY_NOTICES.md  Third-party notices and licenses
CHANGELOG.md            Version history
update/latest.json      Update-channel metadata
```

## Build

Requirements:

- JDK 17
- Android SDK 35
- Gradle 8.9

For a normal development build:

```bash
gradle assembleDebug
```

Release signing is intentionally external to this public repository. When the four `ZG_*` signing properties are supplied by the private release pipeline, Gradle uses the long-term 掌卦 release certificate; otherwise debug builds remain fully buildable without access to signing secrets.

## Project naming

- **Project / repository:** 柳之卦 · Ryu's Gua
- **Android app:** 掌卦
- **Package ID:** `com.zhanggua.app`

The app name and package ID are intentionally retained to preserve the established Android identity and future in-place upgrades.

## Update-channel migration

Released v0.8 builds still query a legacy compatibility metadata path under `Ryyus.github.io` because that endpoint is already shipped on users' devices. New releases and release assets live in this repository. The next signed app release will switch `UpdateChecker` to this repository directly; the legacy metadata file can then remain only as a compatibility redirect/channel for older installs.

## Reference / Attribution

This is an **independent Android reconstruction**. It is not presented as an official port of, or source release from, any referenced implementation, and it does not claim ownership of referenced projects or their original assets.

See **[docs/REFERENCES.md](docs/REFERENCES.md)** for the complete list.

Key references:

1. eXphinx — original inspiration / related StickS3-style implementation and post:  
   https://x.com/EXphinx/status/2061728481281724921
2. Related web implementation:  
   https://zg.yichenlab.com/
3. `Johnson-Jia/liuyao-divination` — reference for ZhouYi / divination data used during reconstruction (MIT):  
   https://github.com/Johnson-Jia/liuyao-divination

Third-party license notices are preserved in **[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)**.

## Disclaimer

本项目用于传统文化、交互设计与软件实现研究。卦象与 AI 解读不应替代医疗、法律、财务或其他专业意见。
