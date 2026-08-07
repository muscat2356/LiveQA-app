# questionCount（一覧なし・詳細のみ・スナップショット列）

## 決定（2026-08-07）

| 項目 | 内容 |
|------|------|
| 一覧 `GET /api/events` | **`questionCount` なし** |
| 詳細 `GET /api/events/{id}` | **`questionCount` あり** |
| 保存 | `events.question_count`（非正規化スナップショット） |
| 更新 | 質問 POST 時に原子的 `+1`（`upvote_count` と同発想） |
| 集計 COUNT クエリ | 一覧・詳細の応答組み立てには使わない |

正: `docs/api/openapi-notes.md` / `docs/design/er-diagram.md` / `docs/design/constraints.md`  
Flyway: `V3__events_question_count.sql`

## 実装順への含意

1. **一覧** … `EventSummaryResponse` に件数なし。JOIN COUNT 不要  
2. **詳細** … Entity の `questionCount` を返す（列追加後）  
3. **質問 POST（F-05）** … `UPDATE events SET question_count = question_count + 1 WHERE id = ?`

CLOSED でも質問行は残る。イベント物理 DELETE 時は CASCADE で質問も消え、件数列もイベントと消える。
