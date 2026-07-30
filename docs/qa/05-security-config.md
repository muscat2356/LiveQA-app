# SecurityConfig（oauth2Login / CSRF / 401 JSON）

## なぜこのクラスを作るか

デフォルト Security のままでは、ログイン後の橋渡し・CSRF のヘッダ名・API の 401 JSON が本番方針と揃わない。  
**無いと困る点**: Keycloak ログイン導線が曖昧、REST が HTML ログインへ飛ぶ、CSRF / HOST の確認がしにくい。

## データの流れ（ログイン成功まで）

```text
ブラウザ → GET /oauth2/authorization/keycloak
       → Keycloak でログイン
       → コールバック（認可コード）
       → 交換: コードをトークンに取り換える
       → 変換: クレーム／ロールを GrantedAuthority に読み替える
       → 格納: SecurityContext をセッション（Redis）へ
       → 発行: SESSION Cookie（＋ XSRF-TOKEN Cookie）
```

以降の API は Cookie で **復元** するだけ（JWT 再検証しない）。

## 操作語

| 操作 | この設定での意味 |
|------|------------------|
| 交換 | oauth2Login が認可コード→トークン |
| 変換 | Keycloak のロール等を `ROLE_HOST` などに読み替える |
| 格納 | 認証結果をセッションへ書く |
| 検証 | CSRF トークン突合、`hasRole('HOST')` |

## ライブラリ／型（正体）

| 名前 | 正体 |
|------|------|
| `SecurityFilterChain` | **インタフェース**。フィルタの並びの契約。Bean として返す |
| `HttpSecurity` | 設定を組み立てる **ビルダー的クラス** |
| `@EnableWebSecurity` | Security 設定を有効にする **アノテーション** |
| `@EnableMethodSecurity` | `@PreAuthorize` を効かせる **アノテーション** |
| `CookieCsrfTokenRepository` | CSRF トークンを Cookie（`XSRF-TOKEN`）に載せる **実装クラス** |
| `AuthenticationEntryPoint` | 未認証時の応答を決める **インタフェース**（ここで 401 JSON） |

## Keycloak 側（HOST）

realm `live-qa` でロール `HOST` を作り、テストユーザーに付ける。  
マッパーが `realm_access.roles` を `ROLE_HOST` に **変換**する。

## 写経ファイル

`src/main/java/com/example/sessionlab/config/SecurityConfig.java`（チャットの写経コードを参照）

## 任意: SessionConfig の namespace揃え

```java
@EnableRedisHttpSession(redisNamespace = "session-lab")
```

yml の `session-lab` と Redis キー接頭辞を一致させる。

---

## Q: `oauth2Login(Customizer.withDefaults())` だけでハイブリッド認証ができるのは何故？

**結論:** その1行は「OIDC ログインを有効にするスイッチ」に近い。ハイブリッドの残り（セッションに載せる・Redis・Cookie 名）は **別部品のデフォルト／自前設定** が組み合わさって成立している。

### `Customizer.withDefaults()` の意味

`Customizer` は **インタフェース**。「設定を細かくいじる」ための受け皿。  
`withDefaults()` は「追加のカスタマイズはしない＝Spring Security の標準 oauth2Login 動作でよい」という空のカスタマイザ。

つまり書いているのは実質:

> oauth2Login をオンにする（細かい上書きはしない）

### その1行がやってくれること（交換〜認証成功）

1. `/oauth2/authorization/{registrationId}`（ラボでは `keycloak`）をログイン開始口にする  
2. コールバック `/login/oauth2/code/keycloak` を受け取る  
3. **交換** → 認可コードをトークンに取り換える  
4. ユーザー情報を載せて `Authentication` を作る  
5. デフォルトのセッション方針のままなら、成功した認証を **セッションに格納** する  

「JWT を API のたびに検証しない」側は、そもそも `oauth2ResourceServer().jwt()` を書いていないから。入口はクライアント（oauth2Login）、以降はセッション、がハイブリッド。

### その1行だけでは足りない／別途用意しているもの

