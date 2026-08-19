# SecurityConfig 周りのデータの流れ（言語化）

## 質問

`SecurityConfig` でデータがどこから来て、何の形になり、どこへ行くか。

---

## なぜこの流れがあるか

ブラウザは毎回バラバラの HTTP を送る。Keycloak（IdP）は「誰か」を証明するが、API のたびに JWT を検証し直さない設計なので、**ログイン時に一度だけ材料を受け取り、セッションに載せて以降は復元する**必要がある。

---

## 全体（一文）

> Keycloak が証明した「誰か・ロール」を、backend が **交換→検証→変換→格納→発行** し、以降の API は Cookie で **復元** して認可する。

---

## シーン① ログイン（初回）

```text
① ブラウザ
   GET /oauth2/authorization/keycloak
   → 形: ただの HTTP リクエスト（まだ SESSION が無くてもよい）

② Keycloak（IdP）
   ログイン UI → 成功すると「認可コード」（短い文字列）を backend に返す
   → 操作: 発行（コード）

③ backend（oauth2Login・フレームワーク）
   認可コードを Keycloak に渡し、トークンに取り換える
   → 操作: 交換
   → 形: OidcUserRequest
        （ClientRegistration + IDトークン + アクセストークン等）

④ oidcUserService（SecurityConfig）
   受け取り: OidcUserRequest
   処理:
     delegate.loadUser → IDトークンを検証し OidcUser を組み立てる
     provisionHost(sub) → DB users に HOST 行が無ければ INSERT（JIT）
   返し: OidcUser（principal になる）
   ※ ここでは ROLE_* を足さない（次の⑤が別処理）
   → 形:
        OidcUser … sub / name / email / クレーム
        String sub … IDトークンの subject
        User（Entity）… DB 側。セッションには載せない（副作用のみ）

⑤ grantedAuthoritiesMapper（SecurityConfig）※ loadUser の外・別メソッド
   誰が呼ぶか: OidcAuthorizationCodeAuthenticationProvider
     （loadUser の直後に mapAuthorities を呼ぶ。delegate の中ではない）
   受け取り: Collection<GrantedAuthority>
     中に OidcUserAuthority（IdToken / UserInfo / attributes を保持）
   処理: realm_access.roles の "HOST" などを ROLE_HOST に変換
   返し: Set<GrantedAuthority>（元の SCOPE_* + ROLE_*）
   → 操作: 変換
   → 形:
        Map realm_access … { "roles": ["HOST", ...] }  ← 外側は Map
        List roles … ["HOST", ...]                     ← 内側が List
        GrantedAuthority … "ROLE_HOST"

⑥ フレームワーク
   Authentication を組み立てる
     principal = OidcUser
     authorities = mapper が返した Set（← Set 自体が SecurityContext ではない）
   → SecurityContext に Authentication を載せる
   → HttpSession 経由で Redis に書く
   → 操作: 格納
   → 形: SecurityContext（認証状態の箱）⊃ Authentication ⊃ authorities

⑦ ブラウザへ
   Set-Cookie: SESSION=…（箱の鍵）
   Set-Cookie: XSRF-TOKEN=…（CSRF 用）
   → 操作: 発行
```

| 段階 | 操作語 | データの形 |
|------|--------|------------|
| IdP → RP | 交換 | 認可コード → IDトークン等 |
| loadUser | 検証・組み立て | トークン → `OidcUser` |
| JIT | 格納（DB） | `sub` → `users` 行 |
| mapper | 変換 | `"HOST"` → `ROLE_HOST` |
| セッション | 格納 | Authentication → Redis |
| Cookie | 発行 | セッション ID 文字列 |

---

## シーン② ログイン後の API（例: GET /api/me）

```text
① ブラウザ
   Cookie: SESSION=… を付けて GET /api/me
   → 形: HTTP + 鍵だけ（IDトークンは送らない）

② Spring Session
   SESSION で Redis の箱を開く
   → 操作: 復元
   → 形: 再び SecurityContext（OidcUser + ROLE_HOST）

③ フィルタ（authorizeHttpRequests）
   authenticated() を検証 → 未ログインなら EntryPoint（401 JSON）

④ メソッド認可（@PreAuthorize）
   hasRole('HOST') を検証 → authorities に ROLE_HOST が無ければ 403
   → 見る場所: Authentication.getAuthorities()（DB の role ではない）

⑤ MeController
   受け取り: @AuthenticationPrincipal OidcUser
   → userService.currentHost(oidcUser)

⑥ UserService
   requireByOidcSubject(sub) → DB から User を復元
   toMeResponse(local, oidcUser)
     id/role ← DB
     displayName/email ← OidcUser クレーム
   → 形: MeResponse（record）

⑦ Controller
   ResponseEntity.ok(MeResponse) → JSON 200
```

| 知りたいこと | どのデータから復元するか |
|--------------|--------------------------|
| ログイン済みか | SESSION → SecurityContext |
| HOST か（認可） | authorities の `ROLE_HOST` |
| ローカル PK | DB `users.id`（JIT 済み前提） |
| 表示名 / email | セッション上の OidcUser（IdP クレーム） |

---

## シーン③ 未ログイン / 権限不足（Controller 未到達または PreAuthorize 失敗）

