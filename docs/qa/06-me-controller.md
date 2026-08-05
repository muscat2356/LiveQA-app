# MeController（GET /api/me・POST /api/lab/ping）

## なぜ作るか

ログイン後に「誰として入れているか」をフロント／curl で確認する口が無いと、セッション橋渡しの成功が分からない。  
`/api/lab/ping` は CSRF + HOST の通し確認用（本番には移植しない）。

## データの流れ

### GET /api/me

```text
ブラウザ → Cookie SESSION
  → 復元: Spring Session が Redis から SecurityContext を読み戻す
  → Controller が @AuthenticationPrincipal OidcUser を受け取る
  → 変換: クレーム（sub / name / email）と ROLE_* から応答 JSON を作る
  → 200 { id, displayName, email, role }
```

未ログインは Security の EntryPoint が 401 JSON（Controller まで来ない）。

### POST /api/lab/ping

```text
SESSION + ヘッダ X-XSRF-TOKEN
  → 検証: CSRF（Cookie XSRF-TOKEN と突合）
  → 検証: hasRole("HOST")（セッション内の ROLE_HOST）
  → 200 { "message": "pong" }
```

## ライブラリ／型

| 名前 | 正体 |
|------|------|
| `@RestController` | アノテーション。HTTP API として JSON を返すクラス |
| `@AuthenticationPrincipal` | アノテーション。SecurityContext の principal を引数に注入 |
| `OidcUser` | インタフェース。OIDC ログイン後のユーザー（クレーム取得） |
| `@PreAuthorize` | アノテーション。メソッド実行前に式で認可（要 `@EnableMethodSecurity`） |
| `record` | Java の簡潔な不変データ型（レスポンス DTO 向け） |

## 写経ファイル

- `controller/MeController.java`
- （任意）`dto/MeResponse.java` — Controller 内 record でも可

ラボの `id` は本番 DB の数値ではなく、クレーム `sub` 文字列でよい。

---

## Q: コントローラーのデータの流れ・認可検証の流れは？

### 全体の順番（リクエストが Controller に届くまで）

Controller のメソッドは、**認可を全部通ったあと**に初めて動く。  
手前で落ちると Controller には来ない。

```text
ブラウザ／Postman
  │ Cookie: SESSION, XSRF-TOKEN
  │ （POST なら）ヘッダ: X-XSRF-TOKEN
  ▼
① Spring Session … SESSION を鍵に Redis から箱を 復元
② SecurityContext … 箱から Authentication（誰か＋権限）を読み戻す
③ CSRF フィルタ … POST 等なら Cookie とヘッダを 検証
④ authorizeHttpRequests … URL 単位の認可（authenticated / hasRole）
⑤ @PreAuthorize … メソッド単位の認可（ping のみ）
  ▼
⑥ MeController のメソッド実行
```

| 段階 | 操作語 | 意味 |
|------|--------|------|
| ① | **復元** | Cookie の ID を鍵に、Redis から前回のセッション内容を読み戻す |
| ② | **読み戻し** | セッションに保存していた `Authentication` を `SecurityContext` に載せる |
| ③ | **検証** | `XSRF-TOKEN` Cookie と `X-XSRF-TOKEN` ヘッダが一致するか確かめる |
| ④⑤ | **検証（認可）** | 「ログイン済みか」「`ROLE_HOST` があるか」を確かめる |
| ⑥ | **変換・返却** | クレームや権限から JSON を組み立てて返す |

落ちたときの応答（Controller まで来ない）:

| 失敗箇所 | HTTP | ラボの JSON |
|----------|------|-------------|
| 未ログインで保護 URL | 401 | `UNAUTHORIZED` / ログインが必須です |
| CSRF 不一致 | 403 | （CSRF フィルタ側。JSON 形式はデフォルト寄りになりうる） |
| 権限不足（HOST なし） | 403 | `FORBIDDEN` / 権限がありません |

---

