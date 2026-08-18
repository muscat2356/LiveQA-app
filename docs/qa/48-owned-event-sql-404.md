# 所有者チェックは SQL（404 一本）

## 方針（反映済み）

レビュー指摘どおり **方針2**: docs を「他人所有も 404」に合わせ、`findByIdAndHost_Id` に寄せる。  
本番コードは本人手書き。契約 docs（openapi / constraints）は更新済み。

## なぜ

- 所有者条件を SQL に寄せる
- `getHost()` の LAZY を所有者判定だけで触らない
- 存在しない／他人を区別せず 404（存在漏洩を避ける）

## 反映箇所

| 層 | 内容 |
|----|------|
| Repository | `findByIdAndHost_Id` |
| Service | `findById` 等で 0件→404 |
| Test | stub / 他人ケースは `NOT_FOUND` |
| openapi / constraints | 403＝ROLE/CSRF、他人所有は 404 |

## 流れ

```
OidcUser → User
  → findByIdAndHost_Id(eventId, hostId)
  → empty → 404
  → あれば Event（所有者がクエリで保証）
```

## 【EJ】

```text
【EJ】公開面最小化 … 「自分のイベント」だけ復元
【EJ】依存 … 派生クエリ名が WHERE id AND host_id
【EJ】あえて外す … 他人と無しを HTTP で分けない（404 一本）
```
