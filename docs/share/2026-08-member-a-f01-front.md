# 1. SPA と SSR と CSR の違い

- **SPA**（Single Page Application）… ページ遷移のたびにサーバーへ HTML を取りに行かず、JS が画面を書き換え続ける方式
- **SSR**（Server Side Rendering）… 画面の組み立てをサーバー側でやり、完成した HTML をブラウザに送る方式
- **CSR**（Client Side Rendering）… 画面の組み立てをブラウザ側でやること。

---

## SPA　React

```
① HTML（ほぼ空）が届く          → 画面は真っ白
② JS をダウンロード              → まだ真っ白
③ JS が実行され、DOM を組み立てる → 骨組みが出る
④ useEffect が fetch を投げる    → 「読み込み中...」
⑤ データが返る → 再描画          → ようやく中身が出る
```

## SSR　Next.js

```
① サーバーがデータを取りに行く    ← ここが先
② サーバーが HTML を組み立てる
③ 中身の入った HTML が届く        → いきなり中身が見える
④ JS をダウンロード
⑤ JS が HTML に接続される         → ここからボタンが効き始める
```

HTML はただの文字列なので、「このボタンを押したらカウントを +1 する」という処理そのものは書けない。

- 処理の担当は JavaScript

```
SSR        ＝ リクエストのたびにサーバーでHTMLを組み立てる（＝タイミングの話）

その中身：
  Server Component → サーバーで実行、結果だけ送る、JSは送らない
  Client Component → サーバーで実行してHTML化、＋JSも送る
                     → hydrationで動き出す（stateもAPI通信も可能に）
```

---

## ハイドレーション　SSR の重要な単語

サーバーが送るのは HTML という文字列です。文字列なので、`onClick` のような関数は含められません。

つまり、③ の時点で画面は見えていますが、ボタンを押しても何も起きない。

そのあと JS が届いて、届いた HTML に対して「このボタンにはこの関数を繋ぐ」という後付けの接続作業が走る。
これを **ハイドレーション（hydration）** と呼ぶ。

```
Client Component の一生

① サーバーで1回描かれる          → HTML に「👍 5」が入る（＝見えている）
② JS がブラウザに送られる
③ ブラウザで再接続される（hydration）→ ここから onClick が効き始める
```

---

# 2. Next.js サーバーとは

Next.js サーバーとは、ブラウザと API サーバーの間で中継するサーバー。

```
ブラウザ  ⇄  Next.js サーバー  ⇄  Spring Boot
```

## fetch を実行について

```tsx
export default async function Page() {
  const events = await fetch("http://localhost:8080/api/events");
  return <ul>...</ul>;
}
```

このコードは `app/` の中にあり、Server Component として動く。
つまり **fetch を投げているのはブラウザではなく、Next.js サーバー。**

### Next.js サーバーが持っていないもの

- Cookie を保存して自動で付ける機能
- Cookie を読んでヘッダーに載せる仕組み

**Cookie が Next.js で途切れる。**

---

## 2-4. Cookie とは何か

HTTP は相手を覚えない。1回のリクエストが終われば忘れる。
それを埋めるのが Cookie。正体は**ただの「名前=値」の文字列**。

```
SESSION=abc123
```

1. サーバーが `Set-Cookie: SESSION=abc123` を返す
2. ブラウザが保存する
3. 次からブラウザが、同じ宛先のリクエストに勝手に付ける

**Cookie 方式は「ブラウザが自動で付ける」ことに全面依存している。**
その機能を持たない Next.js サーバーが間に入った瞬間、前提が崩れる。

---

## 2-5. メリットについて

管理下にある中継地点として使える（**BFF: Backend For Frontend**）。

```
【中継しない】
ブラウザ ────────────────────→ Spring Boot API
        (別ドメインへ直接アクセス)

【中継する = BFF】
ブラウザ ──→ Next.js サーバー ──→ Spring Boot API
        (同一ドメイン)      (サーバー間通信)
```

Server Component の fetch、または Route Handler が中継役。2本目の矢印はブラウザを経由しない。

### 主なメリット

