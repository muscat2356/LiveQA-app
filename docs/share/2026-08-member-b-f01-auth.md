# 共有会資料 — F-01 認証・セッション（網羅版 / Java 向け）

| 項目 | 内容 |
|------|------|
| 機能 ID | **F-01**（MVP-A 死守） |
| 担当 | B 主 / A=UI / C=Compose・Session共有 |
| 想定時間 | **本編 30〜35分** ＋ Q&A 10分（合計 ~45分） |
| 聞き手 | Java / Spring 経験者 |
| 契約の正 | [openapi-notes 認証節](../api/openapi-notes.md) / [scope F-01](../scope.md) |
| 学習メモ | `docs/qa/02,05,06,07,09,14–18,23,25,26` |

設計の核:

> **Keycloak(OIDC) でログイン → Cookie セッション（Redis）で以降を管理。**  
> JWT を API ごとに検証する Resource Server ではない。CSRF 有効。HOST と匿名参加者の二系統。

---

## アジェンダ（時間配分）

| # | 節 | 分 | 内容 |
|---|-----|-----|------|
| 0 | ゴールと F-01 定義 | 2 | scope / 受け入れ |
| 1 | アーキ選択 | 3 | Login+Session vs JWT RS |
| 2 | 登場人物と Cookie | 3 | SESSION / XSRF / Redis |
| 3 | エンドポイント一覧 | 3 | openapi 認証節の全部 |
| 4 | ログイン一連（OIDC→JIT→Session） | 6 | 型・OidcUserService・mapper |
| 5 | GET /api/me | 4 | 層・DTO・@PreAuthorize |
| 6 | CSRF | 3 | repo / handler |
| 7 | 401/403 と Filter 境界 | 3 | EntryPoint vs Advice |
| 8 | logout（RP-initiated） | 3 | end-session・戻り先 |
| 9 | 匿名参加（guest） | 2 | voter_key |
| 10 | Keycloak / Compose / テスト | 3 | realm・CI |
| 11 | A/C 接点・学び・次 | 3 | |
| — | Q&A | 10 | |

短く切る場合: **0→1→3→4→5→8→11**（約20分）＋詳細は質疑。

---

## 0. F-01 とは何か（網羅チェックリスト）

[scope.md](../scope.md) より:

| 要求 | 実装・状態 |
|------|------------|
| Keycloak SSO(OIDC) ログイン | ✅ `oauth2Login` |
| Cookie セッション（Spring Session + Redis） | ✅ `SessionConfig` `@EnableRedisHttpSession` |
| 会員の自動作成（JIT、自前登録画面なし） | ✅ `provisionHost` |
| ログアウト | ✅ `/api/auth/logout` + OIDC end-session |
| RBAC（HOST / 参加者=匿名） | ✅ mapper + `@PreAuthorize` / guest |
| CSRF 有効化 | ✅ `CookieCsrfTokenRepository` + handler |

openapi 認証節のエンドポイント:

| エンドポイント | 役割 | 状態 |
|----------------|------|------|
| `GET /oauth2/authorization/keycloak` | ログイン開始（302→IdP） | ✅ Spring 標準 |
| `GET /login/oauth2/code/keycloak` | コールバック・交換・JIT・Session | ✅ + カスタム UserService |
| `GET /api/me` | 現在ユーザー（HOST） | ✅ |
| `POST /api/auth/logout` | ログアウト（CSRF） | ✅ |
| `POST /api/auth/guest` | 匿名セッション（参加者） | ✅ 最小実装 |

受け入れ疎通（[14](../qa/14-f01-finish.md)）:

1. 初回ログイン後 `users` に HOST 行  
2. `/api/me` → 数値 `id` + `role: HOST`  
3. 未ログイン → 401 JSON（openapi 文言）  
4. logout + CSRF → セッション破棄（＋ Keycloak 302）  
5. 非 HOST → 403  

---

