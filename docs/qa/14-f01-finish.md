# F-01 仕上げ（JIT・`/api/me`・logout）

## 質問 / 依頼

F-01 仕上げを **実装** する（B-01）。

## 現状（2026-08 確認）

| 項目 | 状態 |
|------|------|
| Session / Redis / oauth2Login / mapper 差し込み / CSRF | ✅ |
| JIT（`UserRepository` / `UserService.provisionHost` / `OidcUserService` 差し込み） | ✅ |
| `GET /api/me` | ✅ ローカル PK `Long` + `currentHost` + `@PreAuthorize("hasRole('HOST')")` |
| 単体テスト | ✅ `UserServiceTest` 7 / `MeControllerTest` 3（HOST 200・非HOST 403・未認証 401） |
| `POST /api/auth/logout` | ✅ `logoutUrl` + `OidcClientInitiatedLogoutSuccessHandler`（単体は Keycloak 依存のため Compose 疎通向け） |
| 401/403 JSON 文言 | ✅ SecurityConfig が openapi どおり（`Unauthorized` / 「ログインが必要です」、`Forbidden`） |
| CORS / GlobalExceptionHandler | B-13 / B-19（仕上げ本丸の外でも可） |

### 401/403 と GlobalExceptionHandler の役割分担

| 経路 | 担当 | 例 |
|------|------|-----|
| フィルタ連鎖の未認証 / 認可失敗 | **SecurityConfig** の `AuthenticationEntryPoint` / `AccessDeniedHandler` | Cookie 無し `/api/me`、`@PreAuthorize` 失敗 |
| Controller 到達後の業務・検証例外 | **GlobalExceptionHandler**（B-19） | 404 NotFound、`@Valid` 400、409 Conflict |

B-12: 「401/403 の JSON は SecurityConfig 既存を流用・統一」。B-19: 「Security の 401/403 と形が揃っている、または役割分担がコメントで明確」。  
→ **統一するのはレスポンス形（`{ status, error, message }`）と文言契約**。Handler が Security の 401/403 を置き換える想定ではない。

### Q: `{baseUrl}/` はどこへ展開される？フロント（:3000）に戻すには？

**認識どおり、`{baseUrl}` はログアウトを受けたアプリ（backend）のオリジン。** Compose ならだいたい `http://localhost:8081`。フロントの `http://localhost:3000` にはならない。

| 設定 | 展開例（Compose） | 意味 |
|------|-------------------|------|
| `redirect-uri: "{baseUrl}/login/oauth2/code/..."` | `http://localhost:8081/login/oauth2/code/keycloak` | ログイン後の **backend** コールバック |
| `setPostLogoutRedirectUri("{baseUrl}/")` | `http://localhost:8081/` | Keycloak end-session 後の戻り先も **backend** |

フロントへ戻したいなら **設定値で分けるのがよい**（例: `app.frontend-base-url` / `app.logout.post-logout-redirect-uri=http://localhost:3000/`）。ハードコードの `{baseUrl}/` のままでは API ルートに着地する。

あわせて Keycloak client の **Valid post logout redirect URIs** にそのフロント URL を許可する（ログイン用 redirect とは別枠のことが多い）。

### Q: 401/403 の日本語は `getWriter()` 前に UTF-8 明示が要るか？

**要る（明示した方が安全）。** 現状の Controller JSON は MessageConverter 経由で問題になりにくいが、Security の `writeJson` は `HttpServletResponse` に直接書く。

| 経路 | 文字コード |
|------|------------|
| `@RestController` の JSON | Jackson MessageConverter（実質 UTF-8） |
| EntryPoint / AccessDenied の `getWriter()` | **レスポンスの characterEncoding 依存**。未設定だと Servlet 既定の ISO-8859-1 になりうる |

Boot の `CharacterEncodingFilter`（既定 charset=UTF-8）があっても、`force-response` が効いていない／Content-Type だけ先に付くと Writer が UTF-8 にならないことがある。  
→ `setCharacterEncoding(UTF_8)` を **`getWriter()` の前**に置く（実装済み）。

## 推奨写経順

1. `UserRepository` → 2. `UserService`（JIT）→ 3. `OidcUserService` 差し込み  
4. `MeResponse` / `MeController` → 5. logout + end-session → 6. 401/403 文言合わせ