- **CORS が消える** — ブラウザから見れば同一オリジン
- **隠せる** — API キー、内部エンドポイントの構造
- **形を整えられる** — B の API 3本を1本にまとめて返す、など

---

### CORS が消える

**CORS が起きる理由**

ブラウザは「表示中のページのオリジンと違う場所へのリクエスト」を制限する。
オリジン = **スキーム + ホスト + ポート** の組。

```
https://myapp.com          ← 表示中のページ
https://api.myapp.com      ← ホストが違う → 別オリジン → CORS 発動
```

**BFF にすると**

```
ブラウザ → https://myapp.com/api/products   ← 同一オリジン。CORS の対象外
Next.js → https://api.internal/products     ← ブラウザではないので CORS 無関係
```

CORS はブラウザが課すルールであり、サーバー間通信には存在しない。設定ごと不要になる。

---

### 隠せること

**ブラウザに配られたコードは全部見える**

DevTools の Network タブ、view-source、bundle を読めば、ソースに書いた文字列は露出する。

```tsx
// NG: "use client" のファイルに書くと、鍵がブラウザに露出する
const res = await fetch('https://api.example.com/v1/data', {
  headers: { 'X-API-Key': 'sk_live_xxxxx' }
});
```

**BFF にすると**

```tsx
// Server Component / Route Handler（ブラウザに送られない）
const res = await fetch('https://api.internal.example.com/v1/data', {
  headers: { 'X-API-Key': process.env.API_KEY }   // 環境変数はサーバーに留まる
});
```

| 隠せるもの | 効果 |
|---|---|
| API キー・トークン | 漏洩そのものの防止 |
| 内部エンドポイントの URL | 攻撃者に構造を教えない |
| マイクロサービスの分割構造 | サービスが何本あるか分からせない |
| レスポンスの余計なフィールド | `password_hash` や `internal_memo` を除去できる |

---

### 形を整えられる

**問題：画面1つに対して API が複数**

商品詳細ページに「商品情報 / レビュー / 在庫」が必要な場合。

```
ブラウザ ──→ GET /products/1          (往復 150ms)
        ──→ GET /products/1/reviews   (往復 150ms)
        ──→ GET /inventory/1          (往復 150ms)
```

ブラウザ→サーバーは物理的に遠く、モバイル回線ではさらに遅い。
依存関係があると直列になり、待ち時間が積み上がる。

**BFF にすると**

```tsx
// Next.js 側で3本を並列に叩いて、1つにまとめる
export default async function ProductPage({ params }) {
  const [product, reviews, stock] = await Promise.all([
    fetch(`${API}/products/${params.id}`).then(r => r.json()),
    fetch(`${API}/products/${params.id}/reviews`).then(r => r.json()),
    fetch(`${API}/inventory/${params.id}`).then(r => r.json()),
  ]);

  return <ProductDetail product={product} reviews={reviews} stock={stock} />;
}
```

Next.js と Spring Boot は同じデータセンター内にあることが多く、この3往復は数 ms。
ブラウザとの通信は1回で済む。

---

### BFF のトレードオフ

| デメリット | 内容 |
|---|---|
| 層が1つ増える | デプロイ対象、監視対象、障害点が増える |
| ロジックの置き場所が曖昧になる | 「BFF に書く？Spring に書く？」の判断が必要 |
| 単一障害点になりうる | BFF が落ちると全部落ちる |

> **原則：業務ルールは必ず Spring 側。BFF は整形と中継に徹する。**
> ここにドメインロジックを書き始めると二重管理になって崩れる。

---

## Route Handler（`route.ts`）

### 位置づけ

Next.js に付いてくる「自前の API を書く仕組み」。Spring の `@RestController` に相当する。

> **重要：URL が生えるだけで、中身は空っぽ。** DB は自動的には繋がらない。
> `@GetMapping` を書いても DB に繋がらないのと同じで、中身は自分で書く。

### ファイル配置がそのまま URL になる

```
app/
├── api/
│   ├── products/
│   │   └── route.ts        →  /api/products
│   └── users/
│       └── [id]/
│           └── route.ts    →  /api/users/123
```