| 部品 | どこにあるか | 役割 |
|------|-------------|------|
| Keycloak の URL・client-id 等 | `application.yml` の `spring.security.oauth2.client` | **どこへ**ログインしに行くか（issuer からメタデータ取得） |
| `spring-boot-starter-oauth2-client` | `pom.xml` | oauth2Login 用の **ライブラリ** |
| セッション（stateful） | Security デフォルト + Session | 認証成功後に SecurityContext を **格納** |
| Redis にセッション本体 | `SessionConfig` + `spring.session` / session 依存 | 箱の倉庫 |
| Cookie 名 `SESSION` | `SessionConfig` の `CookieSerializer` | **発行**の作法 |
| `ROLE_HOST` への読み替え | `GrantedAuthoritiesMapper` | **変換**（defaults だけでは Keycloak ロールがそのままでは足りないことが多い） |

なので正確には:

> `oauth2Login(withDefaults())` だけで「OIDC 入口〜成功時にセッションへ載せる」標準動作が揃う。  
> ハイブリッド全体は **yml + oauth2-client + セッション（Redis）+（必要なら）ロール mapper** の合算。

LibraShare との差: あちらは `oauth2ResourceServer`（毎回 JWT **検証**）。こちらは login 成功後は Cookie セッション。

---

## Q: `csrfRepo` と `csrfHandler` はどこから来てる？

**結論:** Spring が自動注入した Bean ではない。`securityFilterChain` メソッドの **先頭で自分で `new`（ファクトリ呼び出し）して作ったローカル変数**。

```java
CookieCsrfTokenRepository csrfRepo =
    CookieCsrfTokenRepository.withHttpOnlyFalse();
CsrfTokenRequestAttributeHandler csrfHandler =
    new CsrfTokenRequestAttributeHandler();
csrfHandler.setCsrfRequestAttributeName(null);

http.csrf(csrf -> csrf
    .csrfTokenRepository(csrfRepo)      // 発行・保存の仕方を渡す
    .csrfTokenRequestHandler(csrfHandler) // リクエストからどう読むかを渡す
);
```

### 正体と役割

| 変数 | 型の正体 | 作り方 | この場での意味 |
|------|----------|--------|----------------|
| `csrfRepo` | `CookieCsrfTokenRepository`（**実装クラス**）。契約は `CsrfTokenRepository`（**インタフェース**） | `withHttpOnlyFalse()` というファクトリ | CSRF トークンを **発行**し、`XSRF-TOKEN` Cookie に載せる（JS から読める） |
| `csrfHandler` | `CsrfTokenRequestAttributeHandler`（**実装クラス**） | `new` | リクエスト属性／ヘッダからトークンをどう扱うか。`setCsrfRequestAttributeName(null)` で SS6 の XOR を避け、ヘッダ平文で **検証**しやすくする（ラボ向け） |

`.csrf(...)` に渡すことで、「CSRF フィルタよ、トークンの置き場は Cookie、読み方はこのハンドラで」と指示している。  
書かなければ Spring Security の CSRF デフォルト（別の Repository／Handler）が使われる。本番方針の `XSRF-TOKEN` + `X-XSRF-TOKEN` に寄せるために、ここで明示している。

---

## Q: `XSRF-TOKEN` がカギの認識でよい？

**半分合っていて、半分ズレる。**  
「カギ」と呼ぶなら **SESSION と XSRF は別の扉のカギ** だと覚える。

| Cookie | 何のカギか | 中身のイメージ |
|--------|------------|----------------|
| `SESSION` | **セッション箱**のカギ（ID） | Redis の箱を開いて「誰がログイン済みか」を **復元** |
| `XSRF-TOKEN` | **状態変更（POST 等）を許すための合い言葉** | 「このブラウザが、正当なページ経由で送っている」ことの証拠 |

### SESSION（セッションのカギ）

- クライアントが持つのは ID だけ  
- サーバ／Redis が箱を持つ  
- ログイン状態の **復元** に使う  

### XSRF-TOKEN（CSRF 用の合い言葉）

- **発行** → サーバがトークンを作り、`XSRF-TOKEN` Cookie に載せる（HttpOnly **false** → JS が読める）  
- フロントが値を読んで、POST 時にヘッダ `X-XSRF-TOKEN` に載せる  
- **検証** → Cookie の値とヘッダの値が一致するか確かめる  
- 一致しない／無い → 403（なりすましのクロスサイト POST を防ぐ）  