## 1. アーキ選択（なぜこの形か）

| | Resource Server（例: 一部の LibraShare） | **F-01 live-qa** |
|--|------------------------------------------|------------------|
| 設定 | `oauth2ResourceServer().jwt()` | `oauth2Login()` |
| 毎リクエスト | Bearer JWT 検証 | Session から `SecurityContext` 復元 |
| ブラウザ | Authorization ヘッダ等 | **`SESSION` Cookie** |
| CSRF | しばしば無効 | **有効**（stateful） |
| WebSocket | トークン別設計になりがち | 同じ Cookie で握手しやすい（C） |

Authorization Code（confidential client `live-qa-app`）:

```text
Browser → authorization
       → IdP login
       → code
       → token exchange（サーバ間）
       → OidcUser + ROLE_* + HttpSession
       → Redis（Spring Session）
       → Set-Cookie: SESSION
```

**ID Token = ログイン処理の材料。以降の API 媒体ではない。**

---

## 2. 登場人物・Cookie・Redis

### 2.1 鍵と箱（[02](../qa/02-session-cookie.md)）

| 名前 | 置き場 | 役割 |
|------|--------|------|
| セッション箱 | **Redis**（namespace `live-qa`） | SecurityContext 等 |
| `SESSION` Cookie | ブラウザ | 箱の**鍵**（HttpOnly） |
| `XSRF-TOKEN` Cookie | ブラウザ | CSRF 合い言葉（**HttpOnly false** → JS 可読） |
| `SecurityContext` | リクエスト中は Thread、永続は Session | 認証状態 |

よくある誤解:

- Cookie にユーザー名 → **入っていない**  
- セッションがある＝ログイン済み → **違う**（箱に Authentication が入って初めて HOST）

### 2.2 Redis への格納はどのメソッドか

**自前の `redis.set` メソッドはない。**

```text
ログイン成功
  → Security が Authentication を SecurityContext / HttpSession へ
  → リクエスト終了付近で Spring Session（SessionRepositoryFilter 等）が Redis へ flush
```

有効化だけがアプリコード:

```java
@EnableRedisHttpSession(redisNamespace = "live-qa")  // SessionConfig
```

復元: 次リクエストで `SESSION` → Redis → 再び `SecurityContext`。

---

## 3. エンドポイント一覧（F-01 契約）

### 3.1 `GET /oauth2/authorization/keycloak`（公開）

- フロント「主催者ログイン」のリンク先  
- Keycloak 認可へ **302**（JSON なし）

### 3.2 `GET /login/oauth2/code/keycloak`（公開・コールバック）

- 認可コード受け取り → トークン交換  
- IDトークン `sub` で **JIT**  
- Session 作成 → `Set-Cookie: SESSION`  
- ダッシュボード等へリダイレクト（JSON なし）

### 3.3 `GET /api/me`（HOST）

```json
{ "id": 1, "displayName": "...", "email": "...", "role": "HOST" }
```

- `id` / `role` ← DB `users`  
- `displayName` / `email` ← **クレーム（DB 非保持）**  
- 401: `{ status, error:"Unauthorized", message:"ログインが必要です" }`

### 3.4 `POST /api/auth/logout`（HOST・CSRF）

- ローカル Session 破棄  
- 既定: RP-initiated → Keycloak end-session → **フロントへ 302**  
- 代替: ローカルのみなら 204  

### 3.5 `POST /api/auth/guest`（公開・CSRF）

- 匿名 Session + `guestId`  
- 応答: `{ guestId, voterKey: "g:{id}" }`  
- 参加者の 1人1票キー（F-04/F-05 で本格利用）

---

## 4. ログイン一連（本編の技術コア）

### 4.1 流れ（操作語＋形）