### GET `/api/me` — データの流れ

**なぜあるか**: ログイン後に「誰として入れているか」を JSON で確認する口。無いとセッション橋渡しの成功が分からない。

```text
GET /api/me  + Cookie SESSION
  → ①② 復元・読み戻し
  → ④ 検証: .requestMatchers("/api/me").authenticated()
       （ログイン済みなら OK。HOST は必須ではない）
  → ⑥ Controller へ
```

メソッド内の流れ（現行コード）:

```text
引数 user（OidcUser）
  ← @AuthenticationPrincipal が SecurityContext の principal を 引き渡し
  → getSubject / getFullName / getEmail でクレームを読む

引数 authentication（Authentication）
  ← Spring MVC が SecurityContext から認証結果を 引き渡し
  → getAuthorities() から ROLE_* を探す
  → 変換: "ROLE_HOST" → 応答の role 文字列 "HOST"
       （無ければ "UNKNOWN"）

戻り値 MeResponse
  → 変換: Java オブジェクト → JSON { id, displayName, email, role }
```

| 引数 | 型の正体 | 何を読むか |
|------|----------|------------|
| `user` | `OidcUser`（**インタフェース**）。principal 側 | `sub` / 表示名 / email |
| `authentication` | `Authentication`（**インタフェース**）。認証トークン全体 | 権限（mapper で足した `ROLE_HOST` は **こちら**） |

> **ポイント**: mapper の結果は `Authentication` に載る。  
> `user.getAuthorities()` だけ見ると `UNKNOWN` になりうる（過去の切り分け③）。

---

### POST `/api/lab/ping` — データの流れ＋認可の二重チェック

**なぜあるか**: CSRF と HOST が揃うかを一度に確かめるラボ専用口（本番には移植しない）。

```text
POST /api/lab/ping
  + Cookie SESSION, XSRF-TOKEN
  + ヘッダ X-XSRF-TOKEN
  → ①② 復元・読み戻し
  → ③ 検証: CSRF（Cookie とヘッダの突合）
  → ④ 検証: .requestMatchers("/api/lab/ping").hasRole("HOST")
       （URL 単位。ROLE_HOST が無ければここで 403）
  → ⑤ 検証: @PreAuthorize("hasRole('HOST')")
       （メソッド単位。同じ条件をもう一度。学習用の二重確認）
  → ⑥ ping() → 200 { "message": "pong" }
```

| レイヤ | どこに書いてあるか | 役割 |
|--------|--------------------|------|
| URL 認可 | `SecurityConfig` の `authorizeHttpRequests` | フィルタ段階で「この URL は HOST だけ」 |
| メソッド認可 | `@PreAuthorize` + `@EnableMethodSecurity` | Controller 実行直前に式で再チェック |

両方とも見ているのは同じ `Authentication` の authorities（中の `ROLE_HOST`）。  
`hasRole("HOST")` は内部で `"ROLE_"` を付けて照合する。

メソッド本体は単純:

```text
受け取り: （ユーザー情報は使わない。認可済みなら十分）
  → 返却: ResponseEntity.ok(Map.of("message", "pong"))
```

---

### 認可検証のイメージ図（ログイン時 vs 毎回のリクエスト）

**ログイン時（1回）** — 権限を箱に入れる

```text
Keycloak ログイン成功
  → 交換・検証（トークン）
  → grantedAuthoritiesMapper で 変換: HOST → ROLE_HOST
  → Authentication に格納
  → SecurityContext → セッション／Redis に格納
  → 発行: SESSION Cookie
```

**その後の各リクエスト** — 箱を開いて照合するだけ

```text
SESSION 付きリクエスト
  → 復元 → Authentication を読む
  → hasRole / authenticated / @PreAuthorize が 検証
  → Controller
```

ログインのたびに mapper をやり直すわけではない。  
**格納済みの権限を、毎回のリクエストで照合する**のが認可の本流。

### 一文

