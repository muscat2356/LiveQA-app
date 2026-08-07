# Cookie 跨ぎと Next.js サーバ（CORS との違い）

## 質問

ログイン後リダイレクト確認で「Cookie 跨ぎ」と言われた。Next.js サーバ経由なら CORS は問題ないと思うが、Cookie 跨ぎとは何か。

## 結論（先に）

**その理解で半分正しい。**  
Next サーバ（BFF）経由なら **ブラウザ↔Spring の CORS は避けられる**。  
ただし **Cookie 跨ぎは CORS とは別問題**で、次が残る。

1. **誰の応答の `Set-Cookie` を、ブラウザが覚えるか**（転送漏れ）  
2. **次のリクエストで、その Cookie を Spring まで届けるか**（転送漏れ）  
3. **Domain / SameSite / Secure**（付け方の契約）

CORS を消しても、2 が無いと `/api/me` は 401 のままになりうる。

## CORS と Cookie は別レイヤ

| | CORS | Cookie |
|--|------|--------|
| 誰のルールか | **ブラウザ**が「別オリジンの JS が応答を読んでよいか」 | **ブラウザ**が「この Cookie をどの URL に付けるか」 |
| サーバ同士 | **無い**（Next→Spring の `fetch` に CORS は効かない） | サーバは Cookie ジャーを自動では持たない |
| 失敗の見え方 | コンソールに CORS エラー | 静かに Cookie が付かない → 401 |

Member A 共有資料どおり、「BFF なら CORS が消える」は **ブラウザが Spring に直接 XHR しない**話。Cookie の話ではない。

## 2つのホップ（BFF）

```text
① ブラウザ  ←→  Next(:3000)     … 同一オリジン（画面のオリジン）
② Next プロセス  ←→  Spring(:8081) … サーバ間。CORS 対象外
```

ブラウザから見ると API も `:3000` なら CORS は消える。  
一方 Cookie は:

```text
Spring が Set-Cookie を返した相手が「Next プロセス」だけ
  → ブラウザの Cookie 箱には自動では入らない
  → Next が応答に Set-Cookie を載せ直す必要（転送）

ブラウザが Next に Cookie を付けて来た
  → Next→Spring の fetch に Cookie ヘッダを付け直す必要（転送）
```

これが「Cookie 跨ぎ」の本丸（[`07-guest-session.md`](07-guest-session.md) と同じ）。

## ログイン成功フローでは何が起きるか

openapi / 今回の successHandler:

```text
ブラウザ → Keycloak → ブラウザ → Spring コールバック(:8081)
  → Set-Cookie: SESSION（この応答はブラウザが直接受け取る）
  → 302 → http://localhost:3000/dashboard
```

このとき **SESSION 発行はブラウザ直受け**なので、転送漏れは起きにくい。  
`localhost` では Cookie にポートが無いため、`:8081` で付いた Cookie が `:3000` へのリクエストにも付きうる（同一サイト扱い・SameSite=Lax）。

問題が出やすいのはその後:

| 呼び方 | CORS | Cookie |
|--------|------|--------|
| ブラウザが **直接** `fetch('http://localhost:8081/api/me')` | **要る**（別オリジン）+ `credentials: 'include'` | ブラウザが SESSION を付ける（SameSite 次第） |
| **Next サーバ**が Spring の `/api/me` を呼ぶ | **不要** | ブラウザ→Next の Cookie を **明示転送**しないと Spring は匿名 |

「CORS は大丈夫」＝上表の右列（BFF）を選んだ、という意味に近い。  
その選択をするなら **Cookie 転送実装がセット**（現状 frontend は未着手、[`07`](07-guest-session.md)）。

## SameSite=Lax（いまの backend）

`SessionConfig` は `SameSite=Lax`。

- トップレベル遷移（ログイン後 302 でダッシュボードを開く）では Cookie が付きやすい  
- 別サイトからの cross-site POST には付かない（CSRF 緩和）  
- `:3000` のページから `:8081` へ XHR する場合は **同一サイトでも CORS + credentials** が別途必要

## 整理表（確認時の見方）

| 心配 | BFF（Next サーバ） | ブラウザ直 Spring |
|------|-------------------|-------------------|
| CORS | だいたい不要 | 必要 |
| ログイン直後の SESSION 発行 | コールバックが Spring 直ならブラウザが保持 | 同じ |
| その後の `/api/me` | **Cookie を Next→Spring に載せる実装が要る** | credentials + CORS |
| ゲスト POST の Set-Cookie | **Next が Set-Cookie 転送必須** | ブラウザが直接受ける |

## 一文

> CORS は「別オリジンの JS が応答を読んでよいか」。Cookie 跨ぎは「鍵をブラウザに覚えさせ、次のホップで Spring まで届けるか」。Next サーバなら前者は消えやすいが、後者は転送コードが無ければ残る。