受け入れの正: ラボ `docs/issues/member-b/B-01-f01-auth-finish.md` + `docs/api/openapi-notes.md` 認証節。

## 疎通チェック

1. 初回ログイン後 DB: `SELECT * FROM users;` に HOST 行  
2. `GET /api/me` → `id` が数値、`role` が `HOST`  
3. 未ログイン `/api/me` → 401 `{ status, error:"Unauthorized", message:"ログインが必要です" }`  
4. `POST /api/auth/logout` + CSRF → セッション破棄（＋ Keycloak へ 302 ならなお良い）  
5. HOST 以外で `@PreAuthorize("hasRole('HOST')")` → 403

## MeController 動作確認手順（詳細・2026-08-04）

> **前提:** API は **SESSION Cookie**。Bruno で取った Keycloak の `access_token` を Bearer しても `/api/me` は通らない。

### 0. 起動

| サービス | URL / ポート |
|----------|----------------|
| Keycloak | `http://localhost:8080` |
| Postgres / Redis | Compose |
| backend（ホスト起動） | **`http://localhost:8081`**（KC と 8080 が被るのでずらす） |

```bash
# リポジトリルート
docker compose up -d postgres redis keycloak

cd backend
# .env を用意（issuer は http://localhost:8080/realms/LibraShare）
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Keycloak に `admin_test@example.com` があり **realm role `HOST`** が付いていること。  
IdToken に `realm_access` が無いと 403 になりうる → Client scopes → roles → realm roles で **Add to ID token** ON（[26](26-securityconfig-data-flow.md) Q&A）。

### 1. 未ログイン（401）

```bash
curl -s -i http://localhost:8081/api/me
```

期待:

- HTTP `401`
- JSON: `{ "status": 401, "error": "Unauthorized", "message": "ログインが必要です" }`

（GET なので CSRF ヘッダは不要）

### 2. ブラウザで OIDC ログイン（SESSION 発行）

1. ブラウザで開く:  
   `http://localhost:8081/oauth2/authorization/keycloak`
2. Keycloak ログイン画面 → `admin_test@example.com` 等（HOST 付き）
3. 成功後、backend に戻り **`SESSION` Cookie** が付く（DevTools → Application → Cookies → `localhost:8081`）
4. 同じブラウザでアドレスバーに:  
   `http://localhost:8081/api/me`  
   または DevTools Network で確認

期待 JSON 例:

```json
{
  "id": 1,
  "displayName": "admin test",
  "email": "admin_test@example.com",
  "role": "HOST"
}
```

| フィールド | 出所 |
|------------|------|
| `id` / `role` | DB `users`（JIT + `currentHost`） |
| `displayName` / `email` | セッション内 `OidcUser` クレーム |

### 3. Bruno で同じ確認（Cookie を載せる）

1. ブラウザから `SESSION` の値をコピー  
2. Bruno: `GET http://localhost:8081/api/me`  
3. Cookie: `SESSION=<コピーした値>`（Cookie Jar でも可）  
4. **Authorization Bearer は付けない**

### 4. DB で JIT / role 確認

接続情報（リポジトリルートの `.env`）:

| 項目 | 値 |
|------|-----|
| Host | `localhost` |
| Port | `5432` |
| DB | `live_qa` |
| User | `live_qa_user` |
| Password | `live_qa_password` |

**方法 A — ホストの `psql`**

```bash
PGPASSWORD=live_qa_password psql -h localhost -p 5432 -U live_qa_user -d live_qa
```

**方法 B — Docker コンテナ内**

```bash
docker compose exec postgres psql -U live_qa_user -d live_qa
```

**確認 SQL**

```sql
SELECT id, oidc_subject, role, created_at FROM users ORDER BY id;
```

初回ログイン後に 1 行増え、`oidc_subject` が ID Token の `sub` と一致し、`role` が `HOST` であること。

> **表記の揺れ（要確認）**
>
> | 項目 | `.env`（Compose の正） | `application.yml` デフォルト（ホスト `mvn`） |
> |------|------------------------|-----------------------------------------------|
> | DB | `live_qa` | `live_qa` ✅ 一致 |
> | User | `live_qa_user` | `live_qa` ❌ |
> | Password | `live_qa_password` | `live_qa` ❌ |
>
> `application-docker.yml` に datasource は無い。Compose の `SPRING_DATASOURCE_*`（← `.env`）が上書きする。  
> ホストで `mvn` 起動するときは `POSTGRES_USER` / `POSTGRES_PASSWORD` を `.env` に合わせて export すること。

