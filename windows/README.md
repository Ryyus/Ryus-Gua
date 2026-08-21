# 柳之卦 · Ryu's Gua — Windows

Windows 桌面版与 Android 版分开维护、分开发版。

## Windows v1.0.0

首个 Windows 桌面版本覆盖核心离线流程：

- 三钱六爻随机起卦（6 / 7 / 8 / 9）
- 手动输入六爻
- 本卦 / 之卦 / 动爻显示
- 完整《周易》卦辞与爻辞
- 与 Android 共用三钱六爻核心与《周易》数据源
- 确定性离线解卦
- 最近 30 条本地历史
- 一键复制当前卦象与解卦
- 深绿 + 淡色流线 + 柳枝 / 六爻视觉语言
- 完全本地运行核心功能，不要求账号

Windows 历史数据默认保存在：

`%APPDATA%\RyusGua\history.tsv`

## 构建

需要 Windows + JDK 17（含 `jpackage`）+ ImageMagick；安装版 EXE 还需要 WiX Toolset。

```powershell
pwsh windows/build.ps1 -Version 1.0.0
```

产物位于 `windows/build/dist/`：

- `RyusGua-Windows-v1.0.0-setup.exe`：安装版
- `RyusGua-Windows-v1.0.0-portable.zip`：免安装便携版，内含 `RyusGua.exe` 和私有 Java Runtime
- `sha256-windows.txt`

> `RyusGua-launcher.exe` 只是便携包中的启动器副本，单独拿走无法替代 portable ZIP 中的 runtime 目录。

## 发布轨道

- Android：`vX.Y.Z`
- Windows：`windows-vX.Y.Z`

两个平台版本号独立，不强行同步。