> Controller は「届いたあと」の変換・返却担当。  
> 認可は手前のフィルタ（URL）と `@PreAuthorize`（メソッド）が、セッションから復元した `Authentication` を検証して通す／落とす。

---

## Q: `user`（OidcUser）は principal なので Redis とは別？ SecurityContext だと思っている

### 結論（認識の修正）

| 認識 | 判定 |
|------|------|
| `OidcUser` は principal | ○ |
| それは SecurityContext 周り | ○（正確には SecurityContext → Authentication → **principal**） |
| Redis に格納されているものとは **別物** | ✕（別物ではない。**同じ箱の中身の一部**） |

### 入れ子の関係

```text
SecurityContext          ← 「いまこのリクエストの認証結果はここ」という置き場（スレッド上）
  └ Authentication       ← 契約（インタフェース）。誰か＋権限
       ├ principal  → OidcUser（user）   ← 「誰か」
       └ authorities → ROLE_HOST など    ← 「何ができるか」
```

リクエストのあいだはメモリ（`SecurityContext`）に載っている。  
リクエストが終わると Spring Session が **この SecurityContext（中の Authentication ごと）を Redis に格納**する。

```text
【リクエスト中】
  SecurityContext（メモリ）
    └ Authentication
         ├ principal = OidcUser
         └ authorities = [..., ROLE_HOST]

【リクエスト終了後】
  Redis セッション箱
    └ 直列化した SecurityContext 相当
         └ 同じ Authentication（OidcUser も含む）

【次のリクエスト】
  SESSION Cookie → Redis から 復元 → SecurityContext に読み戻す
  → @AuthenticationPrincipal が principal（OidcUser）を 引き渡し
```

だから:

- **別物ではない** … Redis に入っているのは「箱」。その中に `OidcUser`（principal）も入っている
- **見た目が違うだけ** … リクエスト中は `SecurityContext` 経由で触り、永続は Redis

### 「別」に見える理由（よくある混同）

| 見る場所 | 何が見えるか |
|----------|--------------|
| Redis のキー一覧 | `spring:session:sessions:...` のような **箱の ID** |
| Controller の `user` | 箱の中から取り出した **principal（OidcUser）** |
| Controller の `authentication` | 同じ箱の中の **Authentication 全体** |

Redis Insight で「OidcUser オブジェクト」がそのまま見えるわけではないが、**セッション属性として Authentication／SecurityContext が直列化されて入っている**。`user` はその復元結果の principal 側。

### SecurityContext と Redis の役割分担

| 名前 | 正体 | 役割 |
|------|------|------|
| `SecurityContext` | **インタフェース**（実装は `SecurityContextImpl` など） | **いまこのリクエスト**用の認証の置き場 |
| Redis（Spring Session） | 外部ストア | リクエストをまたいで箱を **格納／復元** |
| `SESSION` Cookie | クライアント側の鍵 | Redis のどの箱かを指す ID だけ |

> SecurityContext = 作業机の上（いま使う）  
> Redis = ロッカー（しまっておく）  
> principal（OidcUser）= 机にもロッカーにも入っている **同じ書類の「誰か」欄**

### 一文

> `OidcUser` は principal で、SecurityContext → Authentication の中にある。  
> Redis とは別物ではなく、セッションに格納／復元される Authentication の一部。リクエスト中は SecurityContext 経由で触る。

---

## Q: `/api/me` の role が `UNKNOWN`

> **2026-08-04 更新（現行 F-01）**  
> いまの `MeController` は `userService.currentHost` → **`users.role`（DB）** を返す。`backend/src` に `"UNKNOWN"` は無い。  
> したがって **SecurityConfig 不備では `"UNKNOWN"` は出ない**（出るのはだいたい **403**）。  
> `"UNKNOWN"` が見えるなら、(a) ラボ型の旧 Controller が動いている、(b) DB に `UNKNOWN` が入っている、(c) 古いビルド、を疑う。  
> 詳細手順は [14-f01-finish.md](14-f01-finish.md) の「UNKNOWN 切り分け」。