```text
未認証で /api/me
  → AuthenticationEntryPoint
  → writeJson → { status:401, error:"Unauthorized", message:"ログインが必要です" }

ログイン済みだが ROLE_HOST なし
  → AccessDeniedHandler
  → writeJson → { status:403, error:"Forbidden", message:"権限がありません" }
```

- **受け取り:** `HttpServletResponse` + 例外（理由）
- **処理:** UTF-8 明示のうえ JSON を直接書く（GlobalExceptionHandler ではない）
- **返し:** 共通エラー形 `{ status, error, message }`

---

## シーン④ ログアウト

```text
① ブラウザ
   POST /api/auth/logout
   + Cookie SESSION
   + ヘッダ X-XSRF-TOKEN（CSRF 検証）

② LogoutFilter
   ローカルセッション（Redis）を破棄
   → SecurityContext も消える

③ OidcClientInitiatedLogoutSuccessHandler
   ClientRegistrationRepository から Keycloak の end-session URL を組み立て
   postLogoutRedirectUri（規定: http://localhost:3000/）を付ける
   → ブラウザを Keycloak → フロントへリダイレクト（302）
```

| 変数 | 形 | 出所 |
|------|-----|------|
| `clientRegistrationRepository` | Bean | yml の oauth2 client |
| `postLogoutRedirectUri` | String | `app.logout.post-logout-redirect-uri` |

---

## 起動時だけ流れる依存（リクエストデータではない）

| 依存 | 形 | 出所 |
|------|-----|------|
| `UserService` | 具象 `@Service` | コンストラクタ注入 |
| `ObjectMapper` | Jackson Bean | Boot 自動設定 |
| `ClientRegistrationRepository` | oauth2 client 登録の倉庫 | `spring.security.oauth2.client.*` |
| `postLogoutRedirectUri` | String | yml / 環境変数 |

---

## 型の正体（この流れで出てくるもの）

| 名前 | 正体 |
|------|------|
| `OidcUserRequest` | トークン交換直後の入力オブジェクト |
| `OidcUser` | principal 用インタフェース（クレーム取得） |
| `OidcUserAuthority` | authorities の一種。IdToken/UserInfo を内包 |
| `GrantedAuthority` | 権限の契約。実装例 `SimpleGrantedAuthority` |
| `Authentication` | 「誰か＋権限」の契約 |
| `SecurityContext` | 今の認証状態の入れ物 |
| `MeResponse` | API 応答用 record（不変） |

---

## 【EJ】

- **依存性注入** … SecurityConfig はコンストラクタで UserService / Repository / 設定値を受け取る  
- **公開面最小化** … DB `User` を API に直接出さず `MeResponse` に変換  
- **あえて外す（不変性）** … Entity `User` は JPA で可変。応答だけ record  
- **単一の正** … 認可用 ROLE は IdP→authorities。応答の `role` は DB（混ぜない）

---

## 各メソッドの流れ（何が・どこで・どうなるか）

呼び出し関係の鳥瞰:

```text
起動時
  コンストラクタ（依存をフィールドへ）
  securityFilterChain  … ルール登録。他メソッドを「差し込む」だけ
       ├─ oidcUserService()           ← ログイン時に Spring が呼ぶ
       ├─ grantedAuthoritiesMapper()  ← ログイン時に Spring が呼ぶ
       │     └─ addRolesFromRealmAccess
       ├─ authenticationEntryPoint()  ← 未認証時
       │     └─ writeJson
       ├─ accessDeniedHandler()       ← 権限不足時
       │     └─ writeJson
       └─ oidcLogoutSuccessHandler()  ← logout 成功後
```

**重要:** `securityFilterChain` はリクエストごとに業務ロジックを実行しない。  
「いつ・誰が・どの Bean／ラムダを使うか」を **登録する設定メソッド**。実処理はログイン／401／logout のタイミングでフレームワークが呼ぶ。

### コンストラクタ

| | |
|--|--|
| **いつ** | アプリ起動（DI） |
| **受け取り** | `ObjectMapper` / `UserService` / `ClientRegistrationRepository` / `postLogoutRedirectUri` |
| **処理** | フィールドに保持するだけ |
| **返し** | なし（インスタンス生成） |

リクエストのデータはここでは流れない。

### `securityFilterChain(HttpSecurity http)`

| | |
|--|--|
| **いつ** | 起動時に1回（`@Bean`） |
| **受け取り** | `HttpSecurity`（設定ビルダー） |
| **処理** | URL 認可・oauth2Login・CSRF・例外・logout のルールを組み立て |
| **返し** | `SecurityFilterChain`（フィルタの並び） |

中で呼んでいる `oidcUserService()` 等は **今すぐログイン処理するのではなく、後で使う実装をチェーンに登録**している。

ローカル変数:

- `csrfRepo` … CSRF トークンを Cookie `XSRF-TOKEN` に載せる倉庫  
- `csrfHandler` … ヘッダ名の扱い（`setCsrfRequestAttributeName(null)` は SPA 向け）

### `oidcUserService()`

| | |
|--|--|
| **いつ** | Keycloak ログイン成功・トークン交換の直後 |
| **誰が呼ぶ** | oauth2Login の userInfoEndpoint |
| **受け取り** | `OidcUserRequest`（registration + トークン類） |
| **処理** | ① `delegate.loadUser` で `OidcUser` を作る ② `provisionHost(sub)` で DB JIT |
| **返し** | `OidcUser`（このあと principal になる） |