別サイトの罠フォームは、普通 **あなたの `XSRF-TOKEN` を読めない／ヘッダに載せられない** ので、SESSION Cookie が付いても POST が通りにくい。

### 一言

> `SESSION` = 「箱を開けるカギ」  
> `XSRF-TOKEN` = 「箱の中の人として、危険な操作（POST）をしてよいという合い言葉」  

両方カギに似ているが、**ログイン済みの識別は SESSION、書き込み操作の許可は XSRF**。

---

## Q: 自分の言語化は合っているか？（校正）

### 合っているところ

| あなたの言い方 | 判定 |
|----------------|------|
| `securityFilterChain(HttpSecurity)` がアクセス全体のルール設定 | ○ |
| ログイン開始は `oauth2Login(Customizer.withDefaults())` | ○ |
| `csrfRepo` が XSRF トークンを（Cookie に）載せる | ○ |
| `grantedAuthoritiesMapper` がロールを Spring 向け `ROLE_*` に書き換えて Set に入れる | ○ |

### 直したいところ（重要）

#### 1. CSRF が SESSION を格納する、は誤り

**SESSION Cookie の発行・セッション本体の格納は CSRF の仕事ではない。**

| 担当 | 何をするか |
|------|------------|
| Spring Session + `SessionConfig` | **発行** `SESSION` Cookie／**格納** Redis の箱 |
| `oauth2Login` 成功時 | 認証情報（SecurityContext）をその箱へ **格納** |
| CSRF（`csrfRepo`） | **発行・格納**するのは `XSRF-TOKEN`（合い言葉）だけ |

`http.csrf(...)` は「POST の検証ルール」であって、ログイン状態の Cookie 名や Redis 置き場は触らない。

#### 2. 「ログインまでは JWT を用いた Keycloak 認証」は半分だけ正しい

より正確には:

```text
ブラウザ → 認可コードフロー（パスワードを自前検証しない）
       → 交換: 認可コード → トークン（ID トークン等。中身はだいたい JWT）
       → 成功後: SecurityContext をセッションへ格納
       → 以降の API: SESSION Cookie で復元（JWT を毎回送らない・検証しない）
```

- ログイン処理の内部ではトークン（JWT 形式が多い）を扱う
- ブラウザが毎回 `Authorization: Bearer` で JWT を送る方式（LibraShare の resource server）ではない
- 「ログインまで JWT、その後 SESSION」という大枠は近いが、**CSRF が SESSION を書くわけではない**

#### 3. `authorities` は「生の JWT 文字列」ではない

`grantedAuthoritiesMapper` が受け取る `authorities` は:

> OIDC ログイン成功直後に Spring が用意した **`GrantedAuthority` の集合**

よく入っているのが `OidcUserAuthority`（権限の一種）。その中に ID トークン／UserInfo があり、そこから `realm_access` クレームを読む。

| 言い方 | よさ |
|--------|------|
| 「JWT そのものが authorities」 | △ ズレる（mapper の引数は Authority オブジェクト） |
| 「JWT／UserInfo のクレーム由来の情報が OidcUserAuthority に載っていて、そこからロールを取る」 | ○ |

流れ:

```text
トークン（JWT 等）のクレーム
  → Spring が OidcUserAuthority 等に載せる
  → mapper が authorities を受け取る
  → addRolesFormRealmAccess で realm_access.roles を読む
  → 変換: "HOST" → "ROLE_HOST"
  → Set に格納して返す
```

### 校正後の一文まとめ

> `securityFilterChain` が URL ごとの門番。  
> `oauth2Login(defaults)` が Keycloak 認可コードログイン（内部でトークン交換。成功後はセッションへ認証を格納）。  
> SESSION は Spring Session（`SessionConfig`）が発行・Redis 格納。  
> CSRF の `csrfRepo` は XSRF-TOKEN だけを扱う。  
> `grantedAuthoritiesMapper` は Authority（中身はトークンクレーム由来）からロールを取り、`ROLE_*` に変換して Set に入れる。

---

