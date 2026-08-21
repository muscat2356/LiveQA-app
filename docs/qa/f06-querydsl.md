# F-06 動的クエリ（QueryDSL）

scope: `GET /api/events/{id}/questions` の `sort`（`status` は F-12）。正: [architecture](../architecture.md) / [openapi-notes](../api/openapi-notes.md)。Issue: [#20](https://github.com/muscat2356/live-qa-app/issues/20)。

## 到達点

| 項目 | 状態 |
|------|------|
| 一覧 | **あり**（QueryDSL `searchByEventId`） |
| QueryDSL 依存 | **あり**（`querydsl-jpa` jakarta + apt。Q クラスは `mvn compile` で生成） |
| `sort=popular\|newest` | **あり**（未指定は popular。不正は 400） |
| `status` フィルタ | F-12 |

## この単位（`sort` のみ）

未指定は popular。不正は 400。`BooleanBuilder` には `event.id` だけ。F-08 の PATCH はこのブランチの対象外。