### 切り分けの順番（ラボ時代の旧実装向け）

旧 `MeController` は authorities に **`ROLE_HOST`** があるときだけ `HOST` を返す。  
疑うのは2箇所で、**トークン側 → 変換側** の順に見る。

#### ① トークンに `realm_access.roles` があるか（Keycloak 側）

direct access grant でトークンを取り、payload を見る。

```bash
curl -s -X POST "http://localhost:8081/realms/live-qa/protocol/openid-connect/token" \
  -d "client_id=live-qa-session-lab" -d "client_secret=change-me" \
  -d "username=host1" -d "password=host1" \
  -d "grant_type=password" -d "scope=openid profile email"
```

`id_token` の payload に次があれば Keycloak 側は正常。

```json
"realm_access": { "roles": ["offline_access", "HOST", ...] }
```

（ラボの realm JSON には `roles` スコープ + `realm_access.roles` の protocolMapper を入れてある）

#### ② mapper が呼ばれているか（Spring 側）← 実測での原因

`GrantedAuthoritiesMapper` を **`@Bean` にしただけでは呼ばれない**。  
`oauth2Login` に差し込んで初めて **変換**が走る。

```java
.oauth2Login(oauth2 -> oauth2
        .userInfoEndpoint(userInfo -> userInfo
                .userAuthoritiesMapper(grantedAuthoritiesMapper())
        )
)
```

`Customizer.withDefaults()` のままだと authorities は `OIDC_USER` / `SCOPE_*` などで、`ROLE_HOST` が無い → `UNKNOWN`。

#### ③ 権限を読む場所が合っているか（Controller 側）← ②を直しても UNKNOWN のとき

`grantedAuthoritiesMapper` の戻り値は **`Authentication`（認証トークン全体）** に載る。  
**principal（`OidcUser`）が持つ authorities は書き換わらない。**

```text
ログイン成功
 ├ principal（OidcUser）… OidcUserService が作ったまま
 │    authorities = [OIDC_USER, SCOPE_openid, SCOPE_profile, ...]   ← ROLE_HOST なし
 └ Authentication      … mapper の戻り値が入る
      authorities = [OIDC_USER, ..., ROLE_HOST]                     ← こちらにある
```

| 読む場所 | 結果 |
|----------|------|
| `user.getAuthorities()`（`@AuthenticationPrincipal OidcUser`） | `ROLE_HOST` **なし** → UNKNOWN |
| `authentication.getAuthorities()` | `ROLE_HOST` **あり** → HOST |

`hasRole("HOST")` / `@PreAuthorize` は `Authentication` を見るので、`/api/lab/ping` は通るのに `/api/me` だけ UNKNOWN、という食い違いが起きる。これが見分けの目印。

```java
import org.springframework.security.core.Authentication;

@GetMapping("/me")
public MeResponse me(@AuthenticationPrincipal OidcUser user, Authentication authentication) {
    // 権限は principal ではなく Authentication 側（mapper の結果はこちらに載る）
    String role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring("ROLE_".length()))
            .filter(r -> r.equals("HOST"))
            .findFirst()
            .orElse("UNKNOWN");

    String displayName = user.getFullName() != null ? user.getFullName() : user.getPreferredUsername();
    return new MeResponse(user.getSubject(), displayName, user.getEmail(), role);
}
```

| 型 | 正体 | 役割 |
|----|------|------|
| `Authentication` | **インタフェース**（認証結果の契約） | 「誰か（principal）＋権限（authorities）」を持つ。ロール判定はここ |
| `OidcUser` | **インタフェース**（principal 側） | OIDC クレーム（`sub` / `email` / `name`）を読む |

引数に `Authentication` を書くと、Spring MVC が `SecurityContext` から現在の認証を取り出して **引き渡す**（自分で `SecurityContextHolder` を触らなくてよい）。

