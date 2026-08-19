# F-01 認証・セッション

scope: Keycloak OIDC → Cookie Session（Redis）→ `/api/me` / logout / CSRF / JIT。  
正: [openapi-notes](../api/openapi-notes.md) / [architecture](../architecture.md)。  
深いカタログ（Filter 列・型）: [05-security-config.md](05-security-config.md) / [06-me-controller.md](06-me-controller.md) / [26-securityconfig-data-flow.md](26-securityconfig-data-flow.md)。  
Keycloak realm 相乗り・URL 2種: [ops.md](ops.md)。

## 到達点

| 項目 | 状態 |
|------|------|
| oauth2Login / mapper 差し込み / CSRF / Session+Redis | あり |
| JIT（`provisionHost`） | あり |
| `GET /api/me`（ローカル PK + `currentHost` + `@PreAuthorize HOST`） | あり |
| `POST /api/auth/logout` | あり |
| ログイン後 → フロント `/dashboard` | あり（`successHandler`） |
| 401/403 JSON | SecurityConfig（Filter）。業務例外は GlobalExceptionHandler |

## データの流れ

```text
ブラウザ → /oauth2/authorization/keycloak
  → Keycloak ログイン → コールバックで認可コードをトークンに交換
  → delegate.loadUser（標準 OidcUserService）で OidcUser を組み立て
  → provisionHost(sub) … users を検索、無ければ User.hostOf で格納（JIT）
  → mapper が realm_access の HOST → ROLE_HOST
  → SESSION Cookie 発行（箱は Redis）→ 302 /dashboard
  → GET /api/me … Session 復元 → requireByOidcSubject → MeResponse
```

Cookie / Redis / SecurityContext の復元は以降の API でも同じ。JWT を毎リクエスト検証しない。

## 役割分担

| 層 | する | しない |
|----|------|--------|
| SecurityConfig | Filter・CSRF・EntryPoint/AccessDenied・mapper 接続 | 業務分岐 |
| UserService | JIT / 復元 / MeResponse 組み立て | `ResponseEntity` |
| MeController | principal 受け取り、`ResponseEntity.ok` | DTO 組み立て本体 |

401/403（未ログイン・権限不足・CSRF）は **Filter**。Controller 到達後の 400/404/409 は **Advice**。形 `{ status, error, message }` は揃える。

---

## Q. JIT とは？

**ログインしたその瞬間に、必要ならローカル `users` 行を作ること。** Keycloak の `sub` は FK に直接載せない（`host_id → users.id`）。2回目以降は find だけ。displayName / email は DB に保存せずクレームから都度返す。

## Q. UserService はいつ動くか / oidcSubject はどこから？

| いつ | メソッド | 入力 |
|------|----------|------|
| ログイン・コールバック | `provisionHost` | `oidcUser.getSubject()`（ID Token の `sub`） |
| `GET /api/me` | `requireByOidcSubject` → `toMeResponse` | 同じ sub（SESSION 内 OidcUser） |

UserService は Session 箱を検証しない。箱の復元は Filter。Service は **sub → users 行**。

## Q. `delegate.loadUser` とは？

Spring 標準 `OidcUserService` への**委譲**。トークン検証・クレーム組み立ては標準に任せ、差分は JIT 1箇所。`userRequest` は自分で new しない（oauth2Login が渡す）。毎 API では走らない。

## Q. oidcUserService と mapper は両方呼ばれる？

**呼ばれる。順序は loadUser（＋JIT）のあと、Provider が mapper。** mapper を Bean にしただけでは足りない。`userAuthoritiesMapper(...)` で oauth2Login に接続する。

## Q. HOST 表示名はローカル列が要るか？

**F-01 では足さない。** 正はクレーム都度。日本語が IdP に載せられないと確定してから er-diagram / openapi を先に更新する。参加者の `questions.display_name` は別物。

## Q. ログイン後のリダイレクトは？

| 用途 | URL（開発） |
|------|-------------|
| ログイン開始（A のボタン） | `http://localhost:8081/oauth2/authorization/keycloak` |
| 成功後の着地 | `http://localhost:3000/dashboard` |
| ログアウト後 | `http://localhost:3000/` |

`{baseUrl}` は **backend オリジン**。フロントへ戻すなら設定値で分ける。Keycloak の Valid post logout redirect URIs にもフロントを許可する。

## Q. Cookie 跨ぎと CORS は同じ？

**別レイヤ。** Next BFF ならブラウザ↔Spring の CORS は避けられる。残るのは Cookie の転送（Set-Cookie をブラウザが覚えるか、次リクエストで Spring まで届くか）。ログインコールバックはブラウザが SESSION を直受けするので発行漏れは起きにくい。その後 Next サーバが `/api/me` を呼ぶなら Cookie を明示転送する。

## Q. CSRF は Controller にどう書く？

**書かない。** `SecurityConfig` の `CsrfFilter` が POST/PUT/PATCH/DELETE を自動検証。クライアントは Cookie `XSRF-TOKEN` の値をヘッダ `X-XSRF-TOKEN` に載せる。テストは `.with(csrf())`。`addFilters=false` のスライスは CSRF 対象外（配線だけ見る）。

## Q. `@PreAuthorize` 失敗が 500 になる？

`AuthorizationDeniedException` が catch-all に飲まれると 500。`@ExceptionHandler` を認可用に分ける。catch-all 内の if より、型ごとの Handler。

## Q. 応答の `role: UNKNOWN`？

現行は DB `users.role` を返す。ソースに `"UNKNOWN"` は無い。旧実装（authorities から探す）の名残。認可不足はだいたい **403**。IdToken に `realm_access` が無いと mapper が `ROLE_HOST` を足せない → Client scopes → `roles` → `realm roles` → **Add to ID token ON**。

## 疎通（SESSION。Bearer では `/api/me` は通らない）

1. `curl -s -i http://localhost:8081/api/me` → 401  
2. ブラウザで OIDC ログイン → Cookie `SESSION`  
3. 同じ Cookie で `GET /api/me` → `id` 数値・`role: HOST`  
4. `SELECT id, oidc_subject, role FROM users;`  
5. POST は `SESSION` + `X-XSRF-TOKEN` + `Content-Type`

ホスト `mvn` 時は Keycloak が 8080 なので backend は **8081**。DB ユーザーは Compose `.env`（`live_qa_user`）に合わせる。

## テスト

| 層 | 見るもの |
|----|----------|
| `UserServiceTest` | JIT 新規/既存、require、MeResponse |
| `MeControllerTest` | HOST 200 / 非 HOST 403 / 未認証 401。本番 Security は Import しない。OAuth2Client は exclude |

```text
【EJ】静的ファクトリ … User.hostOf
【EJ】依存性注入 … UserService コンストラクタ
【EJ】不変性 … MeResponse は record
【EJ】公開面最小化 … Controller は薄く、組み立ては Service
【EJ】あえて外す … Entity は JPA で可変
```
