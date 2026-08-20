# 柳之卦 · Ryu's Gua

> Android 应用名称 / Android app name: **掌卦**  
> Package: `com.zhanggua.app`

## 中文

**柳之卦（Ryu's Gua）** 是本项目与 GitHub 仓库的名称；Android 应用本身继续显示为 **掌卦**，并保留现有包名与长期签名，以保证既有安装可以直接覆盖升级。

掌卦是一款以三钱六爻为核心交互的 Android 应用，提供传统起卦、正式定时起卦、卦象与变卦展示、完整经文、历史记录、可选 AI 解读，以及强调仪式感的铜钱动画、音效与触感反馈。

### 当前版本

- 版本：`0.9.3`（`versionCode 12`）
- 标准 APK：Android 8.0+（API 26+）
- Legacy APK：Android 6.0+（API 23+）
- targetSdk / compileSdk：35
- 正式签名：由私有发布流水线维护；签名私钥**不会**存入本公开仓库

### v0.9.3 重点

- 经典浮动动画与物理飞出动画统一为约 **1.08 秒**，以物理飞出动画为基准，降低静态铜币翻转速度。
- 正式起卦从设置启动后会自动关闭所有设置弹窗并返回主界面等待下一整分。
- 非“自定义”AI 服务商不再允许编辑 API 地址，只以小字显示固定 Endpoint；自定义服务商仍可编辑。
- AI 解卦结果写入对应历史记录；再次从历史打开该卦时可直接查看当时保存的 AI 解读，并可选择重新解卦覆盖。
- 历史记录显示到秒（`HH:mm:ss`）并标记普通起卦 / 正式起卦。
- 历史保留最近 **30 条未固定记录**；固定（PIN）记录不计入 30 条上限，也不会被“清空未固定”删除。
- 每条历史都可以固定 / 取消固定并添加备注。
- 新增 Legacy APK，兼容 Android 6.0–7.x；标准 APK 继续面向 Android 8.0+。

### 现有功能

- 三钱法六爻起卦，6/7/8/9、动爻与之卦逻辑
- 正式起卦：整分初爻，+10/+20/+30/+40/+50 秒依次起后五爻
- 经典浮动 / 物理飞出两套铜钱动画，可选垂直翻转
- 全面屏、刘海/挖孔、手势导航安全区适配
- 经文、复制、历史、重新起卦
- 逐爻手动投掷、摇动起卦、音效与触感反馈
- AI：OpenAI、DeepSeek、Gemini、通义千问、Kimi、自定义兼容端点
- AI 流式输出；reasoning/thinking 与最终解读分流，默认折叠，可手动展开
- 每个 AI 服务商独立保存 API Key、模型、协议与配置
- API Key 使用 Android Keystore AES/GCM 加密保存在本机
- 应用内版本检查

## English

**Ryu's Gua** is the project and repository name. The Android application itself continues to be displayed as **掌卦**, while keeping the existing package ID and long-term signing identity so installed builds can upgrade in place.

掌卦 is an Android I Ching divination app centered on the three-coin, six-line method. It includes standard casting, scheduled formal casting, hexagram and transformed-hexagram views, full classical text, local history, optional AI interpretation, and ritual-style coin animation, audio, and haptics.

### Current release

- Version: `0.9.3` (`versionCode 12`)
- Standard APK: Android 8.0+ (API 26+)
- Legacy APK: Android 6.0+ (API 23+)
- targetSdk / compileSdk: 35
- Release signing is maintained in a private pipeline; signing secrets are **not** stored in this public repository.

### v0.9.3 highlights

- Classic floating and physics-launch coin animations now use the same ~**1.08 s** duration, using the physics animation as the timing reference.
- Starting Formal Casting from Settings automatically closes all Settings dialogs and returns to the main screen before the next whole-minute trigger.
- Preset AI providers expose their API endpoint as read-only small text; endpoint editing is available only for the Custom provider.
- AI interpretations are persisted into their matching history entry and can be reopened later; rerunning AI overwrites the saved interpretation for that entry.
- History timestamps now include seconds and identify normal vs. Formal Casting.
- The rolling limit is **30 unpinned entries**. Pinned entries do not count toward the limit and are never removed by “Clear unpinned”.
- Every history entry can be pinned/unpinned and annotated with a note.
- A Legacy APK is now published for Android 6.0–7.x, while the Standard APK remains for Android 8.0+.

### Existing features

- Three-coin six-line casting with 6/7/8/9, moving lines, and transformed hexagrams
- Formal Casting at the next whole minute, then +10/+20/+30/+40/+50 seconds
- Classic floating / physics launch animation, with optional vertical flip
- Edge-to-edge, display cutout, and gesture-navigation safe-area handling
- Classical text, copy, history, recast
- Manual per-line casting, shake casting, sound and haptic controls
- AI support for OpenAI, DeepSeek, Gemini, Qwen, Kimi, and custom OpenAI-compatible endpoints
- Streaming AI output with reasoning/thinking separated from final answers and collapsed by default
- Provider-scoped API keys, models, protocols, and settings
- Android Keystore AES/GCM protection for local API keys
- In-app update checking

## Repository layout / 仓库结构

```text
app/                    Android application source / Android 源码
.github/workflows/      Public CI
docs/REFERENCES.md      References / 参考与致谢
THIRD_PARTY_NOTICES.md  Third-party notices / 第三方许可
CHANGELOG.md            Version history / 版本记录
update/latest.json      Update metadata / 更新元数据
```

## Build / 构建

Requirements / 环境：

- JDK 17
- Android SDK 35
- Gradle 8.9

```bash
# Both debug variants / 同时构建标准与 Legacy Debug
gradle assembleDebug

# Individual variants / 单独构建
gradle assembleStandardDebug
gradle assembleLegacyDebug
```

Release signing is intentionally external to this public repository. / 正式签名刻意放在私有发布流水线中，本仓库不包含签名私钥。

## Project naming / 项目命名

- Project / repository：**柳之卦 · Ryu's Gua**
- Android app：**掌卦**
- Package ID：`com.zhanggua.app`

## Update channel / 更新通道

v0.9 and later query this repository through `update/latest.json`. Older released builds may still query the compatibility metadata under `Ryyus.github.io`; that legacy file is retained only so older installations can discover releases from this repository.

v0.9 及之后版本直接查询本仓库的 `update/latest.json`。更早版本仍可能访问 `Ryyus.github.io` 下的兼容更新地址，该文件仅用于保证旧安装继续发现新版本。

## Reference / Attribution / 参考与致谢

This is an **independent Android reconstruction** and is not presented as an official port or source release of any referenced implementation.  
本项目是**独立 Android 重构**，不宣称为任何参考项目的官方移植或源码发布。

See / 详见 **[docs/REFERENCES.md](docs/REFERENCES.md)**.

Key references / 主要参考：

1. eXphinx — original inspiration / related StickS3-style implementation and post  
   https://x.com/EXphinx/status/2061728481281724921
2. Related web implementation / 相关网页实现  
   https://zg.yichenlab.com/
3. `Johnson-Jia/liuyao-divination` — ZhouYi / divination data reference (MIT)  
   https://github.com/Johnson-Jia/liuyao-divination

Third-party license notices are preserved in **[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)**.

## Disclaimer / 免责声明

本项目用于传统文化、交互设计与软件实现研究。卦象与 AI 解读不应替代医疗、法律、财务或其他专业意见。  
This project is for traditional-culture, interaction-design, and software research. Divination and AI interpretation are not substitutes for medical, legal, financial, or other professional advice.