### 反映手順

1. `SecurityConfig` に mapper を差し込む（②）  
2. `MeController` は `Authentication` から権限を読む（③）  
3. アプリ再起動  
4. **SESSION Cookie を捨てて再ログイン**（古い箱には古い権限が入っている）  
5. `GET /api/me` → `"role": "HOST"`

---

## Q: lab では Authentication から ROLE を取っていた。OidcUser との差分は？取れない認識でよい？

**認識はだいたい正しい。mapper で足した `ROLE_HOST` を見るなら `Authentication.getAuthorities()` が正。`OidcUser.getAuthorities()` だけだと足りない／ずれることがある。**

### 箱の中の分担

```text
SecurityContext
  └── Authentication
        ├── principal = OidcUser     … クレーム（sub, name, email）
        │     └── getAuthorities()   … 主に OIDC 由来（OIDC_USER, SCOPE_* 等）
        │                              ※ mapper 結果がここに無い／古いことがある
        └── getAuthorities()         … mapper 適用後の権限（ROLE_HOST が載る）
```

| 取りたいもの | どこから |
|--------------|----------|
| `sub` / name / email | **OidcUser**（`@AuthenticationPrincipal`） |
| mapper 後の `ROLE_HOST` | **Authentication.getAuthorities()** |
| `@PreAuthorize("hasRole('HOST')")` | 内部的に **Authentication** の authorities を見る |

### 「OidcUser から ROLE が取れない」の意味

- OidcUser に `getAuthorities()` メソッドは**ある**（インタフェース上は権限を持つ）
- ただし lab／本番でハマったのは、**GrantedAuthoritiesMapper の結果が Authentication 側に載る**一方、principal（OidcUser）側の authorities は **SCOPE_* のまま**になりうること
- だから `/api/me` で `user.getAuthorities()` だけ見ると **UNKNOWN**、`authentication.getAuthorities()` なら **HOST**、という差分が出た

→ 「認可用の ROLE は OidcUser ではなく Authentication から」が lab の教訓。

### F-01 仕上げ後のもう一つの正（openapi）

openapi: **`id` / `role` はローカル `users` 由来**。

| 方式 | 意味 |
|------|------|
| lab 骨組み | 応答の `role` 文字列も **Authentication** から作る |
| 仕上げ | 応答の `role` は **`users.role`（DB）**。認可ゲートは引き続き Authentication / `@PreAuthorize` |

DB の role と Keycloak の ROLE_HOST は、JIT で HOST 固定なら一致する想定。  
表示用 role と認可用 authorities を混ぜない、がポイント。

**近いが、イコールではない。**

| 名前 | 正体 |
|------|------|
| **SecurityContext** | 「今このリクエストの認証情報一式」を入れる**箱**（スレッド／セッションに紐づく） |
| **Authentication** | 箱の中身（principal・authorities・認証済みか） |
| **principal（OidcUser）** | Authentication の「誰」の部分 |
| **`@AuthenticationPrincipal`** | SecurityContext → Authentication → **principal だけ**を引数に取り出す注釈 |

```text
SecurityContext（広い・箱）
  └── Authentication
        ├── principal = OidcUser   ← @AuthenticationPrincipal が取るのはここ
        └── authorities = ROLE_HOST など
```

SESSION から復元されるのも、ざっくりこの SecurityContext 相当。  
「Context から取っている」感覚は正しく、正確には **Context 全体ではなく中の principal（OidcUser）**。

`@PreAuthorize` / `Authentication.getAuthorities()` が見るのは主に **authorities** 側で、OidcUser（クレーム）とは役割が違う。

---

## Q: セッション復元のあと、認可（Role の検証）はどうなる？

### 結論

**Keycloak や JWT からロールを取り直さない。**  
復元済み `Authentication.getAuthorities()` に入っている `ROLE_HOST` を、門番が毎回 **照合（検証）** するだけ。

