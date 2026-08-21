# F-04 匿名セッション / voter_key

scope: 参加ページは A 主。B は **guest + `voter_key`**。Join 照会は F-02。質問・いいねは [f05-questions.md](f05-questions.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md)。  
ラボ時代の長い素振り: [07-guest-session.md](07-guest-session.md)。

## 到達点

| 項目 | 状態 |
|------|------|
| `POST /api/auth/guest` | あり（Service + `GuestResponse`） |
| Security `permitAll`（guest。CSRF は必須） | あり |
| `VoterKey`（`g:` / `u:`） | あり。HOST の `u:` 利用は F-05 |
| CSRF テスト | `GuestSessionControllerCsrfTest` |
| 質問・いいね | F-05 |

## 正しい形

```text
Controller
  → source = VoterKeySources.fromGuestSession(session)  // Guest / Missing（form は使わない）
  → response = service.issueOrReuse(source)             // 業務（HttpSession は渡さない）
  → session.setAttribute("guestId", response.guestId())
  → ResponseEntity.ok(response)
```

| 層 | F-04 | しない |
|----|------|--------|
| Controller | get/setAttribute、`ResponseEntity` | `VoterKey` 組み立て本体 |
| Service | 既存 id → 発行／再利用、`GuestResponse` | `HttpSession` / DB |
| Repository | **作らない** | guests 表は ER に無い。正は Session→Redis |

## Q. `getAttribute` の Object → String はラッパーにするか？

**する。これは `VoterKeySource` とは別の一段。** Servlet の `getAttribute` は常に `Object`。型安全にする境界は Controller だけ。`QuestionController` が `GuestSessionController.ATTR_GUEST_ID` を見るのは依存の向きが逆。

同じ `instanceof` が guest 発行・質問・一覧・いいねに複製される。定数と読み書きを `GuestSession`（名前は任意）に閉じる。

| 層 | 型 | Servlet |
|----|-----|---------|
| Session 属性 | `GuestSession.readGuestId` → `Optional<String>` | **ここだけ** |
| 投票者の状態 | `VoterKeySource`（Host / Guest / Missing） | 知らない |
| 投票キー | `VoterKey`（`g:` / `u:`） | 知らない |

`GuestId` record で `"abc"` を包むのは余力。いまの不安全性は `Object` なので、戻りを `Optional<String>` にするヘルパで足りる（【EJ】項目55: 戻りは Optional でよい）。

## Q. `GuestSession` は `static class` にしなくてよいか？

**トップレベルは `static` を付けられない。** Java の `static` クラスは **ネストしたクラス** だけ。`public static final class GuestSession` はコンパイルエラー。

「静的に使う」は次で表す。

| 書くもの | 意味 |
|----------|------|
| `final class` | 継承しない（ユーティリティ） |
| `private GuestSession() {}` | `new` させない |
| `public static Optional<String> readGuestId(...)` | インスタンス無しで呼ぶ |

【EJ】項目4 … トップレベル + private コンストラクタ。クラスに `static` は付けない（ネスト専用）

## Q. Service が HttpSession に触ってよいか？

**触らない。** Servlet API は Controller。Service が `ResponseEntity` を持たないのと同系統。引数は `VoterKeySource`（`fromGuestSession` の Guest / Missing）。`Optional` は戻り専用。詳細は [java.md](java.md) の Optional（項目55）。

## Q. GuestSessionControllerTest は CSRF を見ていない？

**意図的。** `addFilters=false` は配線スライス。契約上の CSRF は Filter（`GuestSessionControllerCsrfTest` または Bruno）で見る。

## 流れ

```text
POST /api/auth/guest + X-XSRF-TOKEN
  → Session に guestId が無ければ UUID 発行
  → あれば再利用
  → voter_key = g:{guestId}
  → SESSION Cookie に載る（Redis）
```

```text
【EJ】公開面最小化 … Servlet API は Controller まで
【EJ】不変性 … GuestResponse / VoterKey は record
【EJ】静的ファクトリ … VoterKey.guest
【EJ】あえて外す … Service に HttpSession を渡さない／Repository なし
```

## Q. GuestSessionService の引数は Optional ではなく VoterKeySource？

**Guest / Missing ならそうする。`form` は使わない。**

| 組み立て | 用途 |
|----------|------|
| `VoterKeySources.form(oidc, session)` | 投票者は誰か（Host 優先）。質問・いいね・一覧 |
| `VoterKeySources.fromGuestSession(session)` | 匿名 guestId の有無だけ。`POST /api/auth/guest` |

`issueOrReuse` は `switch`。`Guest` 再利用 / `Missing` 新規 UUID / `Host` は 400（`form` を繋いだときに壊れる）。Session の書き込みは今どおり Controller。

## Q. 匿名セッションが発行されずに質問投稿できない原因は？

**再確認: 呼び出しヘルパはあるが、URL誤り＋未使用で実質発行されていない。**

### 設計上の流れ（screen-transition）

```text
参加ページ → 匿名セッション発行 →（任意）ニックネーム → Q&A → 質問投稿
```

### 現状フロント（再確認）

| 場所 | 状態 |
|------|------|
| `lib/api/guestSession.ts` の `getGuestSession` | **ある**。`POST /api/guest` を叩く |
| backend 契約 | **`POST /api/auth/guest`**（`GuestSessionController`） |
| `useGuestQuestions` | `getGuestSession` を **import のみ**で **未呼び出し** |
| `PostForm` / `VoteButton` | guest 発行を待たず questions / vote を直叩き |
| `enterRoom` | 引き続き `qa_nickname` のみ |

ニックネーム Cookie（`qa_nickname`）は **表示名用**。Spring Session の `guestId` / `voter_key` とは別物。

### 二重の原因

1. **パス不一致** … フロント `/api/guest` → rewrite 後 `backend /api/guest`（存在しない → 404）。正は `/api/auth/guest`
2. **未呼び出し** … パスを直しても、hook / PostForm が `await getGuestSession()` しない限り Session に `guestId` が載らない

### 投稿時に起きること

```text
PostForm → POST questions（guest 未発行）
  → VoterKeySources.form → Missing
  → require → 401
```

### 直し方

```text
1. fetch URL を "/api/auth/guest" に直す
2. Q&A 初期化または投稿・いいねの直前に await getGuestSession()
```

Network で `POST .../api/auth/guest` が 200 になり、続く questions に同じ SESSION が付くかを見る。

## Q. PR #88（guest パス修正 + guestLoad 先頭で発行）は問題ないか？

**本丸の2点は正しい。マージしてよい水準。** 残るのは CSRF 初回と失敗キャッシュの Should。

| 変更 | 判定 |
|------|------|
| `/api/guest` → `/api/auth/guest` | **OK**（backend / openapi と一致） |
| `guestLoad` 先頭で `await getGuestSession()` | **OK**（一覧前に Session 属性を載せる） |

### 残リスク（止めないが知っておく）

1. **初回 CSRF** … Q&A 直着で Cookie が無いと、guest POST が 403 になり一覧も落ちる。必要なら guest の前に軽い GET、または 403 時に再試行
2. **失敗 Promise の固定** … `guestId` に一度 reject が入るとリロードまで再試行しない。失敗時は `guestId = null` に戻すとよい
3. **PostForm 直叩き** … マウントで guestLoad が先に走れば通常は足りる。即投稿レースは稀

関連: GitHub #86 / PR #88

