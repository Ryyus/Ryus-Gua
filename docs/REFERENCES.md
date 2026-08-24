# References / 参考与致谢

This document records the major references behind **柳之卦 · Ryu's Gua**.

## 1. eXphinx — original inspiration

- X post: https://x.com/EXphinx/status/2061728481281724921
- Author/account: eXphinx / EXphinx

This project was initially inspired by the interaction concept and presentation of the related implementation. **柳之卦 is an independent Android reconstruction, not an official port, and was not built from an original source tree supplied by eXphinx.**

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

## 5. Tarot system and local copy

柳之卦 uses the traditional 78-card Tarot structure: 22 Major Arcana and 56 Minor Arcana across Wands, Cups, Swords, and Pentacles. Card names, suit structure, and commonly established archetypes are treated as cultural reference points; the Chinese keywords, upright/reversed explanations, action prompts, spread copy, and local reference topics in `app/src/main/res/raw/tarot_cards.json` are independently written for this project.

During v1.7.0 research, the public SealDice draw datasets for Tarot and the 64 hexagrams were evaluated as product/data references. Their Tarot file contains major-arcana upright/reversed entries but not a complete 78-card deck, while the hexagram file offers broad scenario summaries rather than 柳之卦's classical gua-ci/yao-ci plus moving-line and Najia structure. Because no explicit reusable license was identified for those text files, **none of their interpretation wording is copied or redistributed here**. The review informed only the decision to keep spreads explicit and to separate Tarot from Liuyao references.

## Attribution policy

If a future contribution imports additional code, text datasets, artwork, sounds, or other assets, its origin and license should be added here and, where required, to `THIRD_PARTY_NOTICES.md` before release.