### HTTP メソッドは関数名で決まる

```ts
// app/api/products/route.ts

export async function GET() {
  return Response.json({ message: 'GET が呼ばれた' });
}

export async function POST() {
  return Response.json({ message: 'POST が呼ばれた' }, { status: 201 });
}
```

**パターン1：固定値を返す（動作確認用）**

```ts
export async function GET() {
  return Response.json([{ id: 1, name: 'りんご' }]);
}
```

**パターン2：Spring Boot に転送する（BFF）**

```ts
export async function GET() {
  const res = await fetch('http://localhost:8080/api/products');
  return Response.json(await res.json());
}
```

DB に繋がっているのは Spring。Next.js は取りに行って渡すだけ。

**パターン3：Next.js から直接 DB を叩く**

```ts
import { PrismaClient } from '@prisma/client';
const prisma = new PrismaClient();

export async function GET() {
  const products = await prisma.product.findMany();  // PostgreSQL を直接読む
  return Response.json(products);
}
```

---

## まとめ

```
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│    ブラウザ      │      │  Next.js サーバー │      │   Spring Boot    │
│                  │      │     (Node.js)     │      │                  │
│  ┌────────────┐  │      │                   │      │   ┌──────────┐   │
│  │ 👛 Cookie  │  │      │   ❌ Cookie なし  │      │   │PostgreSQL│   │
│  │  SESSION=  │  │      │   ❌ document なし│      │   └──────────┘   │
│  │   abc123   │  │      │   ❌ アドレスバー │      │   ┌──────────┐   │
│  └────────────┘  │      │        なし       │      │   │  Redis   │   │
│                  │      │                   │      │   └──────────┘   │
└────────┬─────────┘      └─────────┬─────────┘      └────────△──△──────┘
         │                          │                         │  │
         │  ①SSR の経路             │                         │  │
         └──────────────────────────┘                         │  │
              Cookie は自動で付く    └─────────────────────────┘  │
                                       ⚠️ ここで Cookie が途切れる │
                                          （手で詰め直す必要あり） │
         │                                                        │
         │  ②ブラウザ直の経路                                     │
         └╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┘
                          ✅ Cookie が自動で付く
```

---

# 3. フォルダ構成の意味

## 3-1. フォルダがそのまま URL になる

ファイルシステムルーティング（フォルダ = URL）。
App Router では **ファイル配置 = ルーティング**。設定ファイルは書かない。

```
app/
├── page.tsx                 →  /
├── events/
│   ├── page.tsx             →  /events
│   └── [id]/
│       └── page.tsx         →  /events/123
└── join/
    └── [joinCode]/
        └── page.tsx         →  /join/ABC123
```

`[id]` のような **角カッコ = 動的セグメント**。URL の一部を変数として受け取る。

```tsx
export default async function Page({ params }: { params: { id: string } }) {
  const event = await fetchEvent(params.id);   // ← /events/123 なら "123"
}
```

---

## 3-2. `app/` の中の予約ファイル

`app/` の中でだけ特別な意味を持つファイル名がある。

| ファイル | 役割 |
|---|---|
| `page.tsx` | その URL の中身。**これがないと URL にならない** |
| `layout.tsx` | 共通の外枠。配下の全ページを包む |
| `loading.tsx` | データ待ちの間に出す画面 |
| `route.ts` | API を書く（2章の Route Handler） |

```
app/
├── layout.tsx          ← 全ページ共通のヘッダー・フッター
└── events/
    ├── layout.tsx      ← /events 配下だけの共通枠
    ├── loading.tsx
    └── [id]/page.tsx
```

フォルダを作っただけでは URL にならない。**`page.tsx` を置いて初めて URL になる。**

---

## 3-3. `components/` と `lib/` は `app/` の外に置く

実際の構成。

```
frontend/
├── app/                       ← ここだけが URL になる
│   ├── layout.tsx
│   ├── page.tsx
│   └── events/
│       ├── page.tsx
│       └── [id]/page.tsx
├── components/                ★ 部品置き場
│   ├── SessionBootstrap.tsx
│   └── VoteButton.tsx
└── lib/                       ★ 画面ではないコード
    ├── cookie.ts
    └── api.ts
```

