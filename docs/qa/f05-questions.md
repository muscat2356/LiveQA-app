# F-05 質問投稿・いいね

scope: 質問投稿・いいね。一覧の本格 QueryDSL は F-06、ステータス変更は F-08、STOMP は F-07/C。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md)。guest は [f04-guest.md](f04-guest.md)。

## 全体像（会話で固まった形）

F-05 は「質問を載せる」と「いいねする」の REST。リアルタイム配信は F-07。人気順 QueryDSL は F-06。

識別は3段。混ぜない。

```text
HttpSession.getAttribute → Object
        ↓ GuestSession（ユーティリティ。Controller だけ）
   Optional<String> guestId        ← 戻りは Optional でよい
        ↓ Controller が OidcUser と合成
   VoterKeySource（Host / Guest / Missing）
        ↓ VoterKeyResolver
   VoterKey "g:…" / "u:…"
        ↓ いいねのときだけ
   votes 行 ＋ upvote_count ±1
```

- `GuestId` record は項目62（取り違え防止）。Optional 引数（項目55）は消えない。無いときは null か `Missing`
- 質問 POST は `votes` を触らない。`require` で 401 だけ
- ニックネーム空は DB に `"匿名"`、JSON も `"匿名"`（`null` は返さない）。ゲスト識別は `g:` であって display_name ではない
- 他人のイベントも質問は公開。所有者 SQL は使わない（無いとき 404）

## API

| API | 備考 |
|-----|------|
| `POST /api/events/{id}/questions` | OPEN のみ。guest/HOST。201 |
| `GET /api/events/{id}/questions` | 公開。`sort` は F-06（QueryDSL） |
| `POST /api/questions/{qid}/vote` | 1人1票。二重 409 |
| `DELETE /api/questions/{qid}/vote` | 取消 |

## 到達点

| 項目 | 状態 |
|------|------|
| Entity `Question` / `Vote` | あり |
| Flyway questions / votes | あり |
| F-04 `VoterKey` / Guest | あり |
| `VoterKeyResolver` | **あり**（`tryResolve` / `resolve` / `require`。引数は `VoterKeySource`） |
| `GuestSession` / `VoterKeySources` | **あり**（Session の Object 開け + Host/Guest/Missing 合成） |
| `VoteRepository` | **あり**（一覧で読む。いいね POST/DELETE でも INSERT/DELETE） |
| POST 質問 / Security 匿名可 | **あり**（`require` + `VoterKeySources.form`。件数 +1） |
| GET 一覧（`votedByMe`） | **本番あり**（キー無しは全部 false。`sort` は F-06、`status` は F-12 済） |
| GET 一覧テスト | **あり**（票あり true / キーなし false / 404 / Controller 200） |
| いいね POST/DELETE | **あり**（`VoteService` / `VoteController`。`upvote_count` は原子的 ±1。`add`/`remove` とも `@Transactional`。STOMP は F-07） |
| いいねテスト | **あり**（`VoteServiceTest` 正常/409/401/404/UNIQUE 409、`VoteControllerTest` 配線、`VoteControllerCsrfTest` あり→200／なし→403） |

`/api/events/**` が `authenticated()` なので、**その前**に `/api/events/*/questions` を `permitAll`。vote も同様に `/api/questions/*/vote` をその前へ。permitAll でも POST/DELETE は CSRF 必須。

## Q. `incrementQuestionCount()` + `save(event)` だと版が進むのはなぜ？

**`save` は Event 行の普通の UPDATE だから。** `@Version` は「この Entity を flush するたび」+1。件数加算もメモリ上の Event を dirty にして `save` するので、名称変更と同じ口になる。

```text
質問 POST
  → Event を SELECT（version=1）
  → questions へ INSERT
  → event.incrementQuestionCount()  … メモリの questionCount だけ +1
  → eventRepository.save(event)
      UPDATE events SET question_count=?, version=2 WHERE id=? AND version=1
```