```text
① 交換 … code → tokens → OidcUserRequest
② 検証・組み立て … OidcUserService.loadUser → OidcUser
③ JIT … provisionHost(sub) → users 行（無ければ INSERT）
④ 変換 … mapper: realm_access "HOST" → ROLE_HOST
⑤ 格納 … Authentication → HttpSession → Redis
⑥ 発行 … SESSION / XSRF-TOKEN Cookie
```

### 4.2 型グラフ（ログイン完了時）

```text
SecurityContext
  └── Authentication
        ├── principal: OidcUser
        │     getSubject() → String (= JWT sub)
        └── authorities: Collection<GrantedAuthority>
              ├── OidcUserAuthority   // トークンへのハンドル
              ├── SCOPE_*
              └── ROLE_HOST           // mapper 追加
```

これが Session 属性として Redis に残る（§2.2）。

### 4.3 `OidcUserService`（標準）とカスタムラッパ

**標準 `OidcUserService`:** IDトークン検証・クレーム読取・必要なら UserInfo → `OidcUser`。

**このリポ:**

```text
userRequest
  → delegate.loadUser(userRequest)   // Spring
  → userService.provisionHost(sub)   // JIT（戻り User は捨てる）
  → return oidcUser                  // principal は OidcUser のまま
```

Entity を principal にしない → Session に JPA エンティティを載せない。

### 4.4 JIT（[15](../qa/15-jit-provisioning.md)）

| 世界 | 持つもの |
|------|----------|
| Keycloak | `sub`・名前・email・realm ロール |
| DB `users` | 数値 `id`（将来 `events.host_id` の FK） |

```java
provisionHost(sub):
  findByOidcSubject(sub).orElseGet(() → save(User.hostOf(sub)))
```

`/api/me` は **復元**（`requireByOidcSubject`）。通常ここで INSERT しない。  
JIT 無し → ログイン成功しても数値 id / host_id が作れない。

### 4.5 `GrantedAuthoritiesMapper`

```text
入力: Collection<GrantedAuthority>（JWT 生バイトではない）
  → HashSet にコピー
  → OidcUserAuthority なら realm_access を読む
  → "HOST" → SimpleGrantedAuthority("ROLE_HOST")
```

- `hasRole("HOST")` ⇔ authority `"ROLE_HOST"`  
- **Bean だけ置いても不足。** `oauth2Login.userInfoEndpoint.userAuthoritiesMapper(...)` で差し込み必須  
- IdToken / UserInfo / attributes の三重読みは **冗長候補**（正は IdToken の realm role）  
- 将来 client role → `resource_access.{clientId}.roles`

### 4.6 mapper 差し込み（コード上の位置）

```java
.oauth2Login(oauth2 → oauth2
    .userInfoEndpoint(ui → ui
        .oidcUserService(oidcUserService())
        .userAuthoritiesMapper(grantedAuthoritiesMapper())
    )
)
```

---

## 5. GET /api/me

### 5.1 層分担（[23](../qa/23-dto-mapping-layer.md)）

```text
MeController
  @PreAuthorize("hasRole('HOST')")
  @AuthenticationPrincipal OidcUser
  → ResponseEntity.ok(userService.currentHost(oidcUser))

UserService.currentHost
  requireByOidcSubject(sub) → User
  toMeResponse(local, oidcUser) → MeResponse
```

| フィールド | 出所 |
|------------|------|
| `id` | DB `Long` |
| `role` | DB（表示用）。認可の `ROLE_*` とは別 |
| `displayName` / `email` | OidcUser クレーム |

### 5.2 `@AuthenticationPrincipal`

```text
SecurityContextHolder の Authentication.principal を注入
  ≠ Redis を注釈が直接読む
  鍵 → Redis 復元 → SecurityContext → Resolver
```

認可は **`Authentication.getAuthorities()`**（mapper 結果）。  
`OidcUser.getAuthorities()` だけ見るとズレることがある（[06](../qa/06-me-controller.md)）。

---

## 6. CSRF（[05](../qa/05-security-config.md)）

