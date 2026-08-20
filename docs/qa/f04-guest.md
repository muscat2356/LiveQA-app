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
