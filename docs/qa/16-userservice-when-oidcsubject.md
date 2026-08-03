# UserService はいつ動くか / oidcSubject はどこから来るか

## 質問

`UserService` が動くタイミングをデータの流れ込みで知りたい。`oidcSubject` はどのタイミングでどこから渡されるか。

---

## いまの事実（重要）

`provisionHost` / `requireByOidcSubject` は **定義されているだけ**。  
2026-08 時点で `SecurityConfig` や `MeController` から **まだ呼ばれていない**。

したがって **ログインしても UserService は自動では動かない**。  
次の写経（`OidcUserService` 差し込み + `MeController` 改修）で初めて「動くタイミング」が生まれる。

以下は **仕上げ後の想定フロー**（openapi / B-01 どおり）。

---

## oidcSubject とは何か

| 名前 | 正体 |
|------|------|
| OIDC の **`sub`（subject）** | Keycloak がユーザーごとに付ける不変の識別子（文字列） |
| アプリの **`oidcSubject` / 列 `oidc_subject`** | その `sub` をローカル DB に保存した値（橋） |

ブラウザやフロントが `oidcSubject` を POST するわけではない。  
**ID トークン（または UserInfo）のクレーム**から、サーバが読み取る。

---

## データの流れ（仕上げ後）

### A. 初回〜毎回ログイン（JIT = `provisionHost`）

```text
① ブラウザ
   GET /oauth2/authorization/keycloak
   → Keycloak のログイン画面へ（302）

② ユーザーが認証成功
   → Keycloak が認可コード付きでコールバック
   GET /login/oauth2/code/keycloak?code=...

③ Spring Security oauth2Login（backend）
   → 交換: 認可コードをトークン（IDトークン等）に取り換える
   → IDトークンを検証・パース
   → OidcUser を組み立てる（中にクレーム: sub, name, email, realm_access…）

④ ★ ここで（配線後）OidcUserService のカスタムが動く
   String sub = oidcUser.getSubject();   // ← oidcSubject の出所
   userService.provisionHost(sub);
        → users を oidc_subject で検索
        → 無ければ User.hostOf(sub) を save（JIT）
        → あればその行を返す

⑤ セッション発行
   → SecurityContext（OidcUser + ROLE_HOST 等）を Redis に格納
   → Set-Cookie: SESSION=...

⑥ 以降のリクエスト
   → Cookie からセッションを復元（毎回 Keycloak に sub を取りに行かない）
```

**`oidcSubject` が渡されるタイミング:** ④の直後＝**トークン交換が終わり、OidcUser ができた直後**。  
**出所:** `OidcUser.getSubject()` ＝ ID トークンの **`sub` クレーム**。  
呼び出し側（想定）: `SecurityConfig` に差し込んだ `OidcUserService` が `provisionHost(oidcUser.getSubject())` する。

### B. `GET /api/me`（`requireByOidcSubject`）

```text
① ブラウザ
   GET /api/me  + Cookie SESSION

② フィルタがセッションから Authentication を復元
   → principal は OidcUser（ログイン時に載せたもの）

③ MeController（改修後）
   OidcUser oidcUser = ...;
   userService.requireByOidcSubject(oidcUser.getSubject());
        → 同じ sub で users を復元
        → ローカル id / role を MeResponse に載せる
   displayName / email はクレームから（DB には持たない）
```

**タイミング:** ホストが `/api/me` を呼ぶたび。  
**出所:** また `OidcUser.getSubject()`（セッション内の principal）。DB の `oidc_subject` 列そのものをフロントが送るのではない。

---

## メソッドごとの「誰が・いつ・何を渡すか」

| メソッド | 誰が呼ぶか（想定） | いつ | 引数の中身 |
|----------|-------------------|------|------------|
| `provisionHost` | カスタム `OidcUserService` | ログイン・コールバック成功時（毎ログイン） | Keycloak の `sub` |
| `requireByOidcSubject` | `MeController` 等 | 認証済み API | 同じ `sub`（セッション上の OidcUser から） |

2回目以降のログインでも `provisionHost` は呼ばれるが、**検索ヒットして save しない**（JIT は「無ければ作る」）。

---

## いま動くものと動かないもの

| 処理 | いま |
|------|------|
| Keycloak ログイン → SESSION | ✅ 動く |
| realm ロール → ROLE_HOST（mapper） | ✅ 動く |
| `UserService.provisionHost` | ❌ 呼び出し元なし |
| `users` 行の自動作成 | ❌ 上記のため起きない |
| `/api/me` のローカル数値 id | ❌ まだ `sub` 文字列のまま（Me 未改修） |

---

## 【EJ】この設計での位置づけ

- **【EJ】公開 API の最小化** … `UserService` は Controller / Security 配線からだけ使う。フロントが `oidcSubject` を直接 POST する API は持たない（なりすまし防止）
- **【EJ】静的ファクトリ** … DB 行の生成は `User.hostOf(sub)` に閉じる
- **【EJ】例外** … `require…` は JIT 未実行を明示する unchecked。本番では後で 401/500 用の共通ハンドラへ載せ替え可

---

---

## Q: UserService は「session 箱の中身を検証するメソッド」か？

**その認識は少しズレる。** 近いのは `requireByOidcSubject` の呼び出し元がセッション上の `OidcUser` を使う点だけ。