stateful なので有効。状態変更（POST/PATCH/DELETE）はヘッダ必須。

```java
CookieCsrfTokenRepository.withHttpOnlyFalse()  // XSRF-TOKEN を JS 可読に
CsrfTokenRequestAttributeHandler
  setCsrfRequestAttributeName(null)            // SPA 向けヘッダ検証を素直に

.csrf(c → c
    .csrfTokenRepository(csrfRepo)
    .csrfTokenRequestHandler(csrfHandler)
)
```

| | SESSION | XSRF-TOKEN |
|--|---------|------------|
| 役割 | 誰か | 書き込み操作の許可 |
| HttpOnly | true | **false** |
| クライアント | 自動付与 | JS が読んで `X-XSRF-TOKEN` へ |

参加者 guest も CSRF 付き（permitAll でも POST は CSRF 対象）。

---

## 7. 401 / 403 と Filter 境界

```text
Filter chain（Security）
  → 未認証 → AuthenticationEntryPoint → writeJson 401
  → 権限不足 → AccessDeniedHandler → writeJson 403
DispatcherServlet
  → @PreAuthorize 失敗も 403 系
  → 業務例外 →（将来）@ControllerAdvice
```

| | Security EntryPoint | GlobalExceptionHandler（B-19・未） |
|--|---------------------|-------------------------------------|
| いつ | Controller **未到達** | Controller 到達後 |
| 例 | Cookie 無し `/api/me` | 404 NotFound、`@Valid` |

**Advice に 401 を一本化できない**（Filter で終わる）。形 `{ status, error, message }` だけ揃える。

`writeJson`: **`getWriter()` 前に UTF-8**（直書き経路の文字化け対策）。

openapi:

```json
{ "status": 401, "error": "Unauthorized", "message": "ログインが必要です" }
```

---

## 8. ログアウト（RP-initiated）

```text
POST /api/auth/logout + SESSION + X-XSRF-TOKEN
  → LogoutFilter: ローカル Session / Redis 破棄
  → OidcClientInitiatedLogoutSuccessHandler
       Keycloak end-session
       post_logout_redirect_uri = 設定値（規定 http://localhost:3000/）
  → ブラウザ 302
```

| 設定 | 展開先 |
|------|--------|
| `{baseUrl}/` | **backend** オリジン（Compose なら :8081）— UI 戻りには不向き |
| `app.logout.post-logout-redirect-uri` | フロント（実装済みの正） |

Keycloak client の Valid post logout URIs に `http://localhost:3000/*` が必要（realm JSON 済み）。

ローカル Session と IdP SSO Session は別物 → **両方落とす**のが RP-initiated。

---

## 9. 匿名参加（F-01 内の参加者側）

```text
POST /api/auth/guest（CSRF）
  → HttpSession に guestId
  → { guestId, voterKey: "g:{guestId}" }
```

| | HOST | 参加者 |
|--|------|--------|
| Keycloak | 必要 | 不要 |
| Session | OIDC Authentication 入り | guestId 属性 |
| 投票キー（将来） | `u:{userId}` 等 | `g:{guestId}` |

F-04/F-05 の土台。F-01 では「匿名 Session + CSRF が付く」までが範囲。

---

## 10. Keycloak / Compose / テスト / CI

### 10.1 Realm（[09](../qa/09-keycloak-realm-librashare.md)）

- Realm: **LibraShare** 相乗り  
- Client: `live-qa-app`（confidential）  
- Realm role: `HOST`  
- Ports（Compose）: Keycloak `8080` / backend `8081` / front `3000`  

### 10.2 設定ファイル

| ファイル | 役割 |
|----------|------|
| `SessionConfig` | Redis Session + Cookie 属性 |
| `SecurityConfig` | oauth2Login / CSRF / RBAC JSON / logout |
| `application.yml` | client / `app.logout.post-logout-redirect-uri` |
| `application-docker.yml` | コンテナ向け provider URI |

