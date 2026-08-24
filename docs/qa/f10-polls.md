# F-10 ポール作成・開閉

scope: `POST/GET /api/events/{id}/polls`、`PATCH /api/polls/{pollId}/status`。回答・集計は [f11-polls.md](f11-polls.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [er-diagram](../design/er-diagram.md) / [constraints](../design/constraints.md)。 Issue: [#74](https://github.com/muscat2356/live-qa-app/issues/74)。

## 到達点

| 項目 | 状態 |
|------|------|
| `PollStatus` enum | **あり**（`canTransitionTo` は未） |
| `PollType` | **未** |
| Flyway `polls` / `poll_options` / `poll_responses` | **未**（V5） |
| 作成 / 一覧 / 開閉 API | **未** |
| STOMP `POLL_*` | 口だけ残す。配信は C |

## 単位（この順）

1. V5 + `PollType` + `PollStatus.canTransitionTo` + テスト
2. Entity / Repository（`Poll.create`）
3. `POST` 作成 201（HOST・所有者・CSRF）
4. `GET` 一覧（HOST=全件、参加者=OPEN）
5. `PATCH /status`（`DRAFT→OPEN→CLOSED`。終端。409）

F-11（回答 POST / 結果 GET）は混ぜない。`poll_responses` テーブルだけ V5 で先に作る（#74）。

## ルームとの違い

| | ルーム | ポール |
|--|--------|--------|
| 往復 | `OPEN ⇔ CLOSED` | `CLOSED` は終端。再開しない |
| 削除 | `DELETED`（別口） | 物理削除 API は持たない |

DELETED イベントへの作成・一覧は質問と同じく 404。

```text
【EJ】enum … 遷移は canTransitionTo。CLOSED は終端
【EJ】静的ファクトリ … 次単位で Poll.create
【EJ】あえて外す … 配信は C。今は口だけ
```
