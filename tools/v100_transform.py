from pathlib import Path
import re
import shutil

ROOT = Path('.')
OLD = ROOT / 'app/src/main/java/com/zhanggua/app'
NEW = ROOT / 'app/src/main/java/com/ryusgua/app'

if OLD.exists():
    NEW.parent.mkdir(parents=True, exist_ok=True)
    if NEW.exists():
        shutil.rmtree(NEW)
    shutil.move(str(OLD), str(NEW))
    # Remove now-empty old package directories.
    for p in [ROOT/'app/src/main/java/com/zhanggua', ROOT/'app/src/main/java/com/zhanggua']:
        try:
            p.rmdir()
        except OSError:
            pass

# Clean the current source tree, resources and build metadata. Git history is intentionally untouched.
text_roots = [ROOT/'app', ROOT/'settings.gradle', ROOT/'gradle.properties']
files = []
for item in text_roots:
    if item.is_file():
        files.append(item)
    elif item.exists():
        files.extend(p for p in item.rglob('*') if p.is_file() and p.suffix.lower() in {'.java','.xml','.gradle','.properties','.pro','.txt','.json'})

for p in files:
    s = p.read_text(encoding='utf-8')
    s = s.replace('com.zhanggua.app', 'com.ryusgua.app')
    s = s.replace('掌挂', '柳之卦').replace('掌卦', '柳之卦')
    s = s.replace('ZHANGGUA', 'RYUSGUA').replace('ZhangGua', 'RyusGua').replace('zhanggua', 'ryusgua')
    s = s.replace('zhang_gua', 'ryus_gua')
    p.write_text(s, encoding='utf-8')

# New Android identity and formal 1.0 version.
build = ROOT/'app/build.gradle'
s = build.read_text(encoding='utf-8')
s = re.sub(r"namespace\s+'[^']+'", "namespace 'com.ryusgua.app'", s)
s = re.sub(r"applicationId\s+'[^']+'", "applicationId 'com.ryusgua.app'", s)
s = re.sub(r'versionCode\s+\d+', 'versionCode 100', s)
s = re.sub(r"versionName\s+'[^']+'", "versionName '1.0.0'", s)
build.write_text(s, encoding='utf-8')

# Remove pre-1.0 AI settings migration. 1.0 has a new package/storage sandbox.
ai = NEW/'AiSettingsStore.java'
s = ai.read_text(encoding='utf-8')
for line in [
    '    private static final String LEGACY_PREFS = "ai_settings_v1";\n',
    '    private static final String K_MIGRATED = "migrated_from_v1";\n',
    '    private static final String OLD_ENDPOINT = "endpoint";\n',
    '    private static final String OLD_MODEL = "model";\n',
    '    private static final String OLD_MODE = "mode";\n',
    '    private static final String OLD_PROVIDER = "provider";\n',
    '    private static final String OLD_KEY_CT = "key_ct";\n',
    '    private static final String OLD_KEY_IV = "key_iv";\n',
    '    // v0.8 legacy keys.\n',
]:
    s = s.replace(line, '')
s = s.replace('        migrateLegacyIfNeeded(context);\n', '')
s = re.sub(r'\n    private static void migrateLegacyIfNeeded\(Context context\) \{.*?\n    \}\n\n    private static String key', '\n\n    private static String key', s, flags=re.S)
s = s.replace('private static final String PREFS = "ai_settings_v2";', 'private static final String PREFS = "ryusgua_ai_settings_v1";')
s = s.replace('private static final String KEY_ALIAS = "ryusgua_ai_api_key_v1";', 'private static final String KEY_ALIAS = "ryusgua_ai_api_key_v1";')
ai.write_text(s, encoding='utf-8')

# Remove history compatibility helpers that only existed for old app storage.
hist = NEW/'HistoryStore.java'
s = hist.read_text(encoding='utf-8')
s = re.sub(r'\n    /\*\* Backward-compatible overload.*?\n    static Entry add\(Context context, int\[\] lines\) \{ return add\(context, lines, false\); \}\n', '\n', s, flags=re.S)
s = s.replace('                if (id.isEmpty()) id = legacyId(time, lines);', '                if (id.isEmpty()) continue;')
s = re.sub(r'\n    /\*\* v0\.9\.2 compatibility:.*?\n    static void clear\(Context context\) \{ clearUnpinned\(context\); \}\n', '\n', s, flags=re.S)
s = re.sub(r'\n    private static String legacyId\(long time, int\[\] lines\) \{.*?\n    \}\n', '\n', s, flags=re.S)
s = s.replace('private static final String PREFS = "ryus_gua_history";', 'private static final String PREFS = "ryusgua_history_v1";')
hist.write_text(s, encoding='utf-8')

# Current callers should use the explicit APIs now that compatibility aliases are gone.
main = NEW/'MainActivity.java'
s = main.read_text(encoding='utf-8')
s = s.replace('HistoryStore.clear(getContext())', 'HistoryStore.clearUnpinned(getContext())')
main.write_text(s, encoding='utf-8')