同時に2人が投稿すると、どちらも version=1 で読む。片方だけ COMMIT。もう片方は 0 件 → 409。ライブ中の投稿が落ちる。版は名称×削除用なのに、件数まで巻き込んでいる。

いいねの `upvote_count` と同じく、件数は **JPQL の原子的 UPDATE**（`question_count = question_count + 1`）。バルク UPDATE は `@Version` を自動では進めない。名称 PATCH の `save(entity)` だけが版を進める。

【EJ】あえて外す … Entity.incrementQuestionCount + save は版と混ざる  
【EJ】例外 … 版の衝突は名称×削除の 409。投稿同士は件数の原子的 UPDATE で競合しない  

## Q. 質問の投稿時刻は画面に出すか？（#91）

**出す。** API の `createdAt` は既にある（openapi: ISO 8601 / UTC）。切らない。表示はフロント（Asia/Tokyo、日付と時まで。#59 / イベント一覧と同じヘルパ）。参加者 `QaScreen` と主催者 `HostQaScreen` は現状未使用。Issue: [#91](https://github.com/muscat2356/live-qa-app/issues/91)。

## 確認（いいね REST）

本番は契約どおり動く形。テスト前に直した点:

| 項目 | 結果 |
|------|------|
| `add` / `remove` の `@Transactional` | **必須。** `@Modifying` の ±1 と `deleteBy` は TX の外では動かない。`remove` にも付けた |
| Security | `/api/questions/*/vote` は `permitAll`（authenticated より前） |
| 二重 409 | メッセージは `"既に投票済みです。"`（openapi 例は句点なし。実害小） |
| Controller Javadoc | DELETE を「POSTメソッド」と書いていたので直した |

UNIQUE 違反は `saveAndFlush` ＋制約名で 409（#72 指摘）。exists は先回り。STOMP は F-07。

## 写経順

1. ~~F-04 取り込み~~ → ~~Resolver + QuestionRepository~~  
2. ~~POST 質問~~  
3. ~~入口整理~~（`GuestSession` + `VoterKeySource` + Resolver 3口 + `create` は `require`）  
4. ~~GET 一覧~~（本番 + テスト）  
5. ~~いいね POST/DELETE~~（`votes` INSERT/DELETE + 原子的 ±1。テスト込み）

STOMP 配信は F-07（Member C）。F-05 の REST は DB まで。

## Vote 地図（混乱しやすい4層）

いいねは **4つの別物**。混ぜると「質問 POST で Vote を書くのか」「resolve の戻りは？」が崩れる。

| 層 | 何か | 置き場 | いま |
|----|------|--------|------|
| 1. 識別子 | 「この SESSION は誰か」 | `VoterKey`（`g:{guestId}` / `u:{userId}`） | F-04 済み。組み立ては `VoterKeyResolver` |
| 2. 事実 | 「この人がこの質問に押した」 | テーブル `votes`（1行 = 1票） | Entity / Repository / `VoteService` あり |
| 3. 部屋の数字 | 「何人が押したか」（キャッシュ） | `questions.upvote_count` | 列あり。+1/−1 はいいね API で書く |
| 4. 自分の表示 | 「自分は押済みか」（ハート） | 応答の `votedByMe`。列ではない | 一覧・いいね応答で組み立て |

```text
SESSION (Redis: guestId または OIDC)
        ↓ 組み立て（DB に書かない）
   VoterKey  "g:abc" / "u:1"
        ↓
   ┌────┴────┐
   │         │
 GET一覧   いいね POST/DELETE
  読むだけ    votes に INSERT / DELETE
  votedByMe   + upvote_count ±1
```

**質問 POST は Vote ではない。** `questions` に本文を載せる。`votes` は触らない。`voter_key` 列も質問行に無い。呼ぶのは「匿名 SESSION が無いと 401」だけ。

フロントの guest JSON にある `voterKey` は表示・再計算用。いいね API はボディでキーを受け取らない（Cookie Session からサーバが算出）。