## Q: SESSION の発行／Redis 格納はどこで処理？コードに書く必要は？

### どこで処理されているか

自分で「Redis に SET する」コードは書かない。**Spring Session のフィルタ／リポジトリ**がリクエストの前後で動く。

```text
リクエスト到着
  → SessionRepositoryFilter（フレームワーク）が Cookie SESSION を読む
  → Redis から箱を復元（または新規作成）
  → コントローラ等の処理
  → 応答前に変更を Redis へ格納
  → 必要なら Set-Cookie で SESSION を発行
```

ラボで「書く」のは、スイッチと作法の指定だけ。

| 書く／用意するもの | 役割 |
|--------------------|------|
| `pom` の `spring-session-data-redis` + `data-redis` | ライブラリを入れる |
| `application.yml` の `spring.session` / `spring.data.redis` | Redis 接続・ストア種別 |
| `@EnableRedisHttpSession`（`SessionConfig`） | Redis を HttpSession 置き場にするスイッチ |
| `CookieSerializer` Bean（`SessionConfig`） | Cookie 名 `SESSION`・SameSite など **発行の作法** |

`SecurityConfig` に「SESSION を Redis へ」と書く必要はない。セッションが有効なまま `oauth2Login` が成功すると、認証情報は普通の HttpSession 属性として載り、それが Spring Session 経由で Redis に載る。

---

## Q: 「成功後にセッションへ格納」＝ Redis にトークン（JWT）が丸ごと入る？

**基本は「いいえ。毎回使うための JWT を倉庫に置いている」わけではない。**

入るのはだいたい次のイメージ。

| Redis の箱の中（イメージ） | 意味 |
|----------------------------|------|
| `SecurityContext`（認証済みであること、principal、`ROLE_HOST` など） | 「誰で、何の権限か」——以降の API はこれを **復元**する |
| その他セッション属性 | CSRF 関連など |

本番方針（ハイブリッド）:

> ログイン時にトークン交換はする。成功後はセッションで認証。  
> **アクセス／リフレッシュトークンは、以降不要なら破棄してよい**（毎回 JWT 検証しない）。

したがって:

| 言い方 | 判定 |
|--------|------|
| Redis に「ログイン結果（誰か・ロール）」が格納される | ○ |
| Redis に JWT を置いて、毎回それを検証している | ✗（このラボ／本番方針ではない） |
| ID トークン由来のユーザー情報が principal としてセッションに残ることがある | ○（オブジェクトとして。Bearer 運用ではない） |

流れの正確版:

```text
交換: 認可コード → トークン（JWT 形式が多い）
  → 変換: クレーム／ロールを認証オブジェクトへ
  → 格納: SecurityContext を HttpSession へ
  → Spring Session がそれを Redis に永続化
  → 発行: ブラウザには SESSION（ID）だけ
  → 以降: Cookie の ID で箱を復元（JWT 再検証しない）
```

---

## Q: ログイン成功後「JWT を検証 → セッションへ誰かを書き込む」（検証と格納は切り分け）でよいか？

**方向は合っている。** 「検証」と「格納」を分けるのも正しい。  
ただし「ブラウザに発行された JWT を、API のたびに検証する」イメージではない。

### より正確なイメージ（oauth2Login・認可コード）

```text
① 交換
   Spring（RP）が認可コードを Keycloak に渡し、トークンを受け取る
   （ID トークンはだいたい JWT。ブラウザの SESSION とは別物）

② 検証（JWT／OIDC としてのチェック）
   署名・issuer・audience・有効期限・nonce などを確かめる
   → 変換: クレームから「誰か」「ロール」を読む

③ 格納（セッション）
   SecurityContext（誰か・権限）を HttpSession へ書く
   → Spring Session が Redis へ永続化
   → 発行: ブラウザには SESSION（ID）だけ

④ 以降のリクエスト
   JWT 再検証はしない
   → SESSION で箱を復元するだけ
```

| 段階 | 操作 | 何をしているか |
|------|------|----------------|
| ログイン処理中 | **検証** | 受け取った ID トークン（JWT）が本物か・改ざんされていないか |
| ログイン処理中 | **格納** | 検証・変換の結果（誰か）をセッションへ書く |
| ログイン後の API | **復元** | Cookie `SESSION` で箱を読む（ここで JWT 検証しない） |