```text
userRequest
  → delegate.loadUser → oidcUser
  → userService.provisionHost(oidcUser.getSubject())  // 戻り User は捨てる（副作用）
  → return oidcUser
```

**【EJ】** 標準実装へ委譲（検証は Spring）／自前は JIT の公開面だけ。

### `grantedAuthoritiesMapper()`

| | |
|--|--|
| **いつ** | 同上、ログイン成功直後（`OidcUser` 組み立てと連携） |
| **誰が呼ぶ** | `userAuthoritiesMapper(...)` に差し込んだため Spring が呼ぶ（`@Bean` だけでは不足） |
| **受け取り** | `authorities`（`OidcUserAuthority` や `SCOPE_*` など） |
| **処理** | `OidcUserAuthority` から `realm_access` を取り、`addRolesFromRealmAccess` で `ROLE_*` を足す |
| **返し** | `Set<GrantedAuthority>` → `Authentication.getAuthorities()` に載る |

IdToken / UserInfo / attributes の三重読みは保険（冗長になりうる）。

### `addRolesFromRealmAccess(realmAccess, dest)`

| | |
|--|--|
| **いつ** | mapper のループ内 |
| **受け取り** | `Map`（`realm_access`）と書き込み先 `Set` |
| **処理** | `roles` リストの各文字列に `ROLE_` を付けて `dest` へ add |
| **返し** | void（`dest` を副作用で更新） |

```text
{ "roles": ["HOST", ...] }  →  dest に ROLE_HOST
```

null や型違いなら何もしない（安全側）。

### `authenticationEntryPoint()` / `accessDeniedHandler()`

| | EntryPoint | AccessDenied |
|--|------------|--------------|
| **いつ** | 要認証なのに未ログイン | ログイン済みだが権限不足等 |
| **受け取り** | request, response, 例外 | 同左 |
| **処理** | `writeJson` に委譲 | 同左 |
| **返し** | void（response に書き終わり） | 同左 |
| **HTTP** | 401 | 403 |

Controller や `@RestControllerAdvice` には届かない経路。

### `writeJson(...)`

| | |
|--|--|
| **いつ** | EntryPoint / AccessDenied から |
| **受け取り** | `HttpServletResponse` + status/error/message |
| **処理** | UTF-8 設定 → Content-Type → `objectMapper` で Map を Writer へ |
| **返し** | void |

出る JSON: `{ status, error, message }`。

### `oidcLogoutSuccessHandler()`

| | |
|--|--|
| **いつ** | `POST /api/auth/logout` でローカルセッション破棄のあと |
| **受け取り** | （handler 生成時）`ClientRegistrationRepository` + 戻り先 URI |
| **処理** | Keycloak end-session URL を組み立て、ブラウザをリダイレクト |
| **返し** | `LogoutSuccessHandler` 実装 |

フィールド `postLogoutRedirectUri` がフロント（規定 `:3000`）へ戻す指定。

---

## データ型カタログ（詳しく）

読み方: **正体** = interface / 具象 / 関数型インタフェース / record / Entity。  
**ジェネリクス**は「何を扱う契約か」を表す。

### A. コンストラクタ／設定まわり

| 型 | 正体 | 役割 |
|----|------|------|
| `ObjectMapper` | 具象（Jackson） | Java オブジェクト ↔ JSON |
| `UserService` | 具象 `@Service` | JIT / me 組み立て |
| `ClientRegistrationRepository` | **interface** | client 登録（keycloak 等）の倉庫 |
| `String` (`postLogoutRedirectUri`) | プリミティブ包装ではない通常の文字列 | ログアウト後 URL |
| `HttpSecurity` | 具象ビルダー | Security ルール組み立て |
| `SecurityFilterChain` | **interface** | 完成したフィルタ列の契約。`http.build()` の戻り |
| `CookieCsrfTokenRepository` | 具象 | CSRF トークンを Cookie に載せる |
| `CsrfTokenRequestAttributeHandler` | 具象 | CSRF をリクエスト属性／ヘッダでどう読むか |

### B. ログイン（OIDC）まわり

| 型 | 正体 | 中に入っているもの（イメージ） |
|----|------|--------------------------------|
| `OAuth2UserService<OidcUserRequest, OidcUser>` | **interface**（関数的） | 「Request を User にロードする」契約。`<入力, 出力>` |
| `OidcUserService` | 具象（Spring） | 上記の標準実装（`delegate`） |
| `OidcUserRequest` | 具象 | `ClientRegistration` + access token + **`OidcIdToken`** など |
| `OidcUser` | **interface** | principal。`getSubject()`→`String`、`getEmail()`→`String`、`getFullName()`→`String` |
| `OidcIdToken` | 具象（JWT 表現） | 検証済み IDトークン。クレーム取得 API あり |
| `OidcUserInfo` | 具象 | UserInfo エンドポイントの属性。null のことも |
| `String` (`sub`) | | Keycloak ユーザーの一意 ID。DB `oidc_subject` と対応 |

```text
OAuth2UserService<OidcUserRequest, OidcUser>
                 └入力型┘          └出力型┘
```

### C. 権限（mapper）まわり

