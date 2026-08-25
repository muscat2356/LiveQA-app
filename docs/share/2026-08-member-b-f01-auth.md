# 共有会資料 — F-01 認証・セッション（網羅版 / Java 向け）

| 項目 | 内容 |
|------|------|
| 機能 ID | **F-01**（MVP-A 死守） |
| 担当 | B 主 / A=UI / C=Compose・Session共有 |
| 想定時間 | **本編 30〜35分** ＋ Q&A 10分（合計 ~45分） |
| 聞き手 | Java / Spring 経験者 |
| 契約の正 | [openapi-notes 認証節](../api/openapi-notes.md) / [scope F-01](../scope.md) |
| 学習メモ | [qa/f01-auth.md](../qa/f01-auth.md)（入口）／深いカタログは `02,03,05,06,26` ／ 用語は [qa/README](../qa/README.md#用語グロッサリ) |

設計の核:

> **Keycloak(OIDC) でログイン → Cookie セッション（Redis）で以降を管理。**  
> JWT を API ごとに検証する Resource Server ではない。CSRF 有効。HOST と匿名参加者の二系統。

---

## アジェンダ（時間配分）

**用語（この節）** — 以降の節はそれぞれ冒頭に用語表あり。全体の索引は [qa/README 用語一覧](../qa/README.md#質問に出てきた用語一覧グロッサリ)。

| # | 節 | 分 | 内容 |
|---|-----|-----|------|
| 0 | ゴールと F-01 定義 | 2 | scope / 受け入れ |
| 1 | アーキ選択 | 3 | Login+Session vs JWT RS |
| 2 | 登場人物と Cookie | 4 | SESSION / XSRF / Redis セットアップ・`SessionConfig` |
| 3 | エンドポイント一覧 | 3 | openapi 認証節の全部 |
| 4 | ログイン一連（OIDC→JIT→Session） | 6 | 型・OidcUserService・mapper |
| 5 | GET /api/me ＋ログイン後認可 | 5 | 復元→照合・層・DTO・`@PreAuthorize` |
| 6 | CSRF | 3 | repo / handler |
| 7 | 401/403 と Filter 境界 | 3 | EntryPoint vs Advice |
| 8 | logout（RP-initiated） | 3 | end-session・戻り先 |
| 9 | 匿名参加（guest） | 2 | voter_key |
| 10 | Keycloak / Compose / テスト | 3 | realm・CI |
| 11 | A/C 接点・学び・次 | 3 | |
| — | Q&A | 10 | |

短く切る場合: **0→1→3→4→5→8→11**（約20分）＋詳細は質疑。  
§5 では「ログイン後は探しに行かない／`@PreAuthorize` は照合」を必ず一言。

---

## 0. F-01 とは何か（網羅チェックリスト）

**用語（この節）**

| 語 | 意味 |
|----|------|
| F-01 | MVP-A 死守の認証・セッション機能一式 |
| OIDC / SSO | OpenID Connect。Keycloak で一度ログインし複数アプリに使う形 |
| JIT | Just-In-Time。初回ログイン時にローカル DB へユーザー行を作る |
| RBAC | ロールに基づく認可（HOST か参加者か） |
| CSRF | 別サイトからのなりすまし POST 等を防ぐ仕組み |

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

受け入れ疎通（[f01-auth.md](../qa/f01-auth.md)）:

1. 初回ログイン後 `users` に HOST 行  
2. `/api/me` → 数値 `id` + `role: HOST`  
3. 未ログイン → 401 JSON（openapi 文言）  
4. logout + CSRF → セッション破棄（＋ Keycloak 302）  
5. 非 HOST → 403  

---

## 1. アーキ選択（なぜこの形か）

**用語（この節）**

| 語 | 意味 |
|----|------|
| Resource Server / JWT RS | 毎リクエスト Bearer JWT を検証する形（`oauth2ResourceServer`） |
| oauth2Login | ブラウザ向けログイン＋セッション橋渡し（本プロジェクトの正） |
| IdP | Identity Provider。ここでは Keycloak |
| Authorization Code | 認可コード。トークンそのものではない短い許可証 |
| ID Token | ログイン処理の材料（誰か・クレーム）。以降の API 媒体ではない |
| confidential client | client secret を持つサーバ側クライアント（`live-qa-app`） |

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

**用語（この節）**

| 語 | 意味 |
|----|------|
| SESSION Cookie | セッション箱の**鍵**（中身のユーザー名は入っていない） |
| Redis | セッション箱の置き場（メモリ KV） |
| namespace（`live-qa`） | Redis キーの接頭辞。アプリ間の衝突を避ける |
| SecurityContext | リクエスト中の「誰で何の権限か」 |
| Spring Session | HttpSession を Redis 等に載せる仕組み |
| `SessionConfig` | Session 有効化と Cookie 形を決める設定クラス |
| HttpOnly / SameSite / Secure | Cookie の読み取り制限・送信条件・HTTPS 専用 |

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

### 2.2 Redis のセットアップ（インフラ → 依存 → yml）

セッション箱を使うには、**プロセス・依存・接続先**の3段が揃う必要がある（詳細は [03](../qa/03-redis.md)）。

#### (1) インフラ（Compose）

```bash
docker compose up -d redis
# または基盤ごと: docker compose up -d postgres redis keycloak
```

[compose.yml](../../compose.yml) の要点:

| 項目 | 値 |
|------|-----|
| サービス名 | `redis` |
| イメージ | `redis:7-alpine` |
| ホスト公開 | `6379:6379` |
| backend からの接続 | Compose 内は hostname **`redis`**（`SPRING_DATA_REDIS_HOST`） |
| ホストで `mvn` 起動 | `localhost:6379`（`application.yml` の default） |

#### (2) Maven 依存（`pom.xml`）

| 依存 | 役割 |
|------|------|
| `spring-boot-starter-data-redis` | Redis クライアント（Lettuce）・接続の自動設定 |
| `spring-session-data-redis` | HttpSession を Redis に載せる Spring Session 実装 |

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.session</groupId>
  <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

#### (3) `application.yml`（接続と store-type）

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  session:
    store-type: redis
    redis:
      namespace: live-qa   # yml 側。実際のキー接頭辞はアノテーションも要確認（下）
```

| 設定 | 意味 |
|------|------|
| `spring.data.redis.*` | **どこに繋ぐか**（ホスト・ポート） |
| `spring.session.store-type: redis` | Session の永続先を Redis にする宣言 |
| `spring.session.redis.namespace` | 自動設定経路向けの接頭辞。`@EnableRedisHttpSession` がある場合は **アノテーションの `redisNamespace` が効く**ことが多い |

疎通確認の目安:

```bash
# Redis が生きているか（Compose）
docker compose exec redis redis-cli ping   # → PONG

# ログイン後にキーが増えるか（namespace は live-qa）
docker compose exec redis redis-cli KEYS 'live-qa:*'
```

### 2.3 設定クラス `SessionConfig` の作成

ファイル: [`SessionConfig.java`](../../backend/src/main/java/com/liveqa/config/SessionConfig.java)

**なぜ作るか:** yml の接続先だけでは、Cookie 名（`SESSION`）や SameSite、namespace をアプリ方針どおりに固定しにくい。Spring Session を **明示的に有効化**し、Cookie の形をコードで決める。

**役割:** Redis HttpSession を有効化 → Cookie の発行属性を定義 →（実体の書き込みは Filter が担当）

```java
@Configuration
@EnableRedisHttpSession(redisNamespace = "live-qa")
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        // ローカル HTTP。本番 HTTPS では true へ
        serializer.setUseSecureCookie(false);
        serializer.setSameSite("Lax");
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
```

| 要素 | 型の正体 | 何のため |
|------|----------|----------|
| `@Configuration` | アノテーション | このクラスを設定クラスとして DI 容器に載せる |
| `@EnableRedisHttpSession` | アノテーション | Spring Session + Redis を有効化。`redisNamespace` でキー接頭辞 |
| `CookieSerializer` | **interface**（契約） | Session ID を Cookie にどう載せるか |
| `DefaultCookieSerializer` | 具象 | Cookie 名・Secure / SameSite / HttpOnly を設定 |
| `@Bean` | アノテーション | `CookieSerializer` をコンテナに登録し Session が使う |

【EJ】公開面最小化 … Cookie 方針は `SessionConfig` に閉じ、Controller から触らない  
【EJ】依存性注入 … Serializer は `@Bean` で定義し、フレームワークが取得  
【EJ】設定と実装の分離 … 接続先は yml、Session 有効化と Cookie 形は Java Config

Cookie 属性の意図:

| 設定 | ローカル開発 | 意味 |
|------|--------------|------|
| `CookieName=SESSION` | ✅ | フロント／C の文書と揃える |
| `HttpOnly=true` | ✅ | JS から SESSION を読めない（XSS 耐性） |
| `SameSite=Lax` | ✅ | 通常遷移では送る。厳しすぎない |
| `Secure=false` | ローカル HTTP | 本番 HTTPS では **true** |

### 2.4 Redis への格納はどのメソッドか

**自前の `redis.set` メソッドはない。**

```text
ログイン成功
  → Security が Authentication を SecurityContext / HttpSession へ
  → リクエスト終了付近で Spring Session（SessionRepositoryFilter 等）が Redis へ flush
```

アプリが書くのは **有効化＋Cookie 形**（`SessionConfig`）まで。flush / 復元は Filter 連鎖。

```text
有効化: @EnableRedisHttpSession(redisNamespace = "live-qa")
復元:   次リクエストで SESSION Cookie → Redis → 再び SecurityContext
```

キーのイメージ（ログイン後）:

```text
live-qa:sessions:{sessionId}
```

（ラボメモ [03](../qa/03-redis.md) の `spring:session:*` は **namespace 未指定時の既定**。live-qa はアノテーションで `live-qa` に固定している。）

---

## 3. エンドポイント一覧（F-01 契約）

**用語（この節）**

| 語 | 意味 |
|----|------|
| 認可エンドポイント | ログイン開始 URL（ブラウザを IdP へ 302） |
| コールバック | IdP から code を受け取る backend URL |
| クレーム | トークンに載る属性（`sub` / `email` / `name` 等） |
| 302 / 204 | リダイレクト／本文なし成功 |
| openapi 認証節 | REST 契約の正（Req/Res・401 文言） |

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

### 3.5 `POST /api/auth/guest`（公開・CSRF ignore）

- 匿名 Session + `guestId`  
- 応答: `{ guestId, voterKey: "g:{id}" }`  
- 参加者の 1人1票キー（F-04/F-05 で本格利用）

---

## 4. ログイン一連（本編の技術コア）

**用語（この節）**

| 語 | 意味 |
|----|------|
| `OidcUserRequest` | トークン交換後の材料。Spring が `loadUser` に渡す |
| `OidcUser` / principal | ログイン主体。クレーム取得用（Entity ではない） |
| `delegate.loadUser` | 標準 `OidcUserService` への委譲（検証・組み立て） |
| `provisionHost` | JIT。`users` に HOST 行が無ければ INSERT |
| `GrantedAuthoritiesMapper` | `realm_access` のロールを `ROLE_*` に変換 |
| `realm_access` | ID Token 内の realm ロール袋 |
| Authentication | principal＋authorities。mapper 結果はこちらに載る |
| 交換／検証／変換／格納／発行 | 操作語（code→token、期待一致、形の変換、Redis へ書く、Cookie を渡す） |

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

これが Session 属性として Redis に残る（§2.4）。

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

### 4.4 JIT（[f01-auth.md](../qa/f01-auth.md)）

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

## 5. GET /api/me ＋ ログイン後の認可

**用語（この節）**

| 語 | 意味 |
|----|------|
| 復元 | SESSION → Redis → HttpSession → SecurityContext へ読み戻す |
| 照合（検証） | 机上の `authorities` に `ROLE_HOST` があるか見る（再取得しない） |
| `@PreAuthorize` | メソッド実行**前**の認可式。探索ではなく照合 |
| `@EnableMethodSecurity` | メソッド認可を有効化するスイッチ（無いと `@PreAuthorize` 無効） |
| `@AuthenticationPrincipal` | SecurityContext の principal（`OidcUser`）を引数注入 |
| `MeResponse` | API 応答 DTO。`id`/`role` は DB、表示名はクレーム |
| `ROLE_HOST` vs `role:"HOST"` | 認可用 authorities ／ 応答 JSON の DB 列（混ぜない） |

### 5.1 ログイン後の認可の流れ（発表の核）

**結論:** Keycloak／JWT／DB からロールを取り直さない。  
Session に入っていた `ROLE_HOST` を門番が **照合**するだけ。自動に見えるのは「復元が先・照合が後」だから。

#### なぜこの形か

ログイン時だけ OIDC・mapper・JIT を通し、以降は Cookie の箱を開く（F-01 の正）。  
毎回 IdP に聞きに行く設計ではない。

#### 全体の流れ（`GET /api/me`）

```text
【ログイン時・1回】変換・格納
  realm_access "HOST"
    → mapper → ROLE_HOST
    → Authentication → Redis
    → 発行: SESSION Cookie

【以降・各リクエスト】復元 → 照合 → 業務
  Cookie SESSION
    →① SessionRepositoryFilter
         復元: Redis → HttpSession
    →② SecurityContextHolderFilter
         + HttpSessionSecurityContextRepository
         読み戻し: Session → SecurityContext（机に Authentication）
    →③ AuthorizationFilter（URL）
         検証: /api/me は authenticated()
         未ログイン → EntryPoint → 401
    →④ @PreAuthorize（メソッド）※ @EnableMethodSecurity 必須
         検証: hasRole('HOST') → authorities に ROLE_HOST?
         なし → AccessDeniedHandler → 403
         あり → me() 本体へ
    →⑤ MeController / UserService
         受け取り: 復元済み OidcUser
         DB 復元: users を sub で読む（認可の代わりではない）
```

| 段階 | 操作語 | 誰がやるか | `@PreAuthorize`? |
|------|--------|------------|------------------|
| ①② | 復元・読み戻し | Spring Session / Security Filter | ✗ |
| ③ | 検証（ログイン済みか） | `authorizeHttpRequests` | ✗ |
| ④ | 検証（HOST か） | `@PreAuthorize` + メソッドセキュリティ | ○ |
| ⑤ | 変換・返却 | Controller / Service | ✗ |

#### `@PreAuthorize` の効果（する／しない）

```java
@PreAuthorize("hasRole('HOST')")
public ResponseEntity<MeResponse> me(@AuthenticationPrincipal OidcUser oidcUser)
```

| する | しない |
|------|--------|
| メソッド呼び出し**前**に式を評価 | Redis / Keycloak / DB を「探しに行く」 |
| `ROLE_HOST` の有無を **照合** | ロールを新しく作る・取り直す |
| NG なら本体に入らず 403 | Controller 内の業務ロジック |

`hasRole("HOST")` は内部で接頭辞 `ROLE_` を付けて照合する。  
実体は AOP インターセプタが `SecurityContextHolder` から `Authentication` を取る。

#### 二段ゲート（現行コード）

| 段 | 設定 | 見ているもの |
|----|------|--------------|
| URL | `.requestMatchers("/api/me").authenticated()` | ログイン済みか（ROLE までは見ない） |
| メソッド | `@PreAuthorize("hasRole('HOST')")` | `ROLE_HOST` の有無 |

config で触れるのは:

- **ログイン時** … mapper で「どこから `ROLE_*` を作るか」（取り込み）
- **以降** … `hasRole` / `@PreAuthorize` で「何を要求するか」（照合ルール）

「毎リクエスト Keycloak を見に行け」という標準スイッチは無い（[05](../qa/05-security-config.md) / [06](../qa/06-me-controller.md)）。

#### 認可用 Role と応答の `role` は別物

| 用途 | どこ | 例 |
|------|------|-----|
| 認可ゲート | `Authentication.authorities`（Session 由来） | `ROLE_HOST` |
| 応答 JSON | DB `users.role` → `MeResponse.role` | `"HOST"` |

#### 発表用・一文

> ログイン後の認可は「机に戻した `ROLE_HOST` を `@PreAuthorize` が照合」。  
> アノテーションは探索装置ではなく、メソッド前の門番。

---

### 5.2 層分担（[java.md](../qa/java.md)）

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

### 5.3 `@AuthenticationPrincipal`

```text
SecurityContextHolder の Authentication.principal を注入
  ≠ Redis を注釈が直接読む
  鍵 → Redis 復元 → SecurityContext → Resolver
```

認可は **`Authentication.getAuthorities()`**（mapper 結果）。  
`OidcUser.getAuthorities()` だけ見るとズレることがある（[06](../qa/06-me-controller.md)）。

---

## 6. CSRF（[05](../qa/05-security-config.md)）

**用語（この節）**

| 語 | 意味 |
|----|------|
| CSRF | Cross-Site Request Forgery。別オリジンからの勝手な状態変更を防ぐ |
| `XSRF-TOKEN` | 合い言葉 Cookie（HttpOnly false → JS 可読） |
| `X-XSRF-TOKEN` | クライアントが付けるリクエストヘッダ（Cookie と同値） |
| `CookieCsrfTokenRepository` | CSRF トークンを Cookie に載せる実装 |
| `CsrfTokenRequestAttributeHandler` | リクエストから CSRF をどう読むか（SPA 向け設定あり） |
| stateful | サーバがセッションを持つ。CSRF を有効にする前提 |

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

参加者 guest の **発行**（`POST /api/auth/guest`）は CSRF ignore。質問 POST / いいねは CSRF 対象。

---

## 7. 401 / 403 と Filter 境界

**用語（この節）**

| 語 | 意味 |
|----|------|
| 401 Unauthorized | 未ログイン（誰か分かっていない） |
| 403 Forbidden | ログイン済みだが権限不足など |
| Filter chain | Controller の前に並ぶ Security の処理列 |
| `AuthenticationEntryPoint` | 未認証時に 401 JSON を書く |
| `AccessDeniedHandler` | 認可失敗時に 403 JSON を書く |
| `@ControllerAdvice` | Controller 到達後の例外をまとめて処理（Filter 401 は触れない） |

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

**用語（この節）**

| 語 | 意味 |
|----|------|
| RP-initiated logout | Relying Party（backend）起点で IdP の SSO も落とす流れ |
| end-session | Keycloak 側のログアウトエンドポイント |
| `post_logout_redirect_uri` | IdP ログアウト後のブラウザ戻り先 |
| `{baseUrl}` | リクエストを受けたアプリのオリジン（Compose なら backend :8081） |
| ローカル Session vs IdP Session | Redis の箱と Keycloak のログイン状態は別物 |

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

**用語（この節）**

| 語 | 意味 |
|----|------|
| 匿名セッション | Keycloak なし。SESSION Cookie だけ持つ参加者 |
| `guestId` | ゲスト識別用に Session に置く UUID 等 |
| `voter_key` / `voterKey` | 1人1票のキー。ゲストは `g:{guestId}` |
| HOST vs 参加者 | SSO 必須の主催者／ログイン不要の聴衆 |

```text
POST /api/auth/guest（CSRF ignore）
  → HttpSession に guestId
  → { guestId, voterKey: "g:{guestId}" }
```

| | HOST | 参加者 |
|--|------|--------|
| Keycloak | 必要 | 不要 |
| Session | OIDC Authentication 入り | guestId 属性 |
| 投票キー（将来） | `u:{userId}` 等 | `g:{guestId}` |

F-04/F-05 の土台。F-01 では「匿名 Session が付く」までが範囲。発行口は CSRF ignore、後続の変更系は CSRF 必須。

---

## 10. Keycloak / Compose / テスト / CI

**用語（この節）**

| 語 | 意味 |
|----|------|
| Realm | Keycloak 上のテナント区画（本プロジェクトは LibraShare 相乗り） |
| Client | OIDC クライアント登録（`live-qa-app`） |
| confidential | サーバ側で secret を持つクライアント種別 |
| `@WebMvcTest` | Controller 層をスライスする Spring テスト |
| Mockito | 依存を偽オブジェクトにして単体検証 |
| issuer / AutoConfig exclude | 起動時の IdP 解決を避けるためのテスト工夫 |

### 10.1 Realm（[ops.md](../qa/ops.md)）

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

### 10.3 テスト（[f01-auth.md](../qa/f01-auth.md)）

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

**用語（この節）**

| 語 | 意味 |
|----|------|
| Member A | Next.js / Cookie 付き REST・STOMP 受信 UI |
| Member C | STOMP 配信・Compose・CI |
| 権限の正は API | UI は補助。401/403 で最終判定 |
| STOMP ハンドシェイク | WS 接続開始時。同じ SESSION Cookie で認証する想定 |

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

**用語（この節）**

| 語 | 意味 |
|----|------|
| Spring Session Filter | HttpSession を Redis に読み書きする枠。自前 save メソッドはない |
| IdToken 一本化 | mapper が読むロール元を ID Token の `realm_access` に絞る |
| B-19 Advice | Controller 到達後の業務例外を JSON 化する Issue（Filter 401 は対象外） |

### 刺さった技術論点

1. Redis は **Compose → pom → yml → `SessionConfig`** の順で揃える（接続と Session 有効化は別）  
2. Redis 格納は自前メソッドではなく Spring Session Filter  
3. `{baseUrl}` ≠ フロント → post-logout 設定分離  
4. EntryPoint 直書きは UTF-8 明示  
5. mapper は IdToken の `realm_access` 一本化（Keycloak 側 id.token.claim 必須）  
6. `@AuthenticationPrincipal` ≠ Redis 直読  
7. Filter 401 は Advice に吸収できない  

### 次（B）

| 優先 | 内容 |
|------|------|
| 高 | Compose で受け入れチェックリスト消化 |
| 中 | mapper を IdToken のみへ簡素化（済みならスキップ） |
| 中 | 発表で §5.1（ログイン後認可）をデモ／口頭で通す |
| 後 | F-02 イベント CRUD（`host_id` = ローカル PK） |
| 別Issue | B-19 Advice / B-13 CORS |

---

## 13. 理解チェック（F-01 網羅）

**用語（この節）** — 復習の軸は各節の用語表。迷ったら [qa/README 用語一覧](../qa/README.md#質問に出てきた用語一覧グロッサリ) へ。

1. F-01 のエンドポイントを 5 つ挙げられるか？  
2. Login+Session と JWT RS の違いは？  
3. JIT が無いと何が壊れるか？  
4. `ROLE_HOST` と `MeResponse.role` の違いは？  
5. ログイン後の認可で `@PreAuthorize` は「探しに行く」か「照合」か？復元は誰がやるか？  
6. CSRF で SESSION と XSRF の役割分担は？  
7. 未ログイン 401 は誰が書くか？  
8. logout 後のブラウザ着地は誰が決めるか？  
9. guest の `voterKey` 形式は？  
10. Redis へ書く自前メソッドはどこにある／ないか？  
11. `SessionConfig` で `@EnableRedisHttpSession` と `CookieSerializer` はそれぞれ何を決めるか？  
12. yml の `namespace` とアノテーションの `redisNamespace`、どちらがキー接頭辞の正になりやすいか？  

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
  qa/02,03,05,06,07,09,14,15,18,23,25,26
  share/（本資料）
```

---

## 話し方メモ

- 冒頭で **F-01 チェックリスト（§0）** を見せ、「今日はこの全部」と宣言  
- 技術の山は **§2（Redis）→§4（ログイン・箱に入れる）→§5.1（ログイン後認可・箱を照合）→§7（401/403）→§8（logout）**  
- §5.1 の一言: **`@PreAuthorize` は探しに行かない／照合する。復元は手前の Filter**  
- Redis は「起動・依存・yml・設定クラス」と「書く自前メソッドはない」の **両面**を必ず触る  
- A には Cookie/CSRF/me/guest、C には SESSION 共有とポートを明示  
- 時間が押したら §6 と §10 の細部を質疑に回す。§5.1 の流れ図だけは残す  