### あなたの言い方との対応

| 言い方 | 判定 |
|--------|------|
| 検証と格納は切り分け | ○ |
| 検証の結果「誰がログインしているか」がセッションに入る | ○ |
| ログイン成功後にブラウザ向け JWT を発行して、それを検証してからセッションへ | △ ズレやすい（JWT を出すのは主に Keycloak。ブラウザに載せる主鍵は SESSION） |
| 以降も毎回 JWT 検証 | ✗ ハイブリッドではやらない |

### 一文

> ログイン時だけ：トークン（JWT）を **検証** → 誰かをセッションに **格納**。  
> その後：SESSION で **復元**するだけ（検証と格納は一度きり、以降は切り分け済みの結果を使い回す）。

---

## Q: SecurityContext とは？認可コードとトークン交換はいつ・どこで？

### SecurityContext とは

Spring Security が持つ **「いまこのリクエスト（スレッド）の認証状態」の入れ物**。

中身の中心は `Authentication`（**インタフェース**）で、だいたい次が入る。

| 入っているもの | 意味 |
|----------------|------|
| principal | 誰か（例: OIDC ユーザー、`sub` や名前） |
| authorities | 何ができるか（例: `ROLE_HOST`） |
| authenticated など | 認証済みかどうか |

| 言い方 | 判定 |
|--------|------|
| 検証して得たロール情報そのものが SecurityContext | △ 狭い（ロール＝authorities の一部） |
| 検証・変換の結果としての「誰か＋権限」全体を載せる箱 | ○ |

ログイン成功後:

> **格納** → その箱（SecurityContext）をセッションへ書く → Redis へ  
> 以降の API → セッションから **復元**して、また SecurityContext に載せる

コードで「SecurityContext クラスを new する」必要はほぼない。oauth2Login 成功時にフレームワークがセットする。

---

### 認可コード → トークンの「交換」とは

OAuth2 / OIDC の **Authorization Code フロー**の一段。

| 用語 | 何か |
|------|------|
| 認可コード（authorization code） | 「このユーザーが IdP でログイン＆同意した」ことの短い使い捨て切符。API 呼び出し用の長期鍵ではない |
| トークン | ID トークン（誰か・クレーム。だいたい JWT）／アクセストークンなど |
| **交換** | 切符（コード）を Keycloak のトークンエンドポイントに渡し、トークンに取り換える |

ブラウザにパスワードを預からず、コードだけアプリに戻し、**サーバ側（Spring）が秘密付きでトークンに換える**のがポイント。

---

### 順序（ここがズレやすい）

**認可コードは「Spring 側のログイン成功後」に出るのではない。**  
Keycloak（IdP）でユーザーがログインに成功したあと、コールバックで **先にコードが届く**。

```text
1. ブラウザ → GET /oauth2/authorization/keycloak
2. Keycloak のログイン画面
3. ユーザーが Keycloak でログイン成功
4. Keycloak → ブラウザ redirect
      → /login/oauth2/code/keycloak?code=（認可コード）&state=...
5. Spring の oauth2Login フィルタが code を受け取る
6. 【ここで交換】Spring → Keycloak トークンエンドポイント
      （client-id / client-secret + code）
      → ID トークン等を受け取る
7. 【検証】ID トークン（JWT）を確かめる
8. 【変換】クレーム → ユーザー／ロール（mapper もここら）
9. 【格納】SecurityContext をセッションへ → Redis
10.【発行】SESSION Cookie をブラウザへ
```

| 誤解 | 正しい順 |
|------|----------|
| ログイン成功後に認可コードが発行される（アプリ成功の後） | IdP ログイン成功の結果として **コードが先**。アプリの「ログイン完了」は交換・検証・格納のあと |
| ブラウザがコードをトークンに交換する | **Spring（サーバ）**が交換する（`oauth2Login` の標準処理） |

### 交換はどこで実施される？