| 型 | 正体 | 例・中身 |
|----|------|----------|
| `GrantedAuthoritiesMapper` | **interface**（`Collection`→`Collection`） | ロール読み替えの契約 |
| `Collection<? extends GrantedAuthority>` | 入力の権限袋 | `OidcUserAuthority`, `SCOPE_openid`, … |
| `GrantedAuthority` | **interface** | 「権限1つ」。`getAuthority()` → `"ROLE_HOST"` 等の String |
| `OidcUserAuthority` | 具象（`GrantedAuthority` の一種） | IdToken / UserInfo / attributes を**内包**する特別な権限オブジェクト |
| `SimpleGrantedAuthority` | 具象 | 文字列1本だけの権限。mapper が `new SimpleGrantedAuthority("ROLE_HOST")` |
| `Set<GrantedAuthority>` | 集合 | 重複なしで ROLE を溜める出力 |
| `Map<String, Object>` (`realm_access`) | クレームの JSON オブジェクト相当 | `{ "roles": ["HOST", "offline_access"] }` |
| `List<?>` (`roles`) | ロール名の配列相当 | 要素は実行時は多くが `String`。だから `instanceof String` で絞る |
| `Map<String, Object>` (`attributes`) | OIDC 属性の袋 | key=`"realm_access"` の value が生の `Object`（多くの場合 Map） |
| `Object` / `Map<?, ?>` | 生・ワイルドカード | JSON 由来でコンパイル時型が弱い。パターンマッチで絞り込む |

`getClaimAsMap("realm_access")` の戻り:

- 宣言上: `Map<String, Object>`（無いときは `null`）
- `"roles"` の value: 実行時は `List`（要素は `String` 想定）

なぜ `List<?>` や `Map<?, ?>` か:

> JWT／JSON は静的型が無い。Jackson/Spring は「Map と List」に落とすので、**アプリ側で instanceof してから使う**。

### D. 認証状態（セッションに載るもの）

| 型 | 正体 | 関係 |
|----|------|------|
| `Authentication` | **interface** | ログイン結果全体 |
| `SecurityContext` | **interface** | 今の `Authentication` を持つ箱 |
| principal | 多くの場合 `OidcUser` | `@AuthenticationPrincipal` で受け取る型 |
| authorities | `Collection<GrantedAuthority>` | `@PreAuthorize` / `hasRole` が見る側 |

```text
SecurityContext
  └── Authentication
        ├── principal: OidcUser
        └── authorities: [ ROLE_HOST, SCOPE_openid, ... ]
```

### E. DB／API 応答（SecurityConfig の外だが接続点）

| 型 | 正体 | フィールド |
|----|------|------------|
| `User` | JPA **Entity**（可変） | `Long id`, `String oidcSubject`, `String role`, `Instant createdAt` |
| `Optional<User>` | リポジトリ検索の有無 | JIT / require |
| `MeResponse` | **record**（不変） | `Long id`, `String displayName`, `String email`, `String role` |
| `ResponseEntity<MeResponse>` | 具象ラッパ | HTTP ステータス + ボディ |

注意: `User.role`（DB の `"HOST"`）と `GrantedAuthority` の `"ROLE_HOST"` は **別型・別用途**。

### F. エラー応答／logout

| 型 | 正体 | 中身 |
|----|------|------|
| `AuthenticationEntryPoint` | **interface**（関数的） | `(request, response, AuthenticationException) -> void` |
| `AccessDeniedHandler` | **interface**（関数的） | `(request, response, AccessDeniedException) -> void` |
| `AuthenticationException` | 例外階層 | 未認証の理由 |
| `AccessDeniedException` | 例外 | 権限不足の理由 |
| `HttpServletResponse` | Servlet API | ステータス・ヘッダ・Writer |
| `HttpStatus` | enum | `UNAUTHORIZED`(401), `FORBIDDEN`(403) |
| `Map<String, ?>`（`Map.of`） | 不変 Map | JSON の元。key は String、value は Integer/String |
| `LogoutSuccessHandler` | **interface** | logout 後の振る舞い |
| `OidcClientInitiatedLogoutSuccessHandler` | 具象 | Keycloak end-session へ 302 |

`writeJson` が出す論理形（型というより JSON schema）:

```text
Map {
  "status"  → Integer (例 401)
  "error"   → String  (例 "Unauthorized")
  "message" → String  (例 "ログインが必要です")
}
```

### G. メソッド署名で見る入出力（早見）

```text
oidcUserService:
  OidcUserRequest  →  OidcUser

grantedAuthoritiesMapper:
  Collection<? extends GrantedAuthority>  →  Collection<? extends GrantedAuthority>
  （実装では Set<GrantedAuthority>）

addRolesFromRealmAccess:
  Map<String,Object> + Set<GrantedAuthority>  →  void（dest を更新）

authenticationEntryPoint / accessDeniedHandler:
  HttpServletRequest + HttpServletResponse + Exception  →  void

writeJson:
  HttpServletResponse + HttpStatus + String + String  →  void

oidcLogoutSuccessHandler:
  （生成時）ClientRegistrationRepository, String URI  →  LogoutSuccessHandler
```

### H. 【EJ】型まわり

