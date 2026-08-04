# `delegate.loadUser` とは何か

## 質問

カスタム `oidcUserService` 内の `delegate.loadUser(userRequest)` を解説してほしい。

---

## 一言で

**`delegate` は Spring 標準の `OidcUserService`（実装クラス）。**  
**`loadUser` は「OIDC ログイン途中のリクエスト情報から、アプリが使える `OidcUser` を組み立てる」メソッド。**

自前で JWT をパースして `OidcUser` をゼロから作る代わりに、**既定実装に委譲（delegate）**している。

```java
OidcUserService delegate = new OidcUserService();
OidcUser oidcUser = delegate.loadUser(userRequest);
```

---

## 登場人物の正体

| 名前 | 正体 | 役割 |
|------|------|------|
| `OidcUserService` | Spring Security の**具象クラス** | OIDC 用にユーザー情報をロードする標準実装 |
| `delegate` | そのインスタンスを指す変数名 | 「本体の仕事はこれに任せる」という意味でよく使う名前 |
| `loadUser` | `OAuth2UserService` 契約上のメソッド | 入力 → `OidcUser` を返す |
| `OidcUserRequest` | ロード時の入力オブジェクト | クライアント登録情報・アクセストークン・（OIDCなら）IDトークンなど |
| `OidcUser` | principal になるユーザー表現 | `sub` / `email` / `name` 等のクレーム＋権限の入口 |

`delegate` という名前に特別な Spring 注釈はない。**「委譲先」を表すローカル変数名**。

---

## `loadUser` がやること（概念）

正確な内部ステップはバージョンで差があるが、イメージは次のとおり。

```text
入力: OidcUserRequest
  （トークン交換直後。IDトークンや access token が載っている）

loadUser の中（標準実装が担当）:
  1. IDトークンの検証・クレーム読み取り（sub, email, name, realm_access…）
  2. 必要なら UserInfo エンドポイントへ問い合わせて属性を補完
  3. それらをまとめた OidcUser を生成して返す

出力: OidcUser
  → あとで Authentication の principal になり、SESSION に載る
```

**操作の意味:**

- **検証** → トークンが期待どおりか確かめる（標準実装側）  
- **復元／組み立て** → クレームから「今ログインした人」のオブジェクトを作る  
- **委譲** → その難しい部分を Spring に任せ、自コードは JIT だけ足す  

---

## カスタム全体の中での位置

```text
userRequest
  → delegate.loadUser(userRequest)   ★ ここ（OidcUser 完成）
  → userService.provisionHost(sub)   （DB JIT・自前）
  → return oidcUser                  （Spring に返す）
```

`loadUser` を呼ばないと `OidcUser` が手に入らず、`getSubject()` もセッション用 principal も作れない。  
逆に `loadUser` だけだと JIT が走らない。

---

## なぜ自分で `new OidcUser(...)` しないか

| 自前で全部書く | `delegate.loadUser` |
|----------------|---------------------|
| 署名検証・クロック・クレーム映射を自分でやる | Spring が既定どおりやってくれる |
| バグ・セキュリティ穴が増えやすい | 差分は JIT 1行に閉じられる |

**【EJ】** 車輪の再発明を避け、変わる部分（JIT）だけ自前にする、という合成。

---

## よくある誤解

| 誤解 | 実際 |
|------|------|
| `delegate` は特別な Spring Bean 名 | ただの変数。`new OidcUserService()` した局所オブジェクト |
| `loadUser` が DB の users を読む | 読まない。IdP トークン／UserInfo だけ。DB は `provisionHost` |
| 毎 API リクエストで `loadUser` が走る | 主に**ログイン・コールバック時**。以降は SESSION から `OidcUser` を復元 |

---

---

## Q: `userRequest` はどこから来る？

**自分で new しない。Spring Security の oauth2Login 処理が作り、`loadUser` の引数として渡す。**

```text
ブラウザ → /login/oauth2/code/keycloak?code=...
  → OAuth2 Login フィルタ／認可コードをトークンに交換
  → トークン等を詰めた OidcUserRequest を組み立て   ← ここで生まれる
  → 登録済み OAuth2UserService（今回のラムダ）を呼ぶ
        loadUser(userRequest)  または  userRequest -> { ... }
  → 返った OidcUser を Authentication に載せる
```

| 疑問 | 答え |
|------|------|
| 誰が作る？ | Spring Security（oauth2Login の内部） |
| 中身の材料は？ | 交換で得た IDトークン／アクセストークン、client registration など |
| アプリコードは？ | ラムダの引数 `userRequest` として**受け取るだけ** |
| 毎リクエスト？ | 主に**ログイン・コールバック時**。通常の `/api/me` では来ない |

コード上の対応:

```java
return userRequest -> {   // ← Spring が渡してきた OidcUserRequest
    OidcUser oidcUser = delegate.loadUser(userRequest);
    ...
};
```

`userInfoEndpoint.oidcUserService(...)` で「この Bean／ラムダを使え」と登録したので、コールバック成功時に Spring がここへ `userRequest` を渡してくる。