### なぜ外に置くのか

**`app/` 配下のフォルダは URL のパスになるから。**

```
app/components/VoteButton.tsx
        ↑
   /components という URL ができてしまう
   （page.tsx がないので 404 だが、意図しない構造が生まれる）
```

App Router で特別な意味を持つのは **`app/` の中のファイルだけ**。
それ以外はどこに何を置いても自由。だから外に出す。

### `lib/` とは

**「画面ではない、再利用するコード」** を入れる場所として広く使われる名前。
Next.js の規約ではなく、慣習。

- `lib/api.ts` … fetch のラッパー
- `lib/cookie.ts` … `document.cookie` から値を取り出す関数

---

## 3-4. `components/` に何を置くか

```tsx
// 部品として切り出す
function EventCard({ title, status }: { title: string; status: string }) {
  return <div><h2>{title}</h2><span>{status}</span></div>;
}

// 使う側は map で並べる
{events.map((e) => (
  <EventCard key={e.id} title={e.title} status={e.status} />
))}
```

- **props** = 部品に渡す引数。親 → 子の一方通行
- **key** = React が「前回のどれと同じか」を対応付ける目印。`id` のような**安定した値**を使う

---

## 3-5. live-qa-app の構成

```
frontend/
├── app/
│   ├── layout.tsx
│   ├── page.tsx                        →  /
│   ├── join/[joinCode]/page.tsx        →  /join/ABC123   参加者の入口
│   ├── events/[eventId]/
│   │   ├── page.tsx                    →  /events/1      質問一覧
│   │   └── loading.tsx
│   └── dashboard/page.tsx              →  /dashboard     主催者用
├── components/
│   ├── QuestionForm.tsx
│   ├── QuestionList.tsx                ← STOMP 受信
│   ├── QuestionItem.tsx
│   └── LikeButton.tsx
└── lib/
    ├── api.ts
    └── cookie.ts                       ← CSRF トークン読み取り
```

---

# 4. Next.js の文法

## 4-1. データ取得の書き方が変わる

### 前回（React SPA）

```tsx
export function QuestionList() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/questions")
      .then((res) => res.json())
      .then((data) => setQuestions(data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>読み込み中...</p>;
  return <ul>{questions.map((q) => <li key={q.id}>{q.body}</li>)}</ul>;
}
```

### 今回（Next.js / Server Component）

```tsx
export default async function Page() {
  const res = await fetch("http://localhost:8080/api/questions");
  const questions: Question[] = await res.json();

  return <ul>{questions.map((q) => <li key={q.id}>{q.body}</li>)}</ul>;
}
```

**消えたもの**

- `useState` — サーバーで1回動いて HTML になるだけなので、状態を持つ必要がない
- `useEffect` — 「描画のあとに実行」する必要がない。**描画の前に取ってくる**
- `loading` — `await` で待ってから返すので、空の状態が存在しない

> **`fetch` と `json()` は両方 Promise を返す。`await` は必ず2回。**

---

## 4-2. キャッシュ

### 何が起きるか

Next.js の `fetch` は、素の `fetch` とは別物。**サーバー側の永続キャッシュ**が絡む。

```tsx
const res = await fetch("http://localhost:8080/api/questions");
```

この1行が「毎回取りに行く」のか「1回取ったきり」なのかは、**Next.js のバージョンで逆転している。**

| バージョン | デフォルト | 逆にするには |
|---|---|---|
| **16** | **キャッシュしない** | `cache: "force-cache"` |

### 今回の書き方

```tsx
// 質問リスト = 毎秒変わる → デフォルトのままでよい
const res = await fetch(`${API}/events/${id}/questions`);

// 明示したいなら書く（意図が読めるので推奨）
const res = await fetch(`${API}/events/${id}/questions`, { cache: "no-store" });

// イベントのタイトルなど、めったに変わらないもの
const res = await fetch(`${API}/events/${id}`, { cache: "force-cache" });
```