```text
【ログイン時・1回】変換・格納
  realm_access "HOST" → mapper → ROLE_HOST → Authentication → Redis

【以降のリクエスト】復元・検証
  Redis → Authentication（ROLE_HOST 付き）
    → authorizeHttpRequests（URL）
    → @PreAuthorize（メソッド）
    → 足りなければ AccessDeniedHandler → 403
```

### なぜこの形か

権限の正は API（`Authentication`）。毎回 IdP に聞きに行くと遅延・結合が増える。  
F-01 は「ログイン時に箱へ入れ、以降は箱を見て通す／落とす」。

### 現行コードでの二段ゲート（`/api/me`）

| 段 | どこ | 式／ルール | 見ているもの |
|----|------|------------|--------------|
| ① URL | `SecurityConfig.authorizeHttpRequests` | `/api/me` → **`authenticated()`** | 「ログイン済みか」（ROLE までは見ない） |
| ② メソッド | `MeController.@PreAuthorize` | **`hasRole('HOST')`** | authorities に **`ROLE_HOST`** があるか |

```72:76:backend/src/main/java/com/liveqa/config/SecurityConfig.java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/guest").permitAll()
                .requestMatchers("/api/me","/api/auth/logout").authenticated()
                .anyRequest().permitAll()
            )
```

```36:41:backend/src/main/java/com/liveqa/controller/MeController.java
@GetMapping("/me")
@PreAuthorize("hasRole('HOST')")
public ResponseEntity<MeResponse> me(
    @AuthenticationPrincipal OidcUser oidcUser
) {
```

`hasRole("HOST")` は内部で接頭辞 `ROLE_` を付けて **`ROLE_HOST`** と照合する（自分で `"ROLE_HOST"` と書かなくてよい）。  
メソッド認可を効かせるスイッチが `@EnableMethodSecurity`（`SecurityConfig` クラス上）。

### データの流れ（Role 検証だけ抜き出し）

```text
SESSION 付き GET /api/me
  → Session / SecurityContext 復元（前問）
  → AuthorizationFilter
       検証: authenticated?  （匿名なら EntryPoint → 401）
  → DispatcherServlet → Method Security インターセプタ
       検証: hasRole('HOST')?
            authorities に ROLE_HOST あり → Controller へ
            なし → AccessDeniedHandler → 403 JSON
  → MeController / UserService
       ※ ここは「認可済み」前提。応答の role 文字列は DB users.role
```

| 失敗 | HTTP | 担当 |
|------|------|------|
| Cookie 無し・未ログイン | 401 | `AuthenticationEntryPoint` |
| ログイン済みだが `ROLE_HOST` 無し | 403 | `AccessDeniedHandler`（`@PreAuthorize` 失敗もここに寄る） |

### 認可用 Role と応答 JSON の `role` は別物

| 用途 | どこ | 例 |
|------|------|-----|
| **認可ゲート** | `Authentication.authorities`（Session 由来） | `ROLE_HOST` |
| **応答の表示** | DB `users.role` → `MeResponse.role` | `"HOST"` |

`UserService.currentHost` は DB 行の復元＋DTO 組み立て。**`hasRole` の代わりではない。**  
JIT で HOST 固定なら通常は一致するが、混ぜて考えない。

### 型の正体

| 名前 | 正体 |
|------|------|
| `GrantedAuthority` | インタフェース。「権限1個」の契約 |
| `SimpleGrantedAuthority` | 具象。`"ROLE_HOST"` 文字列を持つ |
| `hasRole` / `@PreAuthorize` | 式／アノテーション。SecurityContext の authorities を **検証** |
| `authorizeHttpRequests` | DSL。Filter 層の URL 認可ルール |

### 【EJ】

```text
【EJ】公開面最小化 … 権限判定は Security／アノテーション。Controller 内で if (role) しない
【EJ】enum 的発想 … 認可用は ROLE_* 文字列。応答 DTO の role と名前空間を分ける
【EJ】あえて外す … URL 側は authenticated のみ。HOST は @PreAuthorize に寄せている
```