- **ジェネリクス** … `OAuth2UserService<OidcUserRequest, OidcUser>` で入出力を明示  
- **interface と具象** … 契約（`OidcUser`, `GrantedAuthority`）と実装（`SimpleGrantedAuthority`, `OidcUserService`）を分ける  
- **不変性** … `MeResponse` は record。`Map.of` も不変  
- **あえて外す** … Entity `User` とクレーム `Map` は可変／動的。JSON 由来は静的型が弱い  
- **公開面最小化** … DB Entity を API に出さず DTO に変換  

---

## 流れ（このメソッドだけ）— 全メソッド詳細版

mapper で使ったのと同じ粒度。入力の正体 → ステップ → 出力。

### コンストラクタ

```text
Spring が SecurityConfig を new するとき

入力（DI）:
  ObjectMapper
  UserService
  ClientRegistrationRepository
  String postLogoutRedirectUri（@Value）

処理:
  → 各引数を final フィールドに代入するだけ

出力:
  → 完成した SecurityConfig インスタンス（コンテナに登録）

やらないこと:
  → JWT を読まない / Redis を見ない / フィルタを動かさない
```

---

### `securityFilterChain`

```text
起動時、@Bean として1回

入力:
  HttpSecurity http  … 設定を積み上げるビルダー（まだ「動くフィルタ列」ではない）

処理:
  ① CSRF 部品を用意
       CookieCsrfTokenRepository csrfRepo
       CsrfTokenRequestAttributeHandler csrfHandler
       csrfHandler.setCsrfRequestAttributeName(null)

  ② http.authorizeHttpRequests(...)
       /oauth2/**, /login/oauth2/** → permitAll
       /api/auth/guest → permitAll
       /api/me, /api/auth/logout → authenticated
       それ以外 → permitAll（今の設定）

  ③ http.oauth2Login(...)
       userInfoEndpoint に
         oidcUserService() の戻り  … ログイン時用コールバックを登録
         grantedAuthoritiesMapper() の戻り … 同様に登録
       ※ ここでは「登録」だけ。ログイン処理は走らない

  ④ http.csrf(...)
       repo / handler をセット

  ⑤ http.exceptionHandling(...)
       authenticationEntryPoint() / accessDeniedHandler() を登録

  ⑥ http.logout(...)
       logoutUrl = /api/auth/logout
       logoutSuccessHandler = oidcLogoutSuccessHandler() の戻りを登録

  ⑦ return http.build()

出力:
  SecurityFilterChain  … 実際にリクエストを通すフィルタの並び

やらないこと:
  → 今この瞬間に OidcUser を作らない / 401 を書かない
```

---

### `oidcUserService`（メソッド本体＋返すラムダ）

```text
【メソッド本体・起動〜登録時】
  ① new OidcUserService() を delegate として用意
  ② return userRequest -> { ... }
       → 型は OAuth2UserService<OidcUserRequest, OidcUser>
       → このラムダが「あとで Spring が呼ぶ実装」

【ラムダ本体・ログイン成功・トークン交換の直後】

入力:
  OidcUserRequest userRequest
    例の中身イメージ:
      ClientRegistration（keycloak）
      OAuth2AccessToken
      OidcIdToken（検証前〜検証材料）
    ※ 生の JWT 文字列を自分でパースする段階ではない

処理:
  ① OidcUser oidcUser = delegate.loadUser(userRequest)
       Spring 標準が:
         IDトークン検証
         クレーム読み取り（sub, email, name 等）
         必要なら UserInfo 取得
         → OidcUser を組み立て
       ※ realm_access は Keycloak 既定だと Access Token 側。
         ID Token に載せるには mapper の id.token.claim=true が必要

  ② userService.provisionHost(oidcUser.getSubject())
       入力: String sub
       DB: findByOidcSubject → 無ければ User.hostOf(sub) を save
       戻り: User Entity
       ※ この戻り値は変数に残さず捨てる（JIT の副作用が目的）
       ※ タイミング: SecurityContext 格納より前（loadUser の中）

  ③ return oidcUser

出力:
  OidcUser
    → このあと Authentication の principal になる
    → セッション（Redis）に載る材料の一部

やらないこと:
  → ROLE_HOST を足さない（それは mapper。しかも loadUser の外）
  → ResponseEntity や JSON を返さない
```

---

### `grantedAuthoritiesMapper`（メソッド本体＋返すラムダ）

```text
【メソッド本体】
  return authorities -> { ... }
    → 型は GrantedAuthoritiesMapper
    → SecurityConfig 上は別メソッド（oidcUserService とは別）

【いつ呼ばれるか】
  OidcAuthorizationCodeAuthenticationProvider.authenticate 内:
    1) userService.loadUser(...)     ← provisionHost までここで終わる
    2) authoritiesMapper.mapAuthorities(oidcUser.getAuthorities())  ← ここ
    3) Authentication 組み立て → SecurityContext
  ※ delegate.loadUser の内部ではない

【ラムダ本体】

入力:
  Collection<? extends GrantedAuthority> authorities
    例: [ OidcUserAuthority(中に IdToken あり), SCOPE_openid, SCOPE_profile, ... ]
    ※ JWT 生バイトではない
    ※ 「権限オブジェクトの袋」がすでにできている

処理:
  ① Set<GrantedAuthority> mapped = new HashSet<>(authorities)
       元の SCOPE_* 等を残しつつ、あとから ROLE_* を足せるコピー

  ② for (GrantedAuthority authority : authorities)
       if (authority instanceof OidcUserAuthority oidc)

         (a) oidc.getIdToken().getClaimAsMap("realm_access")
               → Map<String,Object> | null
               → addRolesFromRealmAccess(..., mapped)
               ※ live-qa の現行方針は **IdToken 一本化**
               ※ UserInfo / attributes fallback は使わない

  ③ return mapped

出力:
  Set<GrantedAuthority>
    例: [ OidcUserAuthority, SCOPE_openid, ..., ROLE_HOST, ... ]
    → Authentication.getAuthorities() に載る（Set ≠ SecurityContext）
    → SecurityContext ⊃ Authentication ⊃ この authorities
    → @PreAuthorize("hasRole('HOST')") が見る材料

注意:
  IdToken に realm_access が無いと mapper は ROLE_HOST を足せない
  → Keycloak の roles scope / realm roles mapper で id.token.claim=true が前提
```

