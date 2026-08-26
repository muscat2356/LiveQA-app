# F-13 簡易モデレーション

scope: 不適切質問を隠す。**新 API は作らない。**  
正: [f08-status.md](f08-status.md) の `ARCHIVED` ＋ [f12-filter.md](f12-filter.md) の既定（ARCHIVED 以外）。 Issue: [#23](https://github.com/muscat2356/live-qa-app/issues/23)。

## 到達点

| 項目 | 状態 |
|------|------|
| `PATCH .../questions/{qid}/status` → `ARCHIVED` | **F-08 済** |
| 参加者一覧から既定で隠す | **F-12 済**（未指定は ARCHIVED 以外。`?status=ARCHIVED` で確認可） |

## 契約

- HOST 所有者が `ARCHIVED` にする（403 / 404 は F-08 と同じ）
- 専用パス・専用ステータスは足さない
- 時間不足なら F-12 を優先。F-08 だけでも「モデレーション相当」（scope の削る順）

```text
【EJ】公開面最小化 … パスを増やさない
```