### API が触るもの

| API | VoterKey | `votes` | `upvote_count` | `votedByMe` |
|-----|----------|---------|----------------|-------------|
| `POST .../questions` | 検証だけ（戻り捨て／`require`） | 触らない | 触らない（0のまま） | 常に `false` |
| `GET .../questions` | 無くてよい（`tryResolve`） | **読む**（この人の票を一括） | 読む（列のまま） | 票があれば true |
| `POST .../vote` | **必須**（`resolve`） | **INSERT** | **+1** | `true` |
| `DELETE .../vote` | **必須**（`resolve`） | **DELETE** | **−1** | `false` |

GET は公開。キーが無くても 200。`votedByMe` は全部 false。いいねは未確立なら 401。ルーム非 OPEN は投稿・いいねとも 409（一覧は DRAFT/CLOSED でも返す）。

**暫定解消:** イベント物理削除なら質問行も CASCADE で消える。いいね対象が無ければ 404。`event.status != OPEN` の 409 は、ルームが残っているときだけ。

### 1人1票

UNIQUE `(question_id, voter_key)`。同じキーで二度 INSERT → **設計上は** 409「既に投票済みです」。アプリの `exists` は先回り。本丸は制約。

同時リクエストは `exists` をすり抜ける。本丸は UNIQUE。`saveAndFlush` して制約名 `uq_votes_question_voter` なら 409「既に投票済みです。」（#72）。詳細は下の Q。

`upvote_count` は votes 件数のキャッシュ。メモリで +1 して save しない。契約は `UPDATE questions SET upvote_count = upvote_count + 1`（取消は −1）。並行で数字がずれないため。

## Q. `incrementUpvoteCount` はなぜ `@Query` の UPDATE か？

**DB 上で `upvote_count = upvote_count + 1` するため。** Entity を `++` して `save` すると、読み取りと書き込みのあいだに別リクエストが入ると件数が欠ける。

```text
ダメ: find → メモリで 3→4 → save     （隣も 3 を読んで 4 を書く → 5 になるべきが 4）
よい: UPDATE ... SET upvote_count = upvote_count + 1 WHERE id = ?   （DB が足す）
```

| 注釈 | 役割 |
|------|------|
| `@Query` | JPQL。`Question` はクラス名、`upvoteCount` はフィールド名（列名ではない） |
| `@Modifying` | SELECT ではないと教える。これ無しだと実行時エラー |
| `clearAutomatically` | 実行後に Persistence Context を空ける。メモリ上の古い `upvoteCount` を残さない |
| `flushAutomatically` | 先に未 flush の変更を DB へ出す。同じ TX の INSERT vote と順序が狂わない |
| `@Param("id")` | JPQL の `:id` とメソッド引数を結ぶ |
| 戻り `int` | 更新した行数。0 ならその id の行が無い（または decrement で既に 0） |

`decrement` の `AND q.upvoteCount > 0` は CHECK（`upvote_count >= 0`）を踏まないため。`CURRENT_TIMESTAMP` は DB の今。Service から `Instant.now()` を渡さなくてよい。

呼び出す側は `@Transactional` 必須（`@Modifying` は TX の外では動かない）。STOMP はこのメソッドのあと（F-07）。

### Resolver の3口（分ける予定）

いま本番は `resolve` 一本。用途が3つあるので口を分ける。

| 口 | 戻り | 無かったら | 使う API |
|----|------|------------|----------|
| `tryResolve` | `Optional<VoterKey>` | empty（例外にしない） | GET 一覧 |
| `resolve` | `VoterKey` | 401 | いいね |
| `require` | `void` | 401 | 質問 POST |

組み立てロジックは `tryResolve` だけ。あとの2つは「empty を 401 にする」「値を公開しない」。

引数は `Optional<String> + OidcUser` のままにせず、**取得元を sealed の `VoterKeySource` に包む**（後述）。`HttpSession` は入れない。

### `tryResolve` の switch は何をしている？

