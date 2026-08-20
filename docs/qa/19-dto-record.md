# Response DTO は record でよいか

## 質問

Response は今後 record で作成した方がよい？

## 結論

**はい。API のリクエスト／レスポンス DTO は `record` を基本にする。**  
JPA Entity（`User` / `Event` 等）は従来どおり **class**（可変＋ファクトリ）。

| 種類 | 推奨 |
|------|------|
| `XxxResponse` / `XxxRequest` | `record` |
| `domain.*` Entity | class |

## なぜ record か

- 応答は作って返すだけ（あとから書き換えない）→ **不変性（EJ）**
- コンストラクタ・アクセサが短く書ける
- Entity を API に直出ししない、という層分けと相性がよい

## 使わない方がよい例

- JPA Entity（永続化でフィールド更新が必要）
- 深い継承が必要な古い DTO 設計（本プロジェクトでは不要）

正: `.cursor/rules/live-qa-backend.mdc` / `live-qa-learning-explain.mdc` の【EJ】不変性。

## Q: Request/Response の Javadoc はコンポーネント（アクセサ）まで付ける？

**付けない。** クラス（どの API の入出力か）＋ファクトリ／compact だけ。record の `body()` 等は Entity の getter と同じ粒度。

検証は Request では Bean Validation（`@NotBlank` 等）、遷移可否は Service。`ErrorResponse` だけ `of` / `withField` と compact（null fields → 空 Map）がある。`@author` は既存作者を残す（ErrorResponse は mizumura）。