### 10.3 テスト（[25](../qa/25-userservice-unit-test.md)）

| クラス | 技法 | 検証 |
|--------|------|------|
| `UserServiceTest` | Mockito | JIT 分岐 / require / toMeResponse / currentHost |
| `MeControllerTest` | `@WebMvcTest` | HOST 200 / 非HOST 403 / 未認証 401 |

注意:

- OAuth2Client AutoConfig **exclude**（issuer 解決で Keycloak 不要）  
- 本番 `SecurityConfig` を無理に Import しない  
- `@EnableMethodSecurity` を足して `@PreAuthorize` を効かせる  
- `oidcLogin().authorities(ROLE_HOST)`  

CI: `.github/workflows/ci.yml` → `backend` で `mvn -B test`（F-09 接続）。

---

## 11. Member A / C への接点

### A（フロント）

| 項目 | 契約 |
|------|------|
| ログインボタン | `/oauth2/authorization/keycloak` |
| 認証確認 | `GET /api/me`（Cookie）。`id` は **number** |
| 状態変更 | `X-XSRF-TOKEN` |
| logout | `POST /api/auth/logout` → 着地 `:3000` |
| 401/403 | UI 表示。権限の正は API |
| 参加者 | `POST /api/auth/guest` → `voterKey` 保持 |

### C（WS / Compose）

| 項目 | 契約 |
|------|------|
| STOMP | ハンドシェイクで **同じ SESSION** |
| Compose | KC / Redis / api:8081 / web:3000 |
| CI | backend test job |

---

## 12. 学び・レビュー・次

### 刺さった技術論点

1. Redis 格納は自前メソッドではなく Spring Session Filter  
2. `{baseUrl}` ≠ フロント → post-logout 設定分離  
3. EntryPoint 直書きは UTF-8 明示  
4. mapper 三重読みは冗長／client role は将来  
5. `@AuthenticationPrincipal` ≠ Redis 直読  
6. Filter 401 は Advice に吸収できない  

### 次（B）

| 優先 | 内容 |
|------|------|
| 高 | Compose で受け入れチェックリスト消化 |
| 中 | mapper を IdToken のみへ簡素化 |
| 後 | F-02 イベント CRUD（`host_id` = ローカル PK） |
| 別Issue | B-19 Advice / B-13 CORS |

---

## 13. 理解チェック（F-01 網羅）

1. F-01 のエンドポイントを 5 つ挙げられるか？  
2. Login+Session と JWT RS の違いは？  
3. JIT が無いと何が壊れるか？  
4. `ROLE_HOST` と `MeResponse.role` の違いは？  
5. CSRF で SESSION と XSRF の役割分担は？  
6. 未ログイン 401 は誰が書くか？  
7. logout 後のブラウザ着地は誰が決めるか？  
8. guest の `voterKey` 形式は？  
9. Redis へ書く自前メソッドはどこにある／ないか？  

---

## 参考パス

```text
実装
  config/SecurityConfig.java
  config/SessionConfig.java
  service/UserService.java
  controller/MeController.java
  controller/GuestSessionController.java
  dto/MeResponse.java
  domain/User.java
  test/.../UserServiceTest.java
  test/.../MeControllerTest.java

docs
  scope.md / api/openapi-notes.md（認証節）
  qa/02,05,06,07,09,14,15,18,23,25,26
  share/（本資料）
```

---

## 話し方メモ

- 冒頭で **F-01 チェックリスト（§0）** を見せ、「今日はこの全部」と宣言  
- 技術の山は **§4（ログイン）→§5（me）→§7（401）→§8（logout）**  
- Redis「どのメソッド？」は必ず触る（自前ではない、が誤解されやすい）  
- A には Cookie/CSRF/me/guest、C には SESSION 共有とポートを明示  
- 時間が押したら §6 と §10 の細部を質疑に回す  