**3状態を、例外にせず `Optional<VoterKey>` に変換する。** sealed なので `default` は不要。枝の足し忘れはコンパイルエラー。

| 枝 | 入力 | すること | 戻り |
|----|------|----------|------|
| `Host(subject)` | OIDC の `sub` | `users` を復元して `u:{id}` | `Optional.of` |
| `Guest(guestId)` | Session の id | 接頭辞 `g:{id}` | `Optional.of` |
| `Missing()` | どちらも無い | 何もしない | `Optional.empty` |

`Host` だけ `{ }` と `yield` なのは、文が2つ（復元してから返す）だから。`Guest` / `Missing` は式1本なので矢印の右に直書き。`if (oidc != null)` をやめる。

### コードの置き場（予定）

- `VoterKeyResolver` … 識別子だけ。votes を知らない  
- `QuestionService` … 投稿・一覧。一覧だけ `VoteRepository` を **読む**  
- `VoteService` … いいね POST/DELETE。`Vote.cast` + `increment`/`decrement`。`@Modifying` のため `@Transactional` 必須（`remove` も）  
- `VoteController` … `/api/questions/{qid}/vote`。ゲストも来るので Security は質問と同様 `permitAll`（CSRF は残す）  
- ポール `poll_responses` … **MVP-B**。同じ `voter_key` 文字列を使う別テーブル。今は触らない  

STOMP の `QUESTION_UPVOTED` には `votedByMe` を載せない（全員同じ配信。ハートは各クライアントの REST 結果）。

## 契約

- body: trim 後 1〜500（`@NotBlank` + `@Size`）
- request `nickname` → 列 `display_name`（任意 0〜50。空は `"匿名"` を格納）
- ルーム非 OPEN → 409「このルームは現在質問を受け付けていません」
- 投稿時 `events.question_count` を原子的 +1（同一 TX）
- 公開イベントなので所有者 SQL は使わない。無いとき 404
- `voter_key` は質問行には載せない（投稿時はセッション有無の検証）

## Q. `VoterKeyResolver` を `UserService` に統合しない理由は？

**統合しない。** UserService は「ローカル `users` ↔ OIDC」。`VoterKey` はゲスト Session も含む横断関心。

| | `UserService` | `VoterKeyResolver` |
|--|---------------|-------------------|
| 正の関心 | `users` 行の JIT / Me | 識別子 `g:` / `u:` |
| ゲスト | 扱わない | 主用途の一つ |
| 依存 | `UserRepository` | UserService（HOST のときだけ）＋ guestId |

## 現行（実装が正・2026-08）

空 nickname は `orElse("匿名")` で **INSERT**。DTO はそのまま返す。下の「NULL で持つ」「DTO で匿名」は検討ログ（採用しなかった枝）。

## Q. nickname の Optional チェーンは何をしている？

**現行は `orElse(ANONYMOUS_DISPLAY_NAME)`（`"匿名"` を格納）。** 下の `orElse(null)` 例は検討ログ。

```java
final String displayName = Optional.ofNullable(request.nickname())
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .orElse(null);
```

**任意ニックネームを「使える文字だけ残し、使えなければ null」に正規化する。** 未入力・空白だけは DB に空文字を載せない（constraints: 未入力は「匿名」表示。列は null）。

| 入力 | 結果 |
|------|------|
| `null`（キー無し／nickname 省略） | `null` |
| `""` / `"   "` | `null` |
| `" 花子 "` | `"花子"` |

`if` で同じことを書くと:

```java
String displayName = null;
if (request.nickname() != null) {
    final String trimmed = request.nickname().trim();
    if (!trimmed.isBlank()) {
        displayName = trimmed;
    }
}
```

Optional は「値が無い／捨てた」を型でつなぐ書き方。`orElse(null)` で箱を開けて `String` に戻す（Entity の `displayName` は null 可）。

## Q. JSON で null を出さず、キーごと消してよいか？