### 時間で切る

```tsx
const res = await fetch(url, { next: { revalidate: 10 } });
```

「10秒キャッシュする」だが、挙動に注意。

```
・10秒経過 → キャッシュは「期限切れ」になるが、まだ古い値のまま
・次のアクセス → その人には古い値を返す。同時に裏で新しい値を取りに行く
・さらに次のアクセス → ようやく新しい値が出る
```

**期限が切れた瞬間に新しくなるのではない。** 一度は古い値が返る。
質問リストのようにリアルタイム性が要るものには使えない。

### 使い分け

| データ | 設定 |
|---|---|
| 質問リスト・いいね数 | キャッシュしない |
| イベントのタイトル・説明 | `force-cache` でよい |
| 参加者数など、多少古くてもいいもの | `revalidate: 10` |

---

# 5. Server / Client Component

## 5-1. 見分け方は1行だけ

```
ファイルの先頭に "use client" が…

ない → Server Component（デフォルト）
       サーバーで1回動いて HTML を作る。それで終わり。
       → データ取得 / 静的な表示          ← 大半はこっち

ある → Client Component
       サーバーでも動くし、ブラウザにも送られて動き続ける。
       → useState / useEffect / onClick   ← 必要な所だけ
```

**デフォルトは Server。** 何も書かなければサーバーで動く。

---

## 5-3. `"use client"` は伝染する ← 核心

`"use client"` を書いたファイルから **import した先も、全部 Client になる。**

```
❌ import した場合

Panel.tsx ("use client")
  └ import EventInfo   ← Panel の一部として読み込まれる
                         → Panel が Client なので EventInfo も Client にされる
```

実際に、`EventInfo` に `"use client"` を書いていないのに Client 扱いになる現象を踏んだ。

### だから、ページの一番上に書いてはいけない

`page.tsx` の先頭に `"use client"` を付けると、2つのことが同時に起きる。

1. **`async` / `await` が使えなくなる** → データ取得ができない
2. **import している子コンポーネントが全部 Client になる** → SSR の意味が消える

```tsx
"use client";                          // ← これを書くと

export default async function Page() { // ← async にできない（エラー）
  const event = await fetchEvent(id);  // ← await できない
  ...
}
```

> **原則: `"use client"` は「押す・打つ・受け取る」末端のファイルにだけ書く。**

---

## 5-4. 実際の境界設計

### 画面の要素

```
┌─────────────────────────────────────┐
│ 新人研修Q&A                  [OPEN] │  ← ①イベント名 ②ステータス
├─────────────────────────────────────┤
│ 質問を投稿                          │
│ ┌─────────────────────────────────┐ │
│ │ (テキスト入力)                  │ │  ← ③入力欄
│ └─────────────────────────────────┘ │
│                          [ 送信 ]   │  ← ④送信ボタン
├─────────────────────────────────────┤
│ 質問一覧 (12件)              [並替] │  ← ⑤件数 ⑥並び替え
│ ┌─────────────────────────────────┐ │
│ │ Q. デプロイ手順は？             │ │  ← ⑦質問本文
│ │ 匿名 / 3分前          [👍 12]   │ │  ← ⑧投稿者・時刻 ⑨いいね
│ │ ✓ 回答済み                      │ │  ← ⑩回答済みバッジ
│ └─────────────────────────────────┘ │
│         ↑ STOMP で増える・並び替わる  │
└─────────────────────────────────────┘
```

### ファイルの割り当て

```
app/events/[eventId]/
├ page.tsx           S   データ取得 + ①② を描画
├ QuestionForm.tsx   C   ③④              ← "use client" を書く
├ QuestionList.tsx   C   ⑤⑥ + STOMP 受信 ← "use client" を書く
├ QuestionItem.tsx   C   ⑦⑧⑩            ← 書かない（伝染）
└ LikeButton.tsx     C   ⑨               ← 書かない（伝染）
```

`"use client"` を**明示的に書くのは2ファイルだけ**。
`QuestionItem` と `LikeButton` は伝染で Client になる。