---

### `addRolesFromRealmAccess`

```text
mapper のループから呼ばれる（このメソッドだけの世界）

入力:
  Map<String, Object> realmAccess
    例: { "roles": ["HOST", "offline_access", ...] }
    null のこともある
  Set<GrantedAuthority> dest
    mapper 側の mapped（ここに足していく）

処理:
  ① realmAccess == null → return（何もしない）

  ② Object rolesObj = realmAccess.get("roles")
       List でなければ return

  ③ for (Object role : roles)
       role が String name なら
         dest.add(new SimpleGrantedAuthority("ROLE_" + name))
         例: "HOST" → SimpleGrantedAuthority("ROLE_HOST")

出力:
  void
  ※ dest が副作用で増える（戻り値で新しい Set を返さない）

やらないこと:
  → DB を見ない / OidcUser を作らない
```

---

### `authenticationEntryPoint`（メソッド本体＋返すラムダ）

```text
【メソッド本体・登録時】
  return (request, response, authException) -> writeJson(...)
    → 型は AuthenticationEntryPoint

【ラムダ本体・要認証 URL なのに未ログインのとき】

入力:
  HttpServletRequest request     … 今のリクエスト（中身はほぼ未使用）
  HttpServletResponse response   … ここに書く
  AuthenticationException authException … 理由（今はメッセージに未使用）

処理:
  → writeJson(
        response,
        HttpStatus.UNAUTHORIZED,   // 401
        "Unauthorized",
        "ログインが必要です"
     )

出力:
  void（response に JSON が書き終わり、フィルタがそこで止める）

やらないこと:
  → Controller / @RestControllerAdvice には届かない
  → Redis や DB は見ない
```

---

### `accessDeniedHandler`（メソッド本体＋返すラムダ）

```text
【メソッド本体・登録時】
  return (request, response, accessDeniedException) -> writeJson(...)
    → 型は AccessDeniedHandler

【ラムダ本体・認証済みだが権限不足などのとき】

入力:
  HttpServletRequest request
  HttpServletResponse response
  AccessDeniedException accessDeniedException  … 今はメッセージに未使用

処理:
  → writeJson(
        response,
        HttpStatus.FORBIDDEN,   // 403
        "Forbidden",
        "権限がありません"
     )

出力:
  void（403 JSON で終わり）

EntryPoint との違い:
  401 = 「誰か分かっていない」
  403 = 「誰かは分かっているが足りない」（典型）
```

---

### `writeJson`

```text
EntryPoint / AccessDenied のラムダから呼ばれる

入力:
  HttpServletResponse response
  HttpStatus status     … UNAUTHORIZED or FORBIDDEN
  String error          … "Unauthorized" / "Forbidden"
  String message        … 日本語メッセージ

処理:
  ① response.setStatus(status.value())
       例: 401

  ② response.setCharacterEncoding("UTF-8")
       ※ getWriter() より前。無いと日本語が化けることがある

  ③ response.setContentType("application/json")

  ④ objectMapper.writeValue(
         response.getWriter(),
         Map.of(
           "status", status.value(),   // Integer
           "error", error,             // String
           "message", message          // String
         )
     )

出力:
  void
  ワイヤ上の JSON 例:
    { "status": 401, "error": "Unauthorized", "message": "ログインが必要です" }

やらないこと:
  → MeResponse は作らない / redirect しない
```

---

### `oidcLogoutSuccessHandler`

```text
【メソッド本体・登録時（securityFilterChain から呼ばれる）】

入力（フィールド経由）:
  ClientRegistrationRepository clientRegistrationRepository
  String postLogoutRedirectUri
    例: "http://localhost:3000/"

処理:
  ① new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)
  ② handler.setPostLogoutRedirectUri(postLogoutRedirectUri)
  ③ return handler
       型としては LogoutSuccessHandler

出力（登録時）:
  組み立て済みハンドラオブジェクト（まだリダイレクトしない）

【ハンドラ実行時・POST /api/auth/logout 成功後】
※ この実行は Spring の LogoutFilter 側。ローカル SESSION 破棄のあと

処理のイメージ:
  ① ClientRegistration から Keycloak の end-session URL を組み立て
  ② 必要なら IDトークン等をクエリに載せる
  ③ post_logout_redirect_uri = postLogoutRedirectUri
  ④ ブラウザを 302 で Keycloak → 最終的にフロントへ

やらないこと（メソッド本体）:
  → 登録時点では Redis を消さない（消すのは LogoutFilter）
```