**JSON の省略はよい。Java / DB の「値なし」はこれまでどおり `null` でよい。** 混ぜない。

| 層 | 「ニックネームなし」 | 出してはいけないもの |
|----|----------------------|----------------------|
| DB `display_name` | SQL `NULL` | 空文字 `""`（「匿名」判定がブレる） |
| Java `String displayName` | `null`（任意列の定番） | Entity に `Optional<String>` は向かない |
| JSON | **キーごと無し**（おすすめ）または `"displayName": null`（いまの openapi 例） | `"displayName": ""` |

openapi 注記は「`null` なら UI は匿名」。キー省略だとフロントは `undefined`。`displayName ?? "匿名"` なら **null もキー無しも同じ**。`""` だと `??` は効かない。

DTO でキーを消す（Java の `null` はそのまま。`orElse(null)` は残してよい）:

```java
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionResponse(
        Long id,
        Long eventId,
        String body,
        String displayName,
        int upvoteCount,
        QuestionStatus status,
        boolean votedByMe,
        Instant createdAt
) {
}
```

## Q. null を出すのは EJ 的にどうか（今回）

**「呼び出し側が忘れうる戻り値の null」は避ける。今回の DB／JSON 境界の null は故意で、【EJ】あえて外す。**

EJ（第3版）が戒めるのは主に次です。

| 戒め | 今回 |
|------|------|
| コレクションを null で返さない（空リストを返す） | 対象外。`displayName` は 0〜1 個の文字列 |
| 値が無い戻りは `Optional`（Item 55） | Service 内部の正規化なら合う |
| `Optional` をフィールド／パラメータにしない | Entity / Request record の列には載せない |
| `orElse(null)` でまた null を配らない | **境界でだけ**使う |

今回の「なし」はバグではなく契約です（任意ニックネーム、未入力は匿名）。

```text
Request.nickname（省略可）
  → Optional チェーンで検証・trim     【EJ】Optional は戻り・中間
  → Question.displayName = null         【EJ】あえて外す … JPA 任意列 ↔ SQL NULL
  → JSON はキー省略（NON_NULL）         呼び出し側が null を触らなくて済む
```

JSON に `"displayName": null` を出すのは、JS 側に「null チェックしてね」と渡すことになり、EJ が嫌う形に近い。**キー省略**の方が「無い」が型として残らない。

空文字 `""` で null を逃げるのは EJ 的にもよくない。空は「値がある」。匿名と区別できなくなる。

Service を EJ に寄せるなら、開けないまま渡せるところまで Optional を持ち、Entity に渡す直前だけ `orElse(null)` です。

```java
private Optional<String> normalizeDisplayName(String nickname) {
    return Optional.ofNullable(nickname)
            .map(String::trim)
            .filter(s -> !s.isBlank());
}

// 格納の直前だけ開ける
Question.post(event, body, normalizeDisplayName(request.nickname()).orElse(null));
```

```text
【EJ】Optional … 「無いかもしれない」戻り・中間。フィールドにしない
【EJ】あえて外す … JPA Entity の任意 String は null（SQL NULL）
【EJ】公開面最小化 … JSON はキー省略し、クライアントに null を配らない
```

## Q. displayName の「匿名」はバックで確定してよいか？

**してよい（MVP）。適用は DTO 組み立てだけ。DB には書かない。**

フロントが名前を表示するだけなら、API の正を「常に表示用文字列」にすると分岐が消える。constraints の「未入力は『匿名』表示」を **Service の DTO 変換** に置く。

| 層 | 値 |
|----|-----|
| DB `display_name` | 未入力は **NULL のまま**（入力した名前だけ格納） |
| Entity | `null` 可 |
| `QuestionResponse.displayName` | **必ず非 null**。無ければ `"匿名"` |

