# 匿名セッション / voter_key（余力）

## なぜ作るか

参加者は Keycloak ログインしない。  
**無いと困る点**: 「1人1票」の判定キー（`voter_key`）を載せる箱（セッション）が無い。

ラボでは「匿名セッションを作って `g:{guestId}` を返す」まで。いいね UNIQUE や DB は不要。

## データの流れ

```text
POST /api/auth/guest  +（要）X-XSRF-TOKEN
  → 検証: CSRF
  → セッションを確保（無ければ新規発行）
  → guestId（UUID）を生成
  → 格納: HttpSession 属性 "guestId" に書き込む
  → 変換: voterKey = "g:" + guestId
  → 返却: 200 { guestId, voterKey }
  → 発行済みの SESSION / XSRF-TOKEN Cookie が付く（または更新）
```

| 操作語 | この場での意味 |
|--------|----------------|
| **格納** | サーバ側セッション（→ Redis）に `guestId` を書き込む |
| **変換** | `guestId` から本番制約どおりの `voter_key` 文字列を作る |
| **発行** | まだ無ければ `SESSION` Cookie をクライアントへ渡す |

## Security 側

`authorizeHttpRequests` で `/api/auth/guest` を **permitAll**（ログイン不要）。  
POST なので CSRF は有効のまま（匿名参加者にも CSRF を発行する、という本番方針）。

## ライブラリ／型

| 名前 | 正体 |
|------|------|
| `HttpSession` | サーブレットのセッション契約。Spring Session 利用時は中身が Redis に載る |
| `UUID` | 乱数の一意 ID。ゲスト識別子に使う |
| `@RestController` | JSON API 用のアノテーション |

## 疎通

1. ブラウザで一度 `GET http://localhost:8080/`（または任意）→ `XSRF-TOKEN` 取得  
2. Console:

```js
fetch('/api/auth/guest', {
  method: 'POST',
  headers: { 'X-XSRF-TOKEN': document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1] }
}).then(r => r.json()).then(console.log);
```

期待: `{ guestId: "...", voterKey: "g:..." }` かつ Cookie `SESSION` あり。

---

## Q: HOST の SESSION とゲストは同じ箱？

ラボ最小では **同じ SESSION Cookie 機構**を使う。  
属性として `guestId` を載せるだけ。本番では参加フローで発行タイミングが決まるが、箱の仕組みは同じ（Spring Session + Redis）。
