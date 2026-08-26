# F-12 質問フィルタ

scope: `GET /api/events/{id}/questions?status=`。並び `sort` は [f06-querydsl.md](f06-querydsl.md)。  
正: [openapi-notes](../api/openapi-notes.md)。 Issue: [#22](https://github.com/muscat2356/live-qa-app/issues/22)。

## 到達点（2026-08-26）

| 項目 | 状態 |
|------|------|
| `sort` | **F-06 済** |
| `status` クエリ | **済み**（未指定は ARCHIVED 以外。不正は 400） |
| `status=ARCHIVED` の認可 | **F-13**（イベント所有者 HOST のみ。他は 403） |

## 契約

| クエリ | 意味 |
|--------|------|
| なし | `ARCHIVED` 以外（OPEN + ANSWERED） |
| `status=OPEN` | 未回答 |
| `status=ANSWERED` | 回答済み |
| `status=ARCHIVED` | アーカイブ（**イベント所有者 HOST のみ**。他は 403） |
| 不正（`open` 含む） | 400 |

`sort` と併用可。QueryDSL の `BooleanBuilder` に `and` する。

## なぜ / 流れ

参加者の既定一覧からアーカイブを隠す（F-13 の見え方）。確認は `?status=ARCHIVED`。**所有者 HOST 以外は 403**（[f13-moderation.md](f13-moderation.md)）。新パスは作らない。

```text
GET /api/events/10/questions?sort=newest&status=OPEN
  → Controller は sort / status の文字列を渡すだけ
  → QuestionSort.form / QuestionStatus.form
  → BooleanBuilder
       必須: event.id = 10
       指定あり: AND status = OPEN
       未指定:   AND status <> ARCHIVED
  → ORDER BY は F-06 のまま
  → votedByMe は別クエリ（混ぜない）
```

architecture の「null はスキップ」は一般論。この API の未指定は「フィルタ無し（ARCHIVED も含む）」ではなく **ARCHIVED 以外**。WHERE を省略しない。

`QuestionStatus.form` は `Optional`。empty＝未指定。Repository に `null` の enum を渡さない。値は openapi どおり大文字（`OPEN`）。`@RequestParam QuestionStatus` にはしない（不正が変換例外になり、`sort` と口が揃わない）。

```text
【EJ】enum … クエリは QuestionStatus.form。valueOf 直投げで 500 にしない
【EJ】Optional … 未指定は empty。Repository に null の enum を渡さない
【EJ】公開面最小化 … BooleanBuilder は QuestionQueryImpl。Controller に出さない
【EJ】あえて外す … votedByMe は F-06 どおり別クエリ
```
