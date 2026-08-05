# 匿名セッション / voter_key（余力）

## なぜ作るか

参加者は Keycloak ログインしない。  
**無いと困る点**: 「1人1票」の判定キー（`voter_key`）を載せる箱（セッション）が無い。

ラボでは「匿名セッションを作って `g:{guestId}` を返す」まで。いいね UNIQUE や DB は不要。

## データの流れ

```text
POST /api/auth/guest  +（要）X-XSRF-TOKEN
  → 検証: CSRF
  → セッションを確保（無ければ新規発行）
  → guestId（UUID）を生成
  → 格納: HttpSession 属性 "guestId" に書き込む
  → 変換: voterKey = "g:" + guestId
  → 返却: 200 { guestId, voterKey }
  → 発行済みの SESSION / XSRF-TOKEN Cookie が付く（または更新）
```

| 操作語 | この場での意味 |
|--------|----------------|
| **格納** | サーバ側セッション（→ Redis）に `guestId` を書き込む |
| **変換** | `guestId` から本番制約どおりの `voter_key` 文字列を作る |
| **発行** | まだ無ければ `SESSION` Cookie をクライアントへ渡す |

## Security 側

`authorizeHttpRequests` で `/api/auth/guest` を **permitAll**（ログイン不要）。  
POST なので CSRF は有効のまま（匿名参加者にも CSRF を発行する、という本番方針）。

## ライブラリ／型

| 名前 | 正体 |
|------|------|
| `HttpSession` | サーブレットのセッション契約。Spring Session 利用時は中身が Redis に載る |
| `UUID` | 乱数の一意 ID。ゲスト識別子に使う |
| `@RestController` | JSON API 用のアノテーション |

## 疎通

1. ブラウザで一度 `GET http://localhost:8080/`（または任意）→ `XSRF-TOKEN` 取得  
2. Console:

```js
fetch('/api/auth/guest', {
  method: 'POST',
  headers: { 'X-XSRF-TOKEN': document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1] }
}).then(r => r.json()).then(console.log);
```

期待: `{ guestId: "...", voterKey: "g:..." }` かつ Cookie `SESSION` あり。

---

## Q: HOST の SESSION とゲストは同じ箱？

ラボ最小では **同じ SESSION Cookie 機構**を使う。  
属性として `guestId` を載せるだけ。本番では参加フローで発行タイミングが決まるが、箱の仕組みは同じ（Spring Session + Redis）。

---

## Q: Next.js 経由だと POST の voterKey が消える。GET で Redis から取るべき？

### 結論（役割を分ける）

| もの | どこに載る想定 | 捨てられると何が起きるか |
|------|----------------|--------------------------|
| `SESSION` Cookie | ブラウザ ↔（Next 経由なら転送）↔ Spring | 次リクエストで guest 箱を開けない |
| `voterKey` | **JSON ボディ**（現行）。Cookie ではない | Front がキーを持てない。ただし SESSION があれば **再計算可能** |

`voterKey` はもともと Cookie に入れる設計ではない。Redis に入っているのは `guestId` 属性で、`voterKey = "g:" + guestId`。

### 起きやすい原因（Next サーバ経由）

```text
Browser → Next (Route Handler / Server Action)
       → fetch(Spring POST /api/auth/guest)
Spring → Set-Cookie: SESSION + JSON { voterKey }
Next   → Set-Cookie をブラウザへ転送し忘れ／JSON を return し忘れ
Browser → SESSION も voterKey も無い、または片方だけ
```

「Redis からヘッダー情報が必要」より先に、**Next が `Set-Cookie` と JSON をブラウザまで渡しているか**を疑う。

### GET を足す案は妥当か

**SESSION がブラウザに残っている**なら、GET で「今の guest を返す」は有効（冪等な読み取り）。

```text
GET /api/auth/guest（案）+ Cookie SESSION
  → Redis から Session 復元
  → guestId 属性を読む（無ければ 401/404）
  → { guestId, voterKey: "g:"+guestId }
```

- CSRF 不要（GET）
- POST は「発行」、GET は「復元して教えて」と役割分担できる
- openapi に無いので **追加するなら契約更新が必要**（Member A と合意）

SESSION 自体が Next で落ちているなら、GET しても箱を開けない。**Cookie 転送の修正が本丸**。

### 推奨順

1. Next: Spring の `Set-Cookie` をブラウザへ転送 + POST の JSON をそのまま return  
2. それでも「ボディを保持したくない」なら backend に **GET 現状取得**を追加（Session から再構成）  
3. voterKey を独自 Cookie にするのは必須ではない（SESSION があれば足りる）

### 一文

> voterKey は Cookie ではなく JSON。落ちるなら Next の転送漏れが多い。SESSION が残るなら GET で Redis 上の guestId から再構成してよい。

---

## Q: Spring の Set-Cookie 転送ロジックはコードのどこ？（Next 経由）

### 結論（現状のリポ）

| 層 | Set-Cookie 関連コード |
|----|------------------------|
| **Spring（backend）** | **ある** … `SessionConfig` の `CookieSerializer` ＋ Spring Session Filter が `Set-Cookie: SESSION` を応答に載せる |
| **Next.js（frontend）** | **無い** … `Set-Cookie` 転送／proxy の実装は未着手（`frontend` はほぼ骨格のみ） |

「転送ロジックを解説して」→ **転送する側のコードはまだ存在しない**。捨てられる／届かない、は実装欠落として説明できる。

### Spring 側（発行の作法だけコードにある）

```19:26:backend/src/main/java/com/liveqa/config/SessionConfig.java
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseSecureCookie(false);
        serializer.setSameSite("Lax");
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
```