| 層 | 役割 |
|----|------|
| **Spring Session / Security** | 箱（`SESSION` Cookie → Redis）の**発行・復元・改ざん検知・認証済みか**の検証 |
| **UserService** | 箱の中身ではなく、**ローカル DB `users` の検索・JIT 作成** |

- `provisionHost(sub)` … ログイン時に DB 行を作る／取る。**セッション検証ではない**（むしろセッションに載せる前〜直後の DB 作業）
- `requireByOidcSubject(sub)` … DB から行を**復元**する。`sub` はセッション内 `OidcUser` から取るが、検証しているのは「users に行があるか」であって、Cookie や Redis の中身そのものではない

たとえ:  
箱の鍵・封印を見るのが Security。箱に書いた「Keycloak の sub」を手がかりに、倉庫（DB）の名簿を引くのが UserService。

---

## Q: IdP に名前を持たない予定なのに displayName カラムがある？

**`users` 表に `displayName` / `display_name` 列はない。** 混同しやすい場所が2つある。

| 場所 | 何か | users との関係 |
|------|------|----------------|
| `questions.display_name` | 質問投稿時の**任意ニックネーム**（未入力なら UI 上「匿名」） | 主催者プロフィールではない |
| `MeResponse.displayName` | `/api/me` の **JSON フィールド** | DB 列ではなく、OIDC クレーム（`name` 等）から都度入れる想定 |

er-diagram の users: 「メール・表示名は Keycloak に統合し、**ローカルには保持しない**」。

IdP 側に名前クレームが無い／使わない運用なら:

- `/api/me` の `displayName` は `null`・空・`preferred_username` フォールバックなど、openapi をどうするかを決める
- **users に display_name を足す必要はまだない**（足すなら母体／プロフィール正の議論のあと）

**`requireByOidcSubject`（`/api/me` 改修後）については、だいたいその理解でよい。** ただし「ほかの user 情報」の出所を分ける。

```text
SESSION 復元
  → OidcUser（principal）
       ├─ getSubject()     → oidc_subject として DB 検索の鍵
       ├─ name / email 等  → クレームからそのまま返す（DB には無い）
       └─ （Authentication の ROLE_HOST 等）
  → UserService.requireByOidcSubject(sub)
       → ローカル users 行: id（PK）, role, createdAt …
```

| 欲しいもの | どこから |
|------------|----------|
| `sub` / 表示名 / email | セッション上の **OidcUser（クレーム）** |
| ローカル数値 `id` / DB の `role` | **users 表**（`oidc_subject` で検索） |

`provisionHost` は「セッションから復元して取る」というより、**ログイン直後に同じ `sub` で DB 行を作る／取る**側。

1. `SecurityConfig` で `oidcUserService` を差し込み、`provisionHost(oidcUser.getSubject())` を呼ぶ  
2. `MeController` で `requireByOidcSubject(oidcUser.getSubject())` を呼ぶ  

関連: [15-jit-provisioning.md](15-jit-provisioning.md) / [14-f01-finish.md](14-f01-finish.md)

---

## Q: `oidcUserService` と `userAuthoritiesMapper` はログイン成功時に二つ呼ばれる？

**「ログイン成功のあと、この2行が並んで2メソッド呼ばれる」イメージは少しズレる。**  
69–72 行は **パイプラインへの差し込み登録**。実行はログイン処理の**別ステージ（だいたい直列）**。

```text
認可コード → トークン交換
  → ① oidcUserService.loadUser
        OidcUser 組み立て + provisionHost(sub)（JIT）
  → ② userAuthoritiesMapper
        realm_access → ROLE_HOST 等を Authentication に載せる
  → SESSION に SecurityContext を格納
```

| 設定 | 役割 |
|------|------|
| `.oidcUserService(...)` | **誰が OidcUser を作るか**（＋ JIT） |
| `.userAuthoritiesMapper(...)` | **権限の読み替え** |

---

## Q: UserService を oidcUserService で再度包む意味は？

**「UserService をもう一度包む」のではなく、Spring 標準の `OidcUserService` を包んで、その前後に JIT を足している。**

```text
【標準だけ】
OidcUserService.loadUser → OidcUser 完成 → セッションへ

【今回】
カスタム OAuth2UserService（ラムダ）
  → 中で標準 OidcUserService.loadUser（委譲）
  → 続けて UserService.provisionHost(sub)（JIT）
  → 同じ OidcUser を返す
```

| 部品 | 役割 | 包んでいる？ |
|------|------|----------------|
| `OidcUserService`（delegate） | トークン／UserInfo から **OidcUser を作る**（Spring の仕事） | — |
| カスタム `oidcUserService` Bean | 上記に **JIT を足す薄いラッパ** | **delegate を包む** |
| `UserService` | DB の検索・INSERT | 包まれていない。**呼ばれるだけ** |

### なぜこの形か

1. **ログインの瞬間に JIT したい**（openapi: コールバックで users を JIT）。`/api/me` だけだと、me を呼ぶまで行ができない  
2. **Spring が既に持っている「OidcUser を作る入口」に差し込む**のが一番自然（`userInfoEndpoint.oidcUserService`）  
3. **SQL / JIT を SecurityConfig に直書きしない** → `UserService` に閉じる（テスト・再利用しやすい）

「UserService を oidc で包む」ではなく、**「Oidc のユーザーロードを包んで、中で UserService を呼ぶ」**、が正確。
