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