```java
private static final String ANONYMOUS_DISPLAY_NAME = "匿名";

public QuestionResponse toQuestionResponse(Question question) {
    return new QuestionResponse(
            question.getId(),
            question.getEvent().getId(),
            question.getBody(),
            Optional.ofNullable(question.getDisplayName())
                    .filter(s -> !s.isBlank())
                    .orElse(ANONYMOUS_DISPLAY_NAME),
            question.getUpvoteCount(),
            question.getStatus(),
            false,
            question.getCreatedAt());
}
```

格納側はこれまでどおり `orElse(null)` → `Question.post`。**「匿名」を INSERT しない。** そうしないと「未入力」と「匿名と書いた人」が区別できず、あとで文言を変えられない。

openapi 追記案: `displayName` は常に文字列。未入力でも `"匿名"`。`null` は返さない。

注意: 一覧・STOMP も同じメソッドを使う（変換口を一つにする）。i18n が要る将来は表示文言をフロントへ戻す。

```text
【EJ】静的ファクトリ／変換 … DB の事実と表示用 DTO を分ける
【EJ】公開面最小化 … クライアントに null を配らない
【EJ】あえて外す … Entity は SQL NULL のまま
```

```text
【EJ】公開面最小化 … UserService の API 面を users に閉じる
【EJ】依存 … Resolver が UserService を使う（逆ではない）
【EJ】不変性 … Request/Response は record
## Q. create の trim〜save〜件数+1 は神メソッドか？

**神メソッドではない。** 1 ユースケース「質問を載せる」の格納手順が並んでいるだけ。

| する | まだ別関心ではない |
|------|-------------------|
| body trim | 正規化してから格納 |
| nickname → null | DB 用の「無し」 |
| `Question.post` + save | 永続化 |
| `incrementQuestionCount` | 同一 TX の副作用（契約どおり） |

神メソッドは「名前が付かないほど無関係な仕事が混ざる」もの。ここは全部「投稿を DB に書く」。404/OPEN 検証も同じ `create` に置いてよい（アプリケーションサービス）。

読みにくければ `normalizeDisplayNameForStore` に分けるのは可読性のため。クラスを割る理由にはならない。DTO の `"匿名"` は **返すとき** の変換で、このブロックには入れない。

## Q. `incrementQuestionCount` はメモリだけ増えて無意味では？

**同じ `@Transactional` の中で `findById` した Event なら、無意味ではない。** コミット時に JPA が変更を検知して `UPDATE events SET question_count = ...` を出す。`eventRepository.save(event)` は省略可（F-03 の `changeStatus` と同じ）。

```text
findById → Event が managed（Persistence Context の箱に載る）
  → incrementQuestionCount() … メモリの questionCount++
  → メソッド終了 → コミット直前の dirty checking
  → UPDATE events SET question_count=?, updated_at=? WHERE id=?