> **取得は Server / 更新は Client。**
> 初期リストはサーバーで HTML に入るので初期表示が速い

---

## なぜ Client Component で `async`/`await` が使えないのか

Client Component は何度も実行されるから。

- **Server Component** → 1回だけ実行 → だから待てる
- **Client Component** → ボタンを押すたび、state が変わるたび何度も実行 → 待てない

---

# 7. CSRF

## 7-2. 誰が送ったリクエストでも Cookie は付く

### 攻撃の形

攻撃者のページに、これが仕込まれている。

```html
<form action="https://api.example.com/events/1/vote" method="post">
  <input type="hidden" name="option" value="1">
</form>
<script>document.forms[0].submit();</script>
```

ログイン中のユーザーがこのページを開くと、勝手に POST が送信される。
そして**ブラウザは律儀に `Cookie: SESSION=abc123` を付ける。**

```
API 側から見ると
  → 正規のセッション ID が付いた、完全に本物のリクエスト
  → 区別がつかない
  → ユーザーは投票したことにすら気づかない
```

これが **CSRF**（Cross-Site Request Forgery / クロスサイトリクエストフォージェリ）。

---

## 7-3. 「CORS があるのでは？」← 効かない

別オリジンへのアクセスはブラウザが制限する。しかし CSRF には効かない。

> **CORS が制限しているのは「レスポンスを読むこと」であって、「リクエストを送ること」ではない。**

```
攻撃者のページ ──POST──▶ api.example.com     ← 送信は成功する
                ◀╌╌✗╌╌╌                       ← レスポンスは読めない
```

攻撃者は結果を読めない。**しかし送信は成功している。**
投票・削除のような「送りつければ目的達成」の操作には無力。

---

## 7-4. 防ぎ方 — 合言葉を足す

問題は「**Cookie だけでは、送信元が正規のページか判断できない**」こと。
なら Cookie 以外の何かを足せばいい。

その何かの条件は、**正規のページには用意できて、攻撃者のページには用意できないもの。**

```
① サーバーがランダムな文字列（トークン）を発行し、XSRF-TOKEN という Cookie に入れる
② 正規ページの JS がその Cookie を読み、X-XSRF-TOKEN ヘッダーに入れて送る
③ サーバーは「Cookie の値」と「ヘッダーの値」が一致するか確認する
```

### なぜこれで防げるのか

**攻撃者のページの JS は、別ドメインの Cookie を読めない**（同一オリジンポリシー）。
読めなければヘッダーに入れられない。

```
Cookie   → 自動で付いてしまう      ← 攻撃者も利用できる
ヘッダー → 自分で付けるしかない    ← 攻撃者は付けられない
```

**ここに非対称性が生まれる。これが防御の本体。**

---

## 7-5. フロントの責務は3つだけ

```tsx
// 1. XSRF-TOKEN Cookie を読む
const token = getCookie("XSRF-TOKEN");

// 2. X-XSRF-TOKEN ヘッダーに入れる
// 3. credentials: 'include' を付ける
await fetch(`${API}/events/1/vote`, {
  method: "POST",
  headers: { "X-XSRF-TOKEN": token },
  credentials: "include",
});
```

---

## 図④

**タイトル**: CSRF — Cookie は付くが、ヘッダーは付けられない

```
【❌ 攻撃の流れ】

  攻撃者のサイト
   evil.com          ブラウザ                    Spring Boot
      │                 │                            │
      │──POST 送信──────▶│                            │
      │                 │──🍪 SESSION 自動で付く────▶│
      │                 │  ✗ X-XSRF-TOKEN 付けられない│
      │                 │    （別ドメインの Cookie を │
      │                 │      読めないから）         │
      │                 │◀────────── 403 ────────────│


【⭕️ 正規の流れ】

  自サイト
   myapp.com          ブラウザ                    Spring Boot
      │                 │                            │
      │─JS が Cookie を─▶│                            │
      │  読む            │──🍪 SESSION 自動で付く────▶│
      │                 │──🍪 X-XSRF-TOKEN 手で付ける▶│
      │                 │                            │
      │                 │◀────────── 200 ────────────│
```