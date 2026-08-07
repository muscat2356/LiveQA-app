# Bruno で SESSION Cookie 付き GET を確認する

## 質問

GET メソッドを Bruno で、SESSION を使って確認する方法は？

## 結論

F-01 の保護 GET（例: `/api/me`）は **Bearer ではなく `Cookie: SESSION=...`**。  
ブラウザで OIDC ログインして付いた `SESSION` 値を Bruno に載せ、同じ backend オリジンへ GET する。

| 付けてよい | 付けない |
|------------|----------|
| `Cookie: SESSION=<値>` | `Authorization: Bearer <access_token>`（通らない） |
| （任意）Cookie Jar | CSRF ヘッダ（**GET では不要**） |

## なぜ Bearer ではダメか

```text
Keycloak ログイン成功
  → 交換: コード→トークン（Spring 内部）
  → 格納: SecurityContext を Redis の箱へ
  → 発行: Set-Cookie SESSION=箱の鍵

以降の API
  → 復元: SESSION で箱を開く
```

Bruno で取った Keycloak `access_token` は「IdP の証明」であり、Spring は **毎回 JWT を見ない**設計。鍵は Cookie。

## 手順（GET /api/me）

### 0. 前提

- backend 例: `http://localhost:8081`（Keycloak が 8080 のとき）
- Keycloak / Redis / Postgres 起動済み
- HOST ロール付きユーザーでログインできる

### 1. 未ログイン確認（対照）

Bruno:

- Method: `GET`
- URL: `http://localhost:8081/api/me`
- Auth: None
- Cookie: 無し

期待: **401**  
`{ "status": 401, "error": "Unauthorized", "message": "ログインが必要です" }`

### 2. ブラウザで SESSION を発行

1. ブラウザで  
   `http://localhost:8081/oauth2/authorization/keycloak`
2. Keycloak ログイン（HOST 付き）
3. 成功後 DevTools → **Application** → Cookies → `http://localhost:8081`
4. 名前 `SESSION` の **Value をコピー**

注意:

- `SESSION` は **HttpOnly** → コンソールの `document.cookie` には出ない。Application タブから取る
- ログイン後リダイレクトで `:3000/dashboard` に飛んでも、Cookie の発行元は backend。見るホストは **API のオリジン**（例: `localhost:8081`）

### 3. Bruno に SESSION を載せる

**方法 A（ヘッダ直書き）**

- Headers に:
  - Name: `Cookie`
  - Value: `SESSION=ここに貼る`

**方法 B（Bruno の Cookie UI）**

- リクエストの Cookie タブ / Cookie Jar で  
  - Name: `SESSION`  
  - Value: （コピーした値）  
  - Domain: `localhost`（またはリクエスト先に合わせる）

Authorization は **None** のまま。

### 4. 送信して確認

期待: **200**

```json
{
  "id": 1,
  "displayName": "...",
  "email": "...",
  "role": "HOST"
}
```

| フィールド | 出所 |
|------------|------|
| `id` / `role` | DB `users` |
| `displayName` / `email` | セッション内 OIDC クレーム |

### 5. 壊し方（理解用）

| 操作 | 期待 |
|------|------|
| Cookie を外す | 401 |
| `SESSION` を適当な文字に変える | 401（箱が開けない） |
| Bearer だけ付ける | 401（SESSION 復元しない） |

## GET と POST の違い（Bruno）

| | GET `/api/me` | POST（logout 等） |
|--|---------------|-------------------|
| SESSION | 必要 | 必要 |
| CSRF | **不要** | `XSRF-TOKEN` Cookie + ヘッダ `X-XSRF-TOKEN` が必要 |

GET 確認では CSRF を気にしなくてよい。

## データの流れ（Bruno 視点）

```text
Bruno GET /api/me + Cookie: SESSION=abc
  → SessionRepositoryFilter が Redis から箱を 復元
  → SecurityContextHolderFilter が SecurityContext を復元
  → authenticated + @PreAuthorize HOST
  → MeController → 200 JSON
```

## よくあるつまずき

| 症状 | 原因 |
|------|------|
| ずっと 401 | Cookie 未設定 / 値のコピー漏れ / ポート違い（8080 と 8081） |
| 403 | SESSION はあるが `ROLE_HOST` が無い（IdToken の realm roles） |
| Bearer で試して落ちる | 設計どおり。SESSION に切り替える |
| Cookie が取れない | HttpOnly なので Application タブを使う |

## 関連

- 手順の親: [14-f01-finish.md](14-f01-finish.md)（Me 動作確認）
- Bearer では不可: [26-securityconfig-data-flow.md](26-securityconfig-data-flow.md) Bruno Q&A
- SESSION の意味: [02-session-cookie.md](02-session-cookie.md)