### 5. 失敗パターンの切り分け

| 結果 | よくある原因 |
|------|----------------|
| 401 | Cookie 無し・期限切れ・別ポートの Cookie |
| 403 Forbidden | `ROLE_HOST` が Authentication に無い（IdToken に `realm_access` 無し等） |
| 500 / IllegalStateException | セッションはあるが `users` に行が無い（JIT 未実行・別 DB） |
| ログイン後すぐエラー | redirect URI 不一致（`http://localhost:8081/*`）/ issuer 不一致 |
| **`role: "UNKNOWN"`** | 下の「UNKNOWN 切り分け」へ |

### `role: "UNKNOWN"` の切り分け（2026-08-04）

**結論（現行コード）: 原因は SecurityConfig ではない。現行 `MeController` は DB の `users.role` を返す。ソースに `"UNKNOWN"` 文字列は無い。**

```text
現行:
  MeController → userService.currentHost → local.getRole()  // DB
  User.hostOf → role = "HOST"

ラボ時代（旧）:
  Authentication.getAuthorities() から ROLE_HOST を探し
  無ければ orElse("UNKNOWN")
  → そのとき初めて SecurityConfig / mapper / IdToken が原因候補
```

| 確認 | 意味 |
|------|------|
| `rg UNKNOWN backend/src` | ヒットしなければ **今のバイナリからは UNKNOWN を出せない** |
| `SELECT id, role FROM users;` | `HOST` なのに応答が UNKNOWN → **古いクラスが動いている**（再ビルド・再起動） |
| DB が `UNKNOWN` | 手で入れた／別経路。`hostOf` は `HOST` しか書かない |
| 200 で `role: HOST` | 応答の role は OK。認可は別（`@PreAuthorize` = Authentication） |

「config が原因」が当たるのは **旧 MeController（authorities → UNKNOWN）を動かしているとき**。その場合の定番は:

1. IdToken に `realm_access` が無い → mapper が `ROLE_HOST` を足せない  
2. mapper が `oauth2Login` に未接続  
3. `OidcUser.getAuthorities()` を読んでしまっている（mapper 結果は `Authentication` 側）

現行設計では SecurityConfig 不備はだいたい **403**（`hasRole('HOST')` 失敗）になり、応答 JSON の `role` には出てこない。

### 403 と Keycloak Mapper の場所（2026-08-04）

`Clients → live-qa-app` の専用タブには **realm roles mapper が無いことが多い**（正常）。  
ロールは **Client scopes → `roles`** 側の組み込み mapper が担当する。

**正しい場所（Keycloak 26）**

1. 左メニュー **Client scopes**（Clients の中ではない）
2. スコープ名 **`roles`** を開く
3. タブ **Mappers**
4. **`realm roles`** を開く → **Add to ID token** を ON → Save

**無い場合（新規作成）**

1. 同上 `roles` → Mappers → **Create mapper** → **By configuration**
2. 種類: **User Realm Role**
3. 設定例:
   - Name: `realm roles`
   - Token Claim Name: `realm_access.roles`
   - Add to ID token: **ON**
   - Add to access token: ON
   - Add to userinfo: ON（任意）
   - Multivalued: ON

**反映確認**

1. Clients → `live-qa-app` → Client scopes → **Evaluate**
2. ユーザーに `admin_test@example.com` を指定
3. **Generated ID token** に `"realm_access": { "roles": [..., "HOST"] }` があるか見る
4. ブラウザの SESSION を捨てて再ログイン → `GET /api/me`

---

## 学習の要点（PR向け）

- JIT はログイン時（`OidcUserService`）に `users` へ。`/api/me` は復元＋DTO組み立てのみ
- 応答の `role` は DB、認可の `ROLE_HOST` は `Authentication`（混ぜない）
- 401/403（フィルタ）は SecurityConfig、業務例外は将来 GlobalExceptionHandler（形だけ揃える）
- `@WebMvcTest` は本番 Security を無理に Import せず、OAuth2Client は exclude（issuer 解決回避）