---

### メソッド間の順番（ログイン成功時だけ）

```text
① oidcUserService ラムダ
     OidcUserRequest → OidcUser ＋ JIT

② grantedAuthoritiesMapper ラムダ
     authorities → ROLE_* 入り Set
     └─ 何度も addRolesFromRealmAccess

③ フレームワーク
     Authentication(principal=OidcUser, authorities=...) を組む
     → SecurityContext → セッション(Redis) 格納
     → SESSION / XSRF-TOKEN Cookie 発行
```

401/403（EntryPoint / AccessDenied → writeJson）と logout（handler）は、この列とは別タイミング。

---

## 各メソッドの書き方（パターン解説）

`SecurityConfig` にはだいたい **3種類の書き方**しか出ない。

| パターン | 形 | 使うメソッド |
|----------|-----|----------------|
| **A. ビルダー設定** | `http.xxx(y -> y....)` | `securityFilterChain` |
| **B. 関数型を返す工場** | `return (引数) -> { ... }` | mapper / oidcUserService / EntryPoint / AccessDenied |
| **C. 普通の手続きメソッド** | 引数を受けて void / 値を返す | コンストラクタ / `addRolesFromRealmAccess` / `writeJson` / logout handler 組み立て |

Security は「あとでフレームワークが呼ぶコールバック」が多いので、**B が多くなる**のが普通。

### コンストラクタ — 依存の受け皿

```java
public SecurityConfig(ObjectMapper objectMapper, ..., @Value("...") String uri) {
    this.objectMapper = objectMapper;
    ...
}
```

- **書き方:** 引数で受けて `this.xxx = xxx`  
- **なぜ:** `@Autowired` フィールド注入より、必須依存がコンストラクタで一目で分かる（【EJ】コンストラクタ注入）  
- `@Value` もコンストラクタ引数に書ける → 設定値も依存として扱う  

### `securityFilterChain` — メソッドチェーン + ラムダ

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // 1) 部品を先に用意
    CookieCsrfTokenRepository csrfRepo = ...;
    CsrfTokenRequestAttributeHandler csrfHandler = ...;

    // 2) http に「設定のかたまり」をラムダで渡す
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(...).permitAll()
            ...
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(oidcUserService())  // 別メソッドの戻りを差し込む
                ...
            )
        )
        ...;
    // 3) 完成品を返す
    return http.build();
}
```

読み方のコツ:

```text
.authorizeHttpRequests(auth -> auth.xxxx())
                 ↑カスタマイザ引数   ↑同じ auth に命令を続けて返す
