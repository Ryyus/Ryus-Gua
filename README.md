# 柳之卦 · Ryu's Gua

> **正式版 / Stable release: 1.1.0**  
> **Android package:** `com.ryusgua.app`

## 中文

**柳之卦（Ryu's Gua）** 是一款以三钱六爻为核心交互的 Android《周易》文化应用。1.0 起项目与 Android 应用统一使用“柳之卦”这一名称，并启用全新的包名 `com.ryusgua.app`。

### 1.1 重要变化

- “解卦”现在首先打开**离线解卦**：不需要网络、账号、API Key 或云端模型。
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

- 三钱六爻起卦：6 / 7 / 8 / 9、动爻与变卦
- 普通起卦与“正式起卦”定时模式
- 经典浮动 / 物理飞出两套铜钱动画与可选垂直翻转
- 经文、历史、复制、分享、备注与固定（PIN）
- 历史记录保存 `HH:mm:ss`、正式起卦标记与 AI 解卦结果；普通历史保留最近 30 条，PIN 项永久保留
- AI 解卦支持 OpenAI、DeepSeek、Gemini、通义千问、Kimi 与自定义兼容端点
- AI 流式输出、思考过程独立折叠、服务商独立 API Key / Endpoint / 模型设置
- API Key 使用 Android Keystore AES/GCM 加密保存
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

**Ryu's Gua (柳之卦)** is an Android I Ching / Zhou Yi cultural app built around the traditional three-coin, six-line casting interaction. Starting with 1.0, the project and the Android app share one formal identity and use the new package name `com.ryusgua.app`.

### What's new in 1.1

- **Reading** now opens a fully offline deterministic interpretation first; no network, account, API key, or cloud model is required.
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
- Normal casting and scheduled **Formal Casting** mode
- Classic floating / physics launch coin animations with optional vertical flipping
- Zhou Yi text, history, copy/share, notes, and PIN support
- History stores `HH:mm:ss`, formal-cast metadata, and AI interpretations; the latest 30 ordinary entries are retained while pinned entries never expire
- AI interpretation with OpenAI, DeepSeek, Gemini, Qwen, Kimi, and custom compatible endpoints
- Streaming AI output with a separately collapsible reasoning/thinking section
- Provider-scoped API keys, endpoints, models, and protocol settings
- API keys protected with Android Keystore AES/GCM
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