### 一文

> Role 検証は「セッションから戻した `ROLE_HOST` を `hasRole` で照合」。  
> `/api/me` は URL でログイン必須、メソッドで HOST 必須。DB の `role` は応答用。

---

## Q: 認可が自動で「探しに行って」くれるのはなぜ？`@PreAuthorize` の効果？

### 結論

**`@PreAuthorize` は Redis／Keycloak／DB を探しに行かない。**  
メソッド実行直前に、すでに手元にある `SecurityContext` の authorities へ式を当てて **検証（照合）** するだけ。

「自動」に感じるのは、次の **役割分担がつながっている** から。

| 担当 | 何をするか | `@PreAuthorize` か？ |
|------|------------|----------------------|
| Session／SecurityContext 復元 | Cookie → Redis → `Authentication` を机に載せる | ✗（別 Filter） |
| `authorizeHttpRequests` | URL 単位の門番（例: `authenticated()`） | ✗（Filter） |
| **`@PreAuthorize`** | メソッド単位の門番（例: `hasRole('HOST')`） | ○ |
| Controller 本体 | 業務（me の組み立て） | ✗ |

### `@PreAuthorize` の効果（正確に）

```java
@PreAuthorize("hasRole('HOST')")
public ResponseEntity<MeResponse> me(...) { ... }
```

| 効果 | 内容 |
|------|------|
| する | メソッド呼び出し**前**に式を評価。失敗ならメソッドに入らず 403 |
| する | `hasRole('HOST')` → authorities に `ROLE_HOST` があるか **照合** |
| しない | Session／Redis の復元 |
| しない | Keycloak や JWT からのロール再取得 |
| しない | DB `users.role` の参照 |

スイッチ: `SecurityConfig` の **`@EnableMethodSecurity`**。これが無いとアノテーションは飾り（効かない）。

仕組みの正体は **メソッドセキュリティ（AOP インターセプタ）**:

```text
呼び出し側 → インターセプタ
  → SecurityContextHolder から Authentication を取る
  → 式 hasRole('HOST') を 検証
  → OK なら me() 本体へ／NG なら AccessDenied
```

Controller の中で `if (!hasRole) throw` を書かなくてよい、というのが「公開面最小化」の効きどころ。

### なぜ「探しに行ってくれる」ように見えるか

脳内のイメージが「role を取りに行く」になりがちだが、実際は:

```text
① 手前の Filter が机に Authentication を置いておく（復元）
② @PreAuthorize が机の上を見る（照合）
```

①が無いと机は空 → 未認証扱い。  
②が無いと（URL が `authenticated()` だけのとき）HOST 以外でもメソッドまで届きうる。

現行 `/api/me`:

```text
① authenticated() … 「誰か分かっているか」
② @PreAuthorize(hasRole HOST) … 「HOST か」← ここが Role 検討の本丸
```

### 型の正体

| 名前 | 正体 |
|------|------|
| `@PreAuthorize` | アノテーション（メタデータ）。「実行前にこの式で認可せよ」 |
| `@EnableMethodSecurity` | アノテーション。メソッド認可の仕組みを **有効化** |
| `hasRole('HOST')` | SpEL 風の認可式。接頭辞 `ROLE_` 付きで照合 |
| `SecurityContextHolder` | 静的ファサード。今スレッドの `SecurityContext` を取る入口 |

### 【EJ】

```text
【EJ】公開面最小化 … 認可はアノテーション／Filter。業務メソッドに混ぜない
【EJ】あえて外す … @PreAuthorize に「取得」はさせない。取得はログイン時 mapper＋Session 復元
```

### 一文

> 自動に見えるのは「復元が先・照合が後」だから。  
> `@PreAuthorize` の効果はメソッド前の **照合** であり、ロールの **探索・取得** ではない。
