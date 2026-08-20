# References / 参考与致谢

This document records the major references behind **柳之卦 · Ryu's Gua / 掌卦**.

## 1. eXphinx — original inspiration

- X post: https://x.com/EXphinx/status/2061728481281724921
- Author/account: eXphinx / EXphinx

This project was initially inspired by the interaction concept and presentation of the related implementation. **柳之卦 / 掌卦 is an independent Android reconstruction, not an official port, and was not built from an original source tree supplied by eXphinx.**

No claim is made over the referenced project's original source code, branding, artwork, or other assets.

## 2. Related web implementation

- https://zg.yichenlab.com/

Used as a related public implementation/reference during product exploration. This repository remains an independent Android implementation.

## 3. Johnson-Jia / liuyao-divination

- Repository: https://github.com/Johnson-Jia/liuyao-divination
- License: MIT

Parts of the ZhouYi / divination reference data used during reconstruction were derived or validated with this project. Its MIT copyright and permission notice are retained in `THIRD_PARTY_NOTICES.md`.

## 4. Traditional method

The casting engine follows the traditional three-coin six-line convention used by this application:

- 6 — 老阴，动爻
- 7 — 少阳
- 8 — 少阴
- 9 — 老阳，动爻
- 六爻自下而上生成
- 变卦：6 变阳，9 变阴

The King Wen 64-hexagram ordering and trigram structure are traditional/public-domain cultural material; implementation code in this repository is independently written.

## Attribution policy

If a future contribution imports additional code, text datasets, artwork, sounds, or other assets, its origin and license should be added here and, where required, to `THIRD_PARTY_NOTICES.md` before release.