```

- `auth -> auth...` は「渡されたビルダーを設定して返す」短絡記法  
- **今ログイン処理はしない**。`oidcUserService()` を呼んでいるのは「実装オブジェクトを作って登録」するため  
- `@Bean` + 戻り値 `SecurityFilterChain` = Spring に「このフィルタ列を使え」と渡す定番  

### `grantedAuthoritiesMapper` / `oidcUserService` — 「インタフェース実装をラムダで返す」

```java
@Bean
public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
    return authorities -> {   // ← これが GrantedAuthoritiesMapper の実装本体
        ...
        return mapped;
    };
}
```

```java
@Bean
public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
    OidcUserService delegate = new OidcUserService();
    return userRequest -> {   // ← OAuth2UserService の loadUser 相当
        OidcUser oidcUser = delegate.loadUser(userRequest);
        userService.provisionHost(oidcUser.getSubject());
        return oidcUser;
    };
}
```

| 要素 | 意味 |
|------|------|
| メソッドの戻り型 | 契約（interface） |
| `return 引数 -> { ... }` | その契約の **実装をその場で作って返す** |
| 中の処理 | あとで Spring がログイン時に実行する本体 |

匿名クラスで書くなら:

```java
return new GrantedAuthoritiesMapper() {
    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(...) { ... }
};
```

ラムダはこれの短縮形。**「メソッドがインタフェースを new して返す工場」**と覚える。

`oidcUserService` 特有:

1. 標準実装を `delegate` として保持（クロージャでラムダが捕獲）  
2. ラムダ内で委譲 → 自分の処理（JIT）→ 返す  

### `authenticationEntryPoint` / `accessDeniedHandler` — 1行ラムダ工場

```java
private AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException)
        -> writeJson(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "ログインが必要です");
}
```

- 戻り型が関数型インタフェース（メソッドが1つ）なので、**引数3つ → writeJson 呼び出し**だけで実装になる  
- `private` … 外の DI には出さず、`securityFilterChain` からだけ使う  
- 共通化は `writeJson` に寄せる（コピペで 401/403 を増やさない）  

未使用の `authException` は「契約上渡ってくるが、今はメッセージに使わない」。

### `addRolesFromRealmAccess` / `writeJson` — 普通の private ヘルパ

```java
private void addRolesFromRealmAccess(Map<String, Object> realmAccess, Set<GrantedAuthority> dest) {
    if (realmAccess == null) return;
    ...
    dest.add(...);  // 引数 dest を書き換える（出力パラメータ）
}
```

- **ガード節**（null / 型違いなら early return）  
- `instanceof` パターンマッチで JSON 由来の弱い型を狭める  
- `void` + コレクション更新 … 呼び出し側の `Set` に足していく書き方  

```java
private void writeJson(...) throws IOException {
    response.setStatus(...);
    response.setCharacterEncoding(...);  // getWriter より前
    response.setContentType(...);
    objectMapper.writeValue(response.getWriter(), Map.of(...));
}
```

- Servlet の「ステータス → エンコーディング → Content-Type → ボディ」の順番を守る手続きコード  
- `throws IOException` … 書く系 I/O。呼び出し元の関数型インタフェースも checked 例外を許容する定義になっている  

### `oidcLogoutSuccessHandler` — 具象を new して設定して返す

```java
private LogoutSuccessHandler oidcLogoutSuccessHandler() {
    final OidcClientInitiatedLogoutSuccessHandler handler =
        new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    handler.setPostLogoutRedirectUri(postLogoutRedirectUri);
    return handler;
}
```

- ラムダではなく **既存クラスを組み立てる工場**  
- フィールドの Repository / URI をコンストラクタ・setter で渡す  
- 戻りは親インタフェース型（`LogoutSuccessHandler`）にして、チェーン側は契約だけ知る  

### クラス先頭のアノテーション

| 書き方 | 意味 |
|--------|------|
| `@Configuration` | このクラスは設定用。中の `@Bean` を容器に登録 |
| `@EnableWebSecurity` | Web Security を有効化 |
| `@EnableMethodSecurity` | `@PreAuthorize` を有効化 |
| `@Bean` | このメソッドの戻りを Spring Bean にする |

`grantedAuthoritiesMapper` / `oidcUserService` は `@Bean` だが、**oauth2Login の DSL に渡して初めてログイン経路で使われる**（Bean だけの置きっぱなしでは mapper が効かない、がこのプロジェクトの注意点）。

### 書き方チートシート

```text
設定を積み上げる     → http.xxx(x -> x.yyy())
あとで呼ばれる処理   → return (in) -> { ... };  （interface を返す）
今すぐやる手続き     → private void helper(...)
部品を組み立てて渡す → new Foo(...); set...; return foo;
依存を受ける         → コンストラクタ + final フィールド
```

---

## Q&A（2026-08-04 追記）

### Q. Bruno で Keycloak のアクセストークンを取りたい。エンドポイントは？

```text
POST http://localhost:8080/realms/LibraShare/protocol/openid-connect/token
```

- Discovery: `GET .../realms/LibraShare/.well-known/openid-configuration` の `token_endpoint`
- client: `live-qa-app` / secret: `live-qa-dev-secret`
- password grant を使うなら Admin で **Direct access grants** を一時 ON（realm JSON 既定は `false`）
- F-01 API は SESSION Cookie 前提。Bearer だけでは `/api/me` は通らない

### Q. デコードした ID トークンに realmRole（`realm_access`）が無い

**想定どおり。** LibraShare の `roles` スコープ mapper「realm roles」はだいたい:

| 設定 | 既定 |
|------|------|
| `access.token.claim` | true（Access Token に載る） |
| `id.token.claim` | 未設定＝ID Token に出さない |

→ Bruno では **`access_token`** をデコードして `realm_access.roles` に `HOST` があるか確認。  
→ backend mapper は IdToken の `realm_access` を読むので、認可用には Admin で **Add to ID token** を ON にするのが筋。

### Q. `attributes` の `realm_access` 処理（114–120 行付近）は List では？

**外側は Map。List は内側の `roles`。**

```json
"realm_access": { "roles": ["HOST", ...] }
```

- 114–120: `realm_access` を `Map<String,Object>` に作り直す
- `addRolesFromRealmAccess`: `get("roles")` を `List` として読む

### Q. `return mapped` した Set が SecurityContext になる？

**だいたい合っているが一段違う。**

```text
mapper の Set
  → Authentication.getAuthorities()
  → SecurityContext に Authentication 全体が入る
```

Set そのものが SecurityContext ではない。箱（Context）の中の権限袋。

### Q. その後 `provisionHost` で users に INSERT？

**INSERT は「その後」ではなく、mapper / SecurityContext より前。**

```text
loadUser:
  delegate.loadUser → provisionHost（無ければ INSERT）
→（loadUser 終了）
→ mapper（ROLE_*）
→ Authentication → SecurityContext
```

毎回 INSERT ではなく `findByOidcSubject` → 無ければ `save`。

### Q. ROLE 書き込み（mapper）は別メソッドでは？`loadUser` の中では？

**別メソッドで、`loadUser` の外。** 認識どおり。

| 処理 | どこ |
|------|------|
| OidcUser 組み立て | `delegate.loadUser` |
| users JIT | `provisionHost`（custom `oidcUserService` 内） |
| `ROLE_*` 追加 | `grantedAuthoritiesMapper`（Provider が loadUser **の後**に呼ぶ） |

以前の説明で「loadUser（組み立て・mapper）」とひとまとめにしたのは不正確。

---

## 関連

- [f01-auth.md](f01-auth.md) — 本番 F-01 の入口（JIT / loadUser / me）
- [ops.md](ops.md) — LibraShare realm / URL 2種
- [java.md](java.md) — DTO record
- [05-security-config.md](05-security-config.md) — 設定の概要・ハマり
- [02-session-cookie.md](02-session-cookie.md) — SESSION / Redis
- [06-me-controller.md](06-me-controller.md) — `/api/me` と Authentication  
