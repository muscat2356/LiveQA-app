# F-12 質問フィルタ

scope: `GET /api/events/{id}/questions?status=`。並び `sort` は [f06-querydsl.md](f06-querydsl.md)。  
正: [openapi-notes](../api/openapi-notes.md)。 Issue: [#22](https://github.com/muscat2356/live-qa-app/issues/22)。

## 到達点

| 項目 | 状態 |
|------|------|
| `sort` | **F-06 済** |
| `status` クエリ | **未**（今は WHERE に載せない。ARCHIVED も返す） |

## 契約

| クエリ | 意味 |
|--------|------|
| なし | `ARCHIVED` 以外（OPEN + ANSWERED） |
| `status=OPEN` | 未回答 |
| `status=ANSWERED` | 回答済み |
| `status=ARCHIVED` | アーカイブ（主催者のモデレーション確認用） |
| 不正 | 400 |

`sort` と併用可。QueryDSL の `BooleanBuilder` に `and` するだけ。

```text
【EJ】ジェネリクス … Predicate の合成。null なら足さない
```