```

TX の外や、自分で `new Event()` しただけの行に呼ぶと、本当にメモリだけになる。

並行投稿で件数が欠ける可能性はある（read-modify-write）。厳密にするなら後で `UPDATE ... SET question_count = question_count + 1`。MVP の 1 TX 1 投稿では今の形で足りる。

## Q. DB は null にせず最初から「匿名」でよくないか？

**匿名ユーザーの絞り込みは `display_name` ではやらない。** ゲストは `voter_key` が `g:` かどうか。ニックネーム未入力とは別物。

MVP（一覧・いいね・status）に「ニックネーム無しだけ」のフィルタは無い。だから **NULL 必須ではない。** 未入力を `"匿名"` で INSERT して、DTO はそのまま返す、で動く。websocket-sequence にも `display_name=nickname or 匿名` とある。

| 残る差 | NULL で持つ | `"匿名"` で持つ |
|--------|-------------|-----------------|
| 「未入力」と「匿名と書いた」 | 区別できる | 区別できない（MVP では不要なことが多い） |
| 表示文言の変更 | DTO だけ変えられる | 既存行の UPDATE か、表示が古いまま |
| DTO | `orElse("匿名")` | 列をそのまま返す |

結論: **絞る瞬間が無いなら、DB を「匿名」に統一してよい。** その場合は格納も `orElse(ANONYMOUS_DISPLAY_NAME)`。DTO 側の二度変換は不要。列は NULL 可のままでも、アプリが常に文字列を書けば足りる（Flyway 既存 V1 は不改変）。

## Q. `resolve` の戻りは受け取らなくてよいか？／void にするか？

**投稿の呼び出し側では受け取らない。メソッドは void の `require` に分ける。** 組み立てロジックは一本（`tryResolve`）に残す。

| 口 | 戻り | 使うところ |
|----|------|------------|
| `tryResolve` | `Optional<VoterKey>` | GET 一覧（無ければ `votedByMe = false`、401 にしない） |
| `resolve` | `VoterKey` | いいね（キーを `votes` に載せる。無ければ 401） |
| `require` | `void` | 投稿（確立だけ検証。無ければ 401） |

`UserService.requireByOidcSubject` と同じ「無ければ例外」の名前。`require` の中で `resolve` を呼んで捨てるのは Resolver の内側だけ。`create` が戻りを捨てない。

## Q. 一覧の `votedQuestionIds` は切り分けるか？

**切り分ける。** Optional の `map` の中に Repository + stream が入ると、「無さ」と「票の ID 集合」が一段に混ざる。`findByEventId` は 404 → 票の ID → DTO、が読めるようにする。

## Q. `QuestionCreateResponse` を `QuestionResponse` にするか？

**する。** POST 201 と GET 一覧（このあと PATCH status も）が同じ形。`Create` は「作ったときだけ」に読める。OpenAPI の `QuestionDTO` に相当。いいね応答 `{ questionId, upvoteCount, votedByMe }` は別 record のまま。

Event 側は Create / Detail / Summary で形が違うので分けている。質問は形が同じなので 1 本。

## Q. VoterKey 取得を型安全なラッパーにするか？

**する。包むのは結果ではなく「取得元」。** `VoterKey` はすでに `g:` / `u:` 文字列のラッパー（JoinCode と同じ層）。足りないのは `Optional<String> guestId` と nullable `OidcUser` がバラで渡っている入口。

| 包む対象 | 採る？ | 理由 |
|----------|--------|------|
| 結果 `VoterKey` | すでにあり | compact で接頭辞検査は余力 |
| 取得元 `VoterKeySource` | **採る** | HOST / ゲスト / 欠落を型で分ける。Resolver の引数が1つになる |
| `HttpSession` を VO に入れる | **採らない** | Service が Servlet に依存する |
| Session の `Object` → String | **Controller の `GuestSession` ヘルパ** | 39–43 行の `instanceof`。`VoterKeySource` の一段手前 |

Java 21 の sealed。domain に Spring 型を入れない（HOST は `oidcSubject` の String）。

```java
public sealed interface VoterKeySource {
    record Host(String oidcSubject) implements VoterKeySource {}
    record Guest(String guestId) implements VoterKeySource {}
    record Missing() implements VoterKeySource {}
}
```

Controller が Session / principal を開けて `VoterKeySource` を組み立てる。Resolver は switch。`tryResolve` / `resolve` / `require` の引数は全部 `VoterKeySource`。

`record Hints(String guestId, OidcUser oidc)` だけだと null が2本残るので、型安全にはならない。

タイミング: Resolver 3口と **同じ単位**。GET 一覧の前。QuestionBody 級の後回しにはしない（引数の不揃いが3 API に複製されるため）。

## Q. `Host` の IAE は 500 に紛れないか？

**捕捉しなければ 500 になる。この単位では compact の IAE を書かない。** `ExceptionHandler(Exception)` が未処理を 500 にする。`IllegalArgumentException` 専用ハンドラは付けない（JoinCode 以外の IAE まで 400 になる）。

| 空の意味 | 例外 | HTTP |
|----------|------|------|
| Session / principal が無い（業務） | `Missing` → `require` の `BusinessException` | **401** |
| 参加コードが不正（ユーザ入力） | JoinCode の IAE を Service が catch → `BusinessException` | **400** |
| `new Host("")`（呼び出し側のバグ） | IAE が外へ | **500**（紛れではなく、バグの印） |

`VoterKeySource.from` は空 subject を Host にしない（Guest か Missing）。HTTP の入口は `from` だけ使う。compact 検査は JoinCode 級（ユーザ入力の形）になってから。

## Q. `readGuestId().orElse(null)` は気持ち悪くないか？

**その感覚でよい。使わない。** Optional の戻りをすぐ null に戻すと、項目55 の箱が無駄になる。`VoterKeySources` で Optional のまま `map` / `orElseGet` して `VoterKeySource` にする。String の null を `form(String, String)` に渡す口は HTTP 経路では不要。

`map(Guest::new).orElseGet(Missing::new)` は、map の推論が `Optional<Guest>` になり Missing と合わない。`.<VoterKeySource>map(...)` が要る。ファイル名 `VoterKeySources.java` の public クラスは `VoterKeySources`（domain の `VoterKeySource` と別物）。

## Q. QuestionBody VO と `Response.from(Entity)` は取るべきか？（EJ 指摘）

**精神は賛成。このリポの正では、DTO 変換は Service に残す。値オブジェクトは JoinCode 級の規則が固まってから。**

| 指摘 | EJ | このリポ |
|------|-----|----------|
| `QuestionBody` record | 項目62 文字列回避、49 生成時検査 | **JoinCode と同じ型**。body はすでに `@Valid`。今足すと検査が二重＋ VO の IAE を HTTP に変換する口が要る |
| `QuestionResponse.from(Question)` | 項目1 静的ファクトリ | **採らない。** 規約は「DTO 組み立ては Service」。`dto` が `domain.Question` に依存する |
| `question.toResponse()`（Entity 側） | domain に寄せる | **採らない。** Entity は API 直出ししない。[java.md](java.md) |
| Service はオーケストレーション | 公開面最小化 | 今の `create` で足りる。`toQuestionResponse` は EventService と同じ置き場 |

項目1 の静的ファクトリは、すでに **Entity / 値オブジェクト** 側にある（`Question.post` / `User.hostOf` / `JoinCode.generate`）。Response の `from(Entity)` は別レイヤをまたぐ。

今の単位: `toQuestionResponse` を **private** にする程度でよい。`QuestionBody` は F-05 を閉じてから、JoinCode に倣うなら検討。

## Q. `exists` 通過後の同時いいねは 409 になるか？

**`save()` だけだとならない。** flush/commit まで遅れると catch-all で 500。`saveAndFlush` して制約名を 409 に変換する（#72）。

### 流れ（競合・修復後）

```text
リクエスト A                         リクエスト B
exists → false                       exists → false
saveAndFlush OK                      saveAndFlush → uq_votes_question_voter
                                     → DataIntegrityViolationException
                                     → 制約名一致 → BusinessException 409
```

| 層 | 役割 |
|----|------|
| `exists` | 先回り（よくある二重を例外にしない） |
| DB UNIQUE | 1人1票の本丸 |
| Service catch | 制約名 `uq_votes_question_voter` → 409。他は再 throw（500） |

Advice 共通（ポール用）は今は足さない。いいね専用の最小差分。

## Q. `votedByMe` の意味は？

**「今このリクエストを送った人（この SESSION の `voter_key`）が、その質問にすでにいいねしているか」。** いいね総数 `upvoteCount` とは別。

| フィールド | 誰の視点 | 例 |
|------------|----------|-----|
| `upvoteCount` | 部屋全体 | 8 人がいいねした |
| `votedByMe` | **自分だけ** | 自分は押済み → `true`。ハート塗りつぶし用 |

同じ質問でも、A さんの一覧では `true`、B さんでは `false` になりうる。セッションが無い閲覧は全部 `false`（401 にしない）。投稿直後はまだ投票していないので常に `false`。STOMP 配信には載せない（各クライアントが自分の状態で出す）。
