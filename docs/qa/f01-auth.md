# F-01 認証・セッション

scope: Keycloak OIDC → Cookie Session（Redis）→ `/api/me` / logout / CSRF / JIT。  
正: [openapi-notes](../api/openapi-notes.md) / [architecture](../architecture.md)。  
深いカタログ（Filter 列・型）: [05-security-config.md](05-security-config.md) / [06-me-controller.md](06-me-controller.md) / [26-securityconfig-data-flow.md](26-securityconfig-data-flow.md)。  
Keycloak realm 専用 `live-qa`・URL 2種: [ops.md](ops.md)。

## 到達点

| 項目 | 状態 |
|------|------|
| oauth2Login / mapper 差し込み / CSRF / Session+Redis | あり。guest POST は ignore（#104 / [f04](f04-guest.md)） |
| JIT（`provisionHost`） | あり |
| `POST /api/auth/register`（HOST メール発行） | **未**（[#122](https://github.com/muscat2356/live-qa-app/issues/122)。契約は [openapi-notes](../api/openapi-notes.md)。ローカル SMTP・専用 realm は済み） |
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

## Q. HTTPS にすると Cookie / SSO はどうなる？

**通信の中身を暗号化し、Cookie に `Secure` を付ける。** 証明書は各コンテナではなく、前段のプロキシ（Caddy など）で止めるのが無料枠向き。OIDC の `{baseUrl}` と Keycloak の redirect は `https://...` に揃える。詳細・フロント本番ビルドは [ops.md](ops.md) の「HTTPS とフロント本番」。

## Q. Cookie 跨ぎと CORS は同じ？

**別レイヤ。** Next BFF ならブラウザ↔Spring の CORS は避けられる。残るのは Cookie の転送（Set-Cookie をブラウザが覚えるか、次リクエストで Spring まで届くか）。ログインコールバックはブラウザが SESSION を直受けするので発行漏れは起きにくい。その後 Next サーバが `/api/me` を呼ぶなら Cookie を明示転送する。

## Q. CSRF は Controller にどう書く？

**書かない。** `SecurityConfig` の `CsrfFilter` が POST/PUT/PATCH/DELETE を自動検証。クライアントは Cookie `XSRF-TOKEN` の値をヘッダ `X-XSRF-TOKEN` に載せる。テストは `.with(csrf())`。`addFilters=false` のスライスは CSRF 対象外（配線だけ見る）。

## Q. `@PreAuthorize` 失敗が 500 になる？

`AuthorizationDeniedException` が catch-all に飲まれると 500。`@ExceptionHandler` を認可用に分ける。catch-all 内の if より、型ごとの Handler。

## Q. 応答の `role: UNKNOWN`？

現行は DB `users.role` を返す。ソースに `"UNKNOWN"` は無い。旧実装（authorities から探す）の名残。認可不足はだいたい **403**。IdToken に `realm_access` が無いと mapper が `ROLE_HOST` を足せない → Client scopes → `roles` → `realm roles` → **Add to ID token ON**。

## Q. 未ログインで `/dashboard` を直打ちすると `/login` で止まる？（#115）

**Next が存在しない `/login` に 302 している。** 一覧 SSR が `GET /api/events` の 401 で `redirect("/login")`。フロントにそのページは無い。正の導線はトップ `/` の Keycloak ボタン（`/oauth2/authorization/keycloak`）。専用ログインフォームは持たない。

詳細・Q&A は `/login` に行かず 401 を throw する（別経路）。改善は 401 なら `redirect("/")` に揃える。Issue: [#115](https://github.com/muscat2356/live-qa-app/issues/115)。

## Q. メールだけで主催者登録するか？（方針・2026-08-27）

**やる。Keycloak にユーザーを作り、Spring にパスワードを持たない。** scope の「自前の登録画面は持たない」は「Spring が会員マスタ＋パスワード検証を持たない」の意味。メール入力はフロント（A）、発行は `POST /api/auth/register`（B / Admin API）。

一次パスワードを `temporary: true` で入れる方式は採らない。execute-actions-email（`UPDATE_PASSWORD`）でリンクだけ送る。

realm `live-qa` は `registrationAllowed: false`。Keycloak の公開自己登録は使わない。参加者は Keycloak に作らない。公開 API なので、メールを知っている人は HOST になれる（招待コードは今は外す）。

登録は HOST のみ。ローカルで専用 realm `live-qa` を通してから OCI に載せる。登録 API は [#122](https://github.com/muscat2356/live-qa-app/issues/122)。

JSON・yml・volume は [ops.md](ops.md) の「専用 realm `live-qa`」。compose は `live-qa-realm.json` だけを import する。

### Q. import JSON の初期ユーザーは誰？

**開発用 HOST 1 人。** `host@example.com` / `live-qa-dev-host`、realm role `HOST`。ログインして JIT すればローカル `users` ができる。SMTP の秘密は載せない。既存 volume には入らないので、入れるときは `docker compose down -v`。本番ではパスワードを変えるか、登録 API の人だけ使う。

### Q. デプロイ手順の「2. SMTP を入れて Admin からテストメール」とは？

**Keycloak が郵便を出せるようにする作業。アプリのコードではない。** パスワード設定リンクは Spring ではなく Keycloak が送る。送り先の郵便局（SMTP）が空だと、登録 API が成功してもメールは届かない。

いまの import JSON は `"smtpServer" : { }`（空）。Admin の Realm settings → Email にホスト・ポート・From・認証を書く。Save のあと **Test connection** / **Send test email** で、自分のアドレスに1通来れば 2 は完了。

ローカルと OCI で同じ口（Gmail など）を使うと、載せたあとに郵便局だけ変えなくて済む。Mailhog は受け専用の素振り。クラウドが 25 番を塞いでいることが多いので **587（STARTTLS）** を使う。

### Q. Keycloak の SMTP は画面のどこに何を入れる？（2026-08-27）

**JSON には書かない。** Admin の Realm settings → Email。保存先は Keycloak の DB（volume）。`docker compose down -v` すると消えるので、作り直したら入れ直す。

1. `http://localhost:8080` → 左上 realm を **`live-qa`**
2. **Realm settings → Email**
3. 保存 → **Test connection** → **Send test email**（自分のアドレス）

Gmail の例（ローカル＝本番で同じ口にするとき）:

| 欄 | 値 |
|----|-----|
| From | その Gmail アドレス（From が違うと拒否されやすい） |
| Host | `smtp.gmail.com` |
| Port | `587` |
| Enable StartTLS | ON |
| Enable Authentication | ON |
| Username | 同じ Gmail アドレス |
| Password | **アプリパスワード**（アカウントのログインパスワードではない） |

アプリパスワード: Google アカウント → セキュリティ → 2段階認証が ON → 「アプリ パスワード」。16 文字をスペース無しで貼る。

完了の合図: テストメールが 1 通、受信箱（または迷惑メール）に来る。

よくある失敗:

| 症状 | 見ること |
|------|----------|
| Authentication failed | 普通のパスワードを入れている。アプリパスワードにする |
| Could not convert socket to TLS | 587 なのに SSL 側を ON にしている。StartTLS だけ |
| Connection refused / timeout | 25 番を使っている。587 にする。Keycloak **コンテナ**から外へ出られるか |
| テストは成功、登録メールが来ない | From 未設定。ユーザーの Email が空 |

Mailhog で受けるだけなら Host は compose のサービス名（例 `mailhog`）、Port `1025`、認証オフ。いまの `compose.yml` には Mailhog は無い。外に届く口が要るなら Gmail 側で足りる。

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