- アプリが `Set-Cookie` 文字列を手書きしているわけではない
- リクエスト終了時に Spring Session が Cookie を応答ヘッダへ載せる
- CSRF の `XSRF-TOKEN` は `CookieCsrfTokenRepository`（SecurityConfig）側

### 設計ドキュメントが期待している流れ（未実装）

[websocket-sequence.md](../design/websocket-sequence.md):

```text
Next → API（SSR）
API → Next（イベント情報 ※設計上ここでゲスト Session も想定）
Next → 参加者ブラウザ: 参加ページ + Set-Cookie SESSION
```

ここが「Next がブラウザへ Cookie を渡す」役。**その転送コードは frontend にまだ無い。**

### Next に書くとしたら（イメージ・未コミット）

```text
const upstream = await fetch(springUrl, { headers: { cookie: ... }, ... });
const setCookie = upstream.headers.getSetCookie?.() ?? [];
// NextResponse に setCookie を一つずつ append('Set-Cookie', ...)
return NextResponse.json(await upstream.json(), { headers: ... });
```

`fetch` が受け取った `Set-Cookie` は **自動ではブラウザに付かない**。明示コピーが「転送ロジック」。

### 一文

> 現状コードベースで解説できるのは Spring の「発行の形」だけ。Next の「転送」実装は未着手で、だからブラウザに SESSION が残らない、という整理になる。

---

## Q: Next.js サーバが Set-Cookie を「漏らす」のはなぜ？

### 結論

ブラウザの `fetch` と違い、**Next サーバ上の `fetch(Spring)` は Cookie ジャーを持たない／ブラウザ向け応答に自動では載せない**。  
Spring の `Set-Cookie` は「Next プロセスが受け取った上流ヘッダ」で止まり、**明示コピーしない限りブラウザに届かない**。

「わざと捨てている」より、**デフォルト動作が転送ではない**、が正確。

### なぜそうなるか

| 主体 | `Set-Cookie` を見ると |
|------|----------------------|
| **ブラウザ** | Cookie ジャーに保存し、以降のリクエストに `Cookie` を付ける（自動） |
| **Next サーバ（Node）の fetch** | 応答オブジェクトのヘッダとして読めるだけ。**ユーザのブラウザには関係ない** |
| **Next → ブラウザの応答** | 別の HTTP 応答。上流ヘッダは**自動継承されない** |

```text
Browser ──HTTP①──► Next
                      Next ──HTTP②──► Spring
                      Spring ── Set-Cookie ──► Next（ここで止まる）
Browser ◄──HTTP①の応答── Next
         ※ ①の応答に Set-Cookie を付け直さないとブラウザは知らない
```

HTTP① と HTTP② は別接続。②のヘッダは①に自動では乗らない。

### よくある実装

1. `return NextResponse.json(await res.json())` … ボディだけ返し `Set-Cookie` を見ていない  
2. SSR の `fetch` でヘッダを取っても `NextResponse` へ append していない  

### 一文

> 漏らす理由＝サーバ側 fetch はブラウザの Cookie 自動処理をしないから。転送は明示実装が必要。

---

## Q: 参加URLを叩いたときのメソッドの流れが分からない

### 結論（設計の正）

入口は **Next のページ GET**。イベント情報は **Spring の GET `/api/join/{joinCode}`**。  
匿名セッションは設計図によって「SSR 中に発行」と書いてあるが、**現行 backend は `POST /api/auth/guest`**。ここを混同しない。

### A. 参加URL直リンク（主催者が配る `/join/x7K2mQ9a`）

```text
① GET /join/{joinCode}          … ブラウザ → Next（HTML ページ。まだ API ではない）
② GET /api/join/{joinCode}      … Next SSR またはブラウザ → Spring（公開・イベント情報）
     応答例: { eventId, title, status }
③ （匿名 Session）
     設計シーケンス: SSR の②のタイミングでゲスト Session 発行、と書いてあることも
     現行 F-01 実装: POST /api/auth/guest（CSRF 付き）で guestId / voterKey
④ 以降 POST 質問・いいね … CSRF + SESSION 必須
```

| # | メソッド | 誰→誰 | 何のため |
|---|----------|--------|----------|
| ① | **GET** | Browser → Next | 参加ページを開く |
| ② | **GET** | Next/Browser → Spring | コードが実在するか・title/status |
| ③ | **POST**（現行） | Browser → Spring | 匿名 SESSION + voterKey |
| ④ | **POST** 等 | Browser → Spring | 質問・いいね |

### B. 参加コード手入力（`/join`）

```text
① GET /join                     … Next（入力フォーム）
② GET /api/join/{入力コード}    … 検証（無効なら 404）
③ ブラウザを /join/{joinCode} へ遷移
④ 以降は A の①〜と同じ
```

### 設計図と現行コードの差（重要）

| 資料 | 匿名 Session のタイミング |
|------|---------------------------|
| websocket-sequence | `GET /api/join` SSR 時に Redis へ guest、と読める |
| openapi + 現行 `GuestSessionController` | **`POST /api/auth/guest`** が発行口 |

実装を進めるときは **openapi / 現行 POST guest を正**にし、シーケンス図は「参加時に匿名 Session が付く」意図と読むのが安全。SSR で発行するなら契約と実装を合わせる必要あり。

### 直叩き（クライアント→Spring）のとき

ページ①は Next のまま。②③だけ Spring 直:

```text
GET  http://localhost:8081/api/join/{code}   … 情報（＋先に XSRF Cookie が付くことも）
POST http://localhost:8081/api/auth/guest    … 匿名 Session（X-XSRF-TOKEN 必須）
```

### 一文

> 入口の「参加URLを叩く」はまず Next の GET。API は GET join で部屋を確認し、匿名箱は現行では POST guest。