# Clean theme/style identifiers too.
for p in (ROOT/'app/src/main/res').rglob('*.xml'):
    s = p.read_text(encoding='utf-8').replace('Theme.ZhangGua', 'Theme.RyusGua').replace('Theme.RyusGua', 'Theme.RyusGua')
    p.write_text(s, encoding='utf-8')

# Bilingual README for the new formal identity.
readme = '''# 柳之卦 · Ryu's Gua

> **正式版 / Stable release: 1.0.0**  
> **Android package:** `com.ryusgua.app`

## 中文

**柳之卦（Ryu's Gua）** 是一款以三钱六爻为核心交互的 Android《周易》文化应用。1.0 起项目与 Android 应用统一使用“柳之卦”这一名称，并启用全新的包名 `com.ryusgua.app`。

### 1.0 重要变化

- 全新 Android 应用身份：`com.ryusgua.app`
- 应用名称统一为 **柳之卦**
- 1.0 不再承担 0.9.x 的数据或覆盖安装兼容；旧版与 1.0 可以并存
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

### What's new in 1.0

- New Android application identity: `com.ryusgua.app`
- App name unified as **柳之卦 / Ryu's Gua**
- 1.0 intentionally drops in-place/data migration compatibility with 0.9.x; old builds and 1.0 may coexist
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
'''
(ROOT/'README.md').write_text(readme, encoding='utf-8')

# Bilingual 1.0 changelog entry; keep prior history below it without reintroducing the old product name.
ch = ROOT/'CHANGELOG.md'
old_ch = ch.read_text(encoding='utf-8') if ch.exists() else '# Changelog\n'
entry = '''# Changelog\n\n## 1.0.0 / 正式版\n- 中文：启用全新包名 `com.ryusgua.app`，应用名称统一为“柳之卦”，不再提供 0.9.x 覆盖安装/数据迁移兼容。\n- 中文：保留 Standard（Android 8.0+）与 Legacy（Android 6.0+）双 APK，并清理旧命名空间与历史迁移代码。\n- English: Introduced the new `com.ryusgua.app` application identity and unified the app name as 柳之卦 / Ryu's Gua. In-place/data migration from 0.9.x is intentionally not supported.\n- English: Retained Standard (Android 8.0+) and Legacy (Android 6.0+) APKs while removing the pre-1.0 namespace and migration helpers.\n\n'''
if old_ch.startswith('# Changelog'):
    old_ch = old_ch[len('# Changelog'):].lstrip('\n')
ch.write_text(entry + old_ch, encoding='utf-8')

# New update channel metadata. Old github.io compatibility metadata is intentionally left on 0.9.x.
update = '''{
  "versionCode": 100,
  "versionName": "1.0.0",
  "title": "柳之卦 v1.0.0 正式版 / Ryu's Gua 1.0.0",
  "notes": "1.0 使用全新包名 com.ryusgua.app 与应用名柳之卦；不支持 0.9.x 覆盖升级。Standard 支持 Android 8.0+，Legacy 支持 Android 6.0+。 / New app identity com.ryusgua.app; no in-place upgrade from 0.9.x. Standard: Android 8.0+, Legacy: Android 6.0+.",
  "releasePage": "https://github.com/Ryyus/Ryus-Gua/releases/tag/v1.0.0",
  "apkUrl": "https://github.com/Ryyus/Ryus-Gua/releases/download/v1.0.0/RyusGua-v1.0.0-standard.apk",
  "legacyApkUrl": "https://github.com/Ryyus/Ryus-Gua/releases/download/v1.0.0/RyusGua-v1.0.0-legacy.apk"
}
'''
(ROOT/'update/latest.json').write_text(update, encoding='utf-8')

# Remove temporary transform machinery from the dev branch before it is merged.
for temp in [ROOT/'.v100-trigger', ROOT/'tools/v100_transform.py', ROOT/'.github/workflows/apply-v100.yml']:
    if temp.exists():
        temp.unlink()
try:
    (ROOT/'tools').rmdir()
except OSError:
    pass

# Hard assertions: current app tree must not retain old identity/name tokens.
for base in [ROOT/'app', ROOT/'README.md', ROOT/'settings.gradle']:
    paths = [base] if base.is_file() else [p for p in base.rglob('*') if p.is_file()]
    for p in paths:
        try:
            t = p.read_text(encoding='utf-8')
        except Exception:
            continue
        forbidden = ['com.zhanggua.app', '掌卦', '掌挂', 'ZhangGua', 'zhanggua']
        found = [x for x in forbidden if x in t]
        if found:
            raise SystemExit(f'Forbidden legacy identity {found} remains in {p}')

assert (NEW/'MainActivity.java').exists()
assert "applicationId 'com.ryusgua.app'" in build.read_text(encoding='utf-8')
assert "versionCode 100" in build.read_text(encoding='utf-8')
assert "versionName '1.0.0'" in build.read_text(encoding='utf-8')
assert 'android:label="柳之卦"' in (ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
print('v1.0.0 transformation complete')