**自分で HTTP クライアントを書く場所ではない。**  
`oauth2Login(Customizer.withDefaults())` が有効なとき、Spring Security OAuth2 Client のフィルタ／`OAuth2AuthorizationCodeAuthenticationProvider` などが、コールバック処理の中でトークンエンドポイントを呼ぶ。

設定の根拠は `application.yml` の `client-id` / `client-secret` / `issuer-uri`（ここから token_endpoint 等が分かる）。

### 一文まとめ

> SecurityContext = 「誰か＋権限」の箱。  
> 認可コード = IdP ログイン成功後にコールバックで届く切符。  
> 交換 = その切符を Spring が Keycloak でトークンに取り換える（oauth2Login 標準処理の中）。  
> そのあと検証 → SecurityContext 作成 → セッション格納 → SESSION 発行。

---

## Q: IdP が Keycloak のとき、ログインで認可コードを得て、別のトークン取得エンドポイントにコードを渡す？

**はい、その理解でよい。**

Keycloak は IdP（OpenID Provider）として、だいたい次の2つを分けて持つ。

| エンドポイント | 役割 |
|----------------|------|
| 認可エンドポイント（authorization） | ブラウザでログイン・同意 → **認可コード**を redirect で返す |
| トークンエンドポイント（token） | **サーバ**が code + client 秘密を渡す → **トークン**を返す |

```text
ブラウザ ──(1)──→ Keycloak 認可EP（ログイン画面）
ブラウザ ←─(2)──  redirect + code=認可コード
ブラウザ ──(3)──→ Spring（/login/oauth2/code/keycloak）
Spring  ──(4)──→ Keycloak トークンEP（code を渡して交換）
Spring  ←─(5)──  ID トークン等
```

- (1)(2) はブラウザ ↔ Keycloak  
- (4) **交換**はブラウザではなく **Spring（RP）** が行う（client-secret をブラウザに出さないため）  
- トークン EP の URL は、起動時に `issuer-uri` から取る OpenID Configuration（`.well-known/openid-configuration`）の `token_endpoint` に書かれている  

なので一言:

> Keycloak でログインして認可コードを得る →（別の）トークン取得エンドポイントにそのコードを渡してトークンに交換する。渡す主体は Spring。

---

## Q: ログインとトークン取得で二度 API を叩く？それを `oauth2Login(withDefaults())` がやる？

**「二段ある」は正しい。ただしどちらも同じ種類の「API 叩き」ではない。**

| 段 | 誰が・どう叩くか | 何か |
|----|------------------|------|
| 1. 認可（ログイン） | **ブラウザ**が Keycloak の認可 EP へ（画面・redirect） | ユーザーがログイン → **認可コード**が Spring に戻る |
| 2. トークン取得 | **Spring サーバ**が Keycloak のトークン EP へ（サーバ間 HTTP） | code を渡して **交換** → トークン |

ブラウザから見ると「Keycloak に行って戻ってくる」が主で、トークン EP は表に出ない。  
Spring から見ると、コールバック後に **トークン EP をもう一段呼ぶ**。

（起動時の `.well-known/openid-configuration` 取得は別。ログインのたびではない。）

### `oauth2Login(Customizer.withDefaults())` がやってくれること

**はい。その一連を有効化・実行してくれる**（自分で「認可 URL を組み立てて、コールバックで code を読み、トークン EP を POST する」を書かなくてよい）。

`withDefaults()` は「細かい上書きなし」。実際に動くのは Spring Security OAuth2 Client の標準処理で、だいたい:

- `/oauth2/authorization/keycloak` で認可へ飛ばす  
- `/login/oauth2/code/keycloak` で code を受ける  
- トークン EP で **交換**  
- ID トークン **検証** → SecurityContext → セッション **格納**  

接続先は `application.yml` の registration / `issuer-uri`。

### 一文

> 認可（ブラウザ）とトークン交換（Spring→Keycloak）の二段。  
> その処理一式を `oauth2Login(Customizer.withDefaults())` が担う。

---

## Q: 検証・セッション格納までやってくれるのに Role 判断は自分で？Redis に改めて格納する？

**ロールの「変換」は自分でフックを書くことが多い。ただし Redis へ二度書きではない。**

### 何をフレームワークがやり、何を足すか

