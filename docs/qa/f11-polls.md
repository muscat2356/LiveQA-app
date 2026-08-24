# F-11 ポール回答・集計

scope: `POST /api/polls/{pollId}/responses`、`GET /api/polls/{pollId}/results`。作成・開閉は [f10-polls.md](f10-polls.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md)。 Issue: [#21](https://github.com/muscat2356/live-qa-app/issues/21)。

## 到達点

| 項目 | 状態 |
|------|------|
| 回答 POST | **未** |
| 結果 GET | **未** |
| `POLL_UPDATED` | 口だけ。配信は C |

## 契約（要約）

- ポール `OPEN` **かつ** ルーム `OPEN` だけ回答可。それ以外は 409
- `voter_key` はセッション。未確立は 401
- `SINGLE` は 1 件。既存があれば **409**（置換しない）
- `MULTI` は 1〜選択肢数。同じ option の二重は UNIQUE で 409
- JSON は F-10 と同じ PollDTO
- イベント `DELETED` は 404

```text
【EJ】例外 … 二重は 409。いいねと同じ口
【EJ】あえて外す … 配信は C
```