| 段階 | 誰が | 内容 |
|------|------|------|
| 交換・検証・Authentication 作成・セッション格納 | `oauth2Login` 標準 | 一通りやる |
| Keycloak の `realm_access.roles` → `ROLE_HOST` | **`GrantedAuthoritiesMapper`（自分の Bean）** | 権限の **変換**フック |
| `hasRole("HOST")` | Security の認可ルール | すでに入っている `ROLE_HOST` を **検証（照合）** |

`withDefaults()` は「ログイン成功〜セッションへ載せる」までやるが、  
Keycloak 特有のクレーム配置を **そのまま** `ROLE_*` にしてくれるとは限らない。  
だから mapper で「Spring が `hasRole` で探せる形」に読み替える。

### タイミング（二度格納ではない）

```text
交換 → 検証
  → Authentication を組み立てる途中で mapper が呼ばれる
      （authorities を ROLE_* 付きに変換）
  → その完成品を SecurityContext に載せる
  → セッションへ格納（1回）→ Spring Session が Redis へ
```

| 誤解 | 正しいイメージ |
|------|----------------|
| 先に Redis へ格納 → あとから Role 判断 → Redis に再格納 | ✗ |
| ログイン処理の中で Role を変換してから、その結果ごと **一度** セッション／Redis へ | ○ |

以降のリクエストでは Redis から **復元**した権限を見て `hasRole` するだけ。毎回 Keycloak や JWT でロールを取り直さない。

### 「Role 判断」の言葉の切り分け

| 言葉 | いつ | 誰 |
|------|------|-----|
| **変換**（mapper） | ログイン時・一度 | `grantedAuthoritiesMapper` |
| **検証**（認可） | API のたび | `hasRole("HOST")` 等 |

### 一文

> oauth2Login が検証〜セッション格納までやる。  
> mapper はその格納の **直前**に権限を `ROLE_*` へ変換するフック（Redis 再書き込みではない）。  
> `hasRole` はあとから、セッションに載った権限を照合するだけ。

---

## Q: `securityFilterChain` で mapper を明示しなくていい？ログイン途中に自動実行されて Context に Set が格納される？

### 明示が必要（`@Bean` だけでは呼ばれない）

`GrantedAuthoritiesMapper` を `@Bean` にしただけでは、**oauth2Login は使ってくれない**。  
`/api/me` の role が `UNKNOWN` になった実測での原因がこれ。

`securityFilterChain` で **どこに差すか** を明示する。

```java
.oauth2Login(oauth2 -> oauth2
        .userInfoEndpoint(userInfo -> userInfo
                .userAuthoritiesMapper(grantedAuthoritiesMapper())
        )
)
```

| 書き方 | 実際 |
|--------|------|
| `@Bean GrantedAuthoritiesMapper` だけ | 変換フックは **呼ばれない**（authorities は `OIDC_USER` などのまま） |
| `userAuthoritiesMapper(...)` で明示 | ログイン処理の途中で **呼ばれる** |

`Customizer.withDefaults()` は「標準動作のまま」なので、ロール変換のような差し込みは自分で書く必要がある。

### 自動実行のイメージ（○）

```text
コールバック → 交換 → 検証
  → Authentication / OIDC ユーザーを組み立てる途中
  → grantedAuthoritiesMapper.mapAuthorities(...) が呼ばれる
  → 返した権限付きで Authentication が完成
  → SecurityContext に載せる → セッション／Redis へ格納
```

**ログイン途中（格納の前）に mapper が走る**、で合っている。

### 「Context に Set<> が格納される」は少しだけ言い換え

mapper の戻り値は `Collection<? extends GrantedAuthority>`（実装では `Set`）。  
SecurityContext に丸ごと `Set` だけがフィールドとして置かれるわけではなく:

> **Authentication**（誰か＋権限）の中の authorities として入り、  
> その Authentication が SecurityContext に入る。

あとから `hasRole("HOST")` は、その Authentication の authorities を見る。

### 一文

> `@Bean` だけでは呼ばれない。`userAuthoritiesMapper(...)` で差し込んで初めて、ログイン組み立ての途中で変換が走る。  
> 返した権限は Authentication 経由で SecurityContext（→セッション）に載る。生の Set だけが Context に直置きされるイメージではない。

---

## Q: Mapper が「編集されている」のは何故？

### 結論

**Mapper の中身（変換ロジック）を直しているのではなく、`oauth2Login` 側の接続を直している。**

| 触る場所 | 何をしているか |
|----------|----------------|
| `grantedAuthoritiesMapper()` メソッド本体 | すでに書いた **変換**（`HOST` → `ROLE_HOST`）。今回の UNKNOWN の主因ではない |
| `.oauth2Login(Customizer.withDefaults())` → `userAuthoritiesMapper(...)` | 「この変換をログイン処理に **使う**」と差し込む |

### なぜ接続を変える必要があるか

1. `@Bean` で Mapper を DI コンテナに載せた＝「部品は用意した」
2. でも `withDefaults()` は「標準のまま・追加差し込みなし」
3. 標準は Keycloak の `realm_access.roles` を `ROLE_HOST` にしない
4. だから部品はあるのに **呼ばれない** → `/api/me` は `UNKNOWN`

接続イメージ:

```text
【いま】withDefaults()
  ログイン成功 → 標準 authorities（OIDC_USER 等）→ セッション
  ※ Mapper Bean は横に置いてあるだけ（未接続）

【直す】userAuthoritiesMapper(grantedAuthoritiesMapper())
  ログイン成功 → Mapper で変換 → ROLE_HOST 付き → セッション
```

### 編集の意味（操作語）

| 語 | この場面での意味 |
|----|------------------|
| **差し込み** | oauth2Login の「権限を決める直前」に、自分の Mapper を通すと指定する |
| **変換** | Mapper が `realm_access` の `HOST` を Spring が読む `ROLE_HOST` に変える（中身は既存のまま） |

### 一文

> UNKNOWN の原因は Mapper の変換式が壊れていることではなく、「変換部品をログインに繋いでいなかった」こと。編集対象は Mapper 本体より **oauth2Login の接続**。

---

## Q: `grantedAuthoritiesMapper()` 本体も編集したよね？

### はい（過去の修復）／今回の UNKNOWN とは別レイヤ

| いつ | 何を直したか | 目的 |
|------|--------------|------|
| 以前 | `grantedAuthoritiesMapper()` **本体** | ループが空 Set を回していた／`realm_access` の取り方／元権限を残す／`@SuppressWarnings` 削除など。**変換ロジックを正しくする** |
| 今回 | `.oauth2Login(...)` の **接続** | `@Bean` だけでは呼ばれないので、`userAuthoritiesMapper` で差し込む |

つまり:

- **本体の編集** … 「呼ばれたときに正しく `ROLE_HOST` を作れるか」
- **接続の編集** … 「そもそも呼ばれるか」

トークンにはすでに `HOST` があるのに `UNKNOWN` のまま、という今回の症状は、主に **接続（呼ばれていない）** 側です。  
本体を直していても、接続が無いと結果は同じ `UNKNOWN` になります。

### 一文

> 本体編集＝変換の中身。接続編集＝その変換をログインに使う指定。両方必要で、今回まだ足りないのは接続側。

---

## Q: では以前の本体修正は不要？

### いいえ、必要（本体と接続は AND 条件）

「接続を直せばいいなら本体修正は要らなかったのでは？」は誤解。両方そろわないと `HOST` にならない。

| 本体 | 接続 | 結果 |
|------|------|------|
| あり | なし | 呼ばれない → **UNKNOWN** |
| なし | あり | 呼ばれるが `realm_access` を変換できない → **UNKNOWN** |
| あり | あり | 変換される → **HOST** ✅ |

理由: 標準の oauth2Login は authorities を `OIDC_USER` / `SCOPE_*` にするだけで、**`realm_access.roles` を `ROLE_*` にする処理を持たない**。  
だから `ROLE_HOST` を作る本体は必須で、その本体を呼ばせる接続も必須。

### 残タスク

本体は導入済みなので、あと1つ：`.oauth2Login(Customizer.withDefaults())` を `userAuthoritiesMapper(grantedAuthoritiesMapper())` を差す形へ。**実装は本人が手で行う。**
