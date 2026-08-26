# F-11 ポール回答・集計

scope: `POST /api/polls/{pollId}/responses`、`GET /api/polls/{pollId}/results`。作成・開閉は [f10-polls.md](f10-polls.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md)。 Issue: [#21](https://github.com/muscat2356/live-qa-app/issues/21)。

## 到達点（2026-08-26）

| 項目 | 状態 |
|------|------|
| `poll_responses` テーブル | **済み**（F-10 の V5。複合 FK 込み） |
| Entity `PollVote` / Repository | **済み** |
| `PollResponse.form` に count / myOptionIds | **済み** |
| 回答 POST | **済み**（Controller + Service + CSRF） |
| 結果 GET | **済み**（公開。所有者以外の DRAFT は 404） |
| 一覧 GET の実集計 | **未**（0 固定のまま。次の単位） |
| `POLL_UPDATED` | 口だけ。配信は C |

## 単位（この順）

1. ~~Entity `PollVote` + Repository~~
2. ~~`form(poll, counts, myOptionIds)` ＋ 回答 POST~~
3. ~~結果 GET（HOST 以外の DRAFT は 404）~~
4. 一覧 GET も同じ集計へ（0 固定をやめる）

Entity 名はテーブル `poll_responses`。DTO が既に `PollResponse` なので **`PollVote`**（`Vote.cast` に揃える）。

`form(poll, totalResponses, myOptionIds)` だけだと `options[].count` が足りない。`PollOptionResponse.form(option, count)` を Service が渡す。

## 契約（要約）

- ポール `OPEN` **かつ** ルーム `OPEN` だけ回答可。それ以外は 409
- `voter_key` はセッション。回答は未確立なら 401。**結果 GET は 401 にしない**
- `SINGLE` は 1 件。既存があれば **409**（置換しない）
- `MULTI` は 1〜選択肢数。同じ option の二重は 409
- JSON は F-10 と同じ PollDTO
- 結果 GET は所有者以外の `DRAFT` を 404（一覧と同じ見え方）

---

# サービス層のロジック（詳細）

## 全体の型の流れ

```text
Cookie(SESSION) ──▶ HttpSession ──┐
                                  ├─▶ VoterKeySources.form ─▶ VoterKeySource（sealed）
SecurityContext ──▶ OidcUser ─────┘         Host / Guest / Missing
                                                     │
                              VoterKeyResolver.resolve（401）／tryResolve（Optional）
                                                     ▼
                                              VoterKey（record）
                                                     │ .value()
                                                     ▼
                                          "g:{guestId}" / "u:{userId}"
                                                     ▼
                                       poll_responses.voter_key（DB）
```

| 型 | 正体 | 役割 |
|----|------|------|
| `VoterKeySource` | sealed interface | 「どこから来たか」。Controller が組み立てる |
| `VoterKey` | record（値オブジェクト） | 「誰か」。`g:` / `u:` の前置はここだけが知る |
| `PollVote` | Entity | `poll_responses` の 1 行。DTO の `PollResponse` とは別物 |
| `OptionVoteCount` | record（projection） | GROUP BY の受け皿。`Object[]` で受けない |

## `respond`（POST /api/polls/{pollId}/responses）

`@Transactional`（書き込み）。順番に意味がある。

| # | 行 | 何をする | 外れると |
|---|----|----------|----------|
| 1 | `voterKeyResolver.resolve(source)` | 誰かを確定。Missing はここで落とす | 401 |
| 2 | `requireVisiblePoll(pollId)` | ポールを復元 | 404 |
| 3 | `requireAccepting(poll)` | ポール `OPEN` **かつ** イベント `OPEN` | 409 |
| 4 | `resolveOptions(poll, optionIds)` | id → 当該ポールの `PollOption` へ変換 | 400 |
| 5 | `myOptionIds(pollId, key)` | 既に選んだ選択肢を **1 回の SQL** で取得 | — |
| 6 | `validateNotAlreadyAnswered(...)` | 二重を弾く（DB は触らない） | 409 |
| 7 | `saveAllAndFlush` | まとめて INSERT。UNIQUE は DB が最後の砦 | 409 |
| 8 | `PollResponse.form(...)` | 得票 + 自分の選択を載せて返す | — |

### なぜ 1 が先か

匿名セッションが無いのにポールの存在有無を返すと、情報が漏れる。**身元 → 存在 → 状態 → 入力** の順に落とす。

### 3 の「両方 OPEN」

ポールだけ OPEN でもルームが締切なら受け付けない（constraints）。`poll.getEvent().getStatus()` は LAZY なので、同一 TX の中で読む必要がある（`open-in-view: false`）。

### 4 の 400 は 4 種類

| 条件 | メッセージ |
|------|-----------|
| 同じ id が 2 回 | 選択肢が重複しています。 |
| SINGLE で件数 ≠ 1 | SINGLEは選択肢を1つ選んでください。 |
| MULTI で 1〜選択肢数の外 | 選択肢は1〜選択肢数にしてください。 |
| 他ポールの選択肢 id | 当該ポールの選択肢を選んでください。 |

最後の 1 つは DB の複合 FK（`fk_poll_responses_option_belongs`）でも守られるが、**400 として先に返す**ほうが親切。

### 6 の判定（DB を触らない）

```text
SINGLE … alreadyChosen が空でなければ 409（1ポール1票）
MULTI  … selected に alreadyChosen と被る id があれば 409
```

`static` にしてある。DB は呼び出し側が 5 で 1 回引くだけ。テストも `List<Long>` を渡すだけで書ける。

### 7 の二段構え

アプリの 6 で弾いても、**同時リクエストはすり抜ける**。UNIQUE `uq_poll_responses_option_voter` が最後の砦で、`DataIntegrityViolationException` を制約名で見分けて 409 に翻訳する（いいねの `VoteService` と同じ口）。制約名が違えば `throw ex` で 500 のままにする。**握りつぶさない。**

### 8 で引き直さない

保存した分は分かっているので、`alreadyChosen` + 今回分を `Stream.concat` で連結する。SELECT を 1 本節約している。

## `findResults`（GET /api/polls/{pollId}/results）

`@Transactional(readOnly = true)`。`options` / `event` が LAZY なので、`form` まで同じ TX の中で終わらせる（F-10 の一覧 GET で 500 になった件と同じ理由）。

| # | 行 | 何をする |
|---|----|----------|
| 1 | `requireVisiblePoll(pollId)` | 無ければ 404 |
| 2 | `DRAFT` なら `requireOwnedDraft` | 未ログイン／他人所有はどちらも 404 |
| 3 | `tryResolve(source)` | **401 にしない。** 未確立は空 |
| 4 | `PollResponse.form(...)` | 得票 + 自分の選択 |

### 見え方の表

| 見る人 | DRAFT | OPEN / CLOSED |
|--------|-------|----------------|
| 所有者 | 200 | 200 |
| それ以外（ゲスト・他 HOST） | **404** | 200 |

一覧 GET（F-10）はゲストに `OPEN` だけ見せるが、結果 GET は **`DRAFT` だけ**隠す。締切後の集計は公開してよい。ここを混ぜない。

### なぜ結果 GET は 401 にしないか

得票は公開情報。`myOptionIds`（自分が何を選んだか）だけがセッション依存で、無ければ空でよい。`resolve`（401）ではなく `tryResolve`（`Optional`）を使う。

## private の役割一覧

| メソッド | 役割 | DB |
|----------|------|-----|
| `requireVisiblePoll` | 存在確認（404） | SELECT 1 |
| `requireOwnedPoll` | 所有確認（404）。`id`+`host_id` で 0 件なら 404 | SELECT 1 + users |
| `requireOwnedDraft` | 未ログインを 404 にしてから所有確認 | 上に同じ |
| `requireAccepting` | 両方 OPEN（409） | なし（LAZY 読み） |
| `resolveOptions` | id → Entity。400 4 種 | なし |
| `validateNotAlreadyAnswered` | 二重判定。`static` | **なし** |
| `countByOptionId` | 選択肢ごとの得票 | GROUP BY 1 本 |
| `myOptionIds` | 自分が選んだ選択肢 id | SELECT 1 本 |
| `alreadyAnswered` | 409 の組み立て | なし |
| `isPollVoteUniqueViolation` | 制約名で 409 か判定 | なし |

## エラー一覧

| status | いつ | message |
|--------|------|---------|
| 400 | 選択肢の重複・件数・所属外 | 上の 4 種 |
| 401 | 回答で匿名セッション未確立 | 匿名セッションが未確立です。 |
| 404 | ポールが無い／所有者以外の DRAFT 結果 | ポールが見つかりません。 |
| 409 | ポールまたはルームが OPEN でない | このポールは現在回答を受け付けていません |
| 409 | 二重回答（アプリ判定 / UNIQUE 両方） | 既に回答済みです。 |

---

# 今回の修正（2026-08-26）

## `null` を渡さない

`tryResolve(...).orElse(null)` も、`formWithVotes(poll)` / `formWithVotes(poll, key)` のオーバーロードも採らなかった。

- オーバーロードは分岐を呼び出し側へ移すだけ。メソッドが増えて `if` は残る
- オーバーロードが効くのは既定値があるとき（`PollOptionResponse.form(option)` → `form(option, 0)`）。「キーが無い」に既定の `String` は無い
- 「識別が無い」＝「何も選んでいない」なので、`Optional<VoterKey>` が出たその場で `List.of()` に変換する

```java
final List<Long> myOptionIds = voterKeyResolver.tryResolve(source)
        .map(voterKey -> myOptionIds(pollId, voterKey.value()))
        .orElseGet(List::of);
```

いいねの `QuestionService.votedQuestionIds` と同じ口。

```text
【EJ】空コレクション … null ではなく List.of()（項目54）
【EJ】Optional … get/null に落とさずその場で意味のある値へ（項目55）
```

## N+1 を畳む

選択肢ごとに `countByPollOption_Id` / `existsByPollOption_IdAndVoterKey` を呼ぶと、**選択肢数ぶん SQL が飛ぶ**（一覧に載せるとポール数 × 選択肢数）。

| 用途 | 前 | 後 |
|------|----|----|
| 得票 | COUNT × 選択肢数 | `countByOptionId(pollId)` GROUP BY 1 本 |
| 二重判定 | exists 1 + exists × 選択肢数 | `findByPoll_IdAndVoterKey` 1 本を使い回す |
| 応答の `myOptionIds` | SELECT 1 本 | 上の結果 + 今回分を連結（0 本） |

GROUP BY は record で受ける。`Object[]` だと型が消える。

```java
@Query("""
        SELECT new com.liveqa.repository.projection.OptionVoteCount(
            v.pollOption.id, COUNT(v))
        FROM PollVote v
        WHERE v.poll.id = :pollId
        GROUP BY v.pollOption.id
        """)
List<OptionVoteCount> countByOptionId(@Param("pollId") Long pollId);
```

**0 票の選択肢は GROUP BY の結果に出てこない。** `PollResponse.form` の `getOrDefault(optionId, 0)` が埋めるので、SQL 側で空行を作らない。

消した finder: `existsByPoll_IdAndVoterKey` / `existsByPollOption_IdAndVoterKey` / `countByPollOption_Id` / `countByPoll_Id`。

```text
【EJ】ジェネリクス … Object[] ではなく record で受ける
【EJ】公開面最小化 … 使わなくなった finder は Repository から消す
【EJ】副作用を持たない … 判定は static。DB は呼び出し側が1回引く
```

## Security

`authenticated` より前に置く。順番が逆だと `/api/polls/**` 側に食われる。

```java
.requestMatchers("/api/polls/*/responses").permitAll()
.requestMatchers("/api/polls/*/results").permitAll()
```

GET は CSRF 不要。POST は必須（`X-XSRF-TOKEN`）。

## テスト

| クラス | 何を見るか |
|--------|-----------|
| `PollServiceTest` | SINGLE 200（count / myOptionIds）、二重 409、MULTI 選び直し 409、未確立 401、非 OPEN 409、UNIQUE 409、結果 GET 200 / DRAFT 404 / 所有者 DRAFT 200 / 無し 404 |
| `PollControllerTest` | 回答 200（Session → `VoterKeySource.Guest`）、CSRF なし 403、結果 200（未ログイン / ゲスト Session） |
| `PollVoteTest` | `cast` が option から poll を取る |

`findResults_OPENならキー未確立でも200` は `verify(never()).findByPoll_IdAndVoterKey` を入れてある。未確立で自分の票を引きに行かないことの確認。

`respond_SINGLEなら200` は `countByOptionId` が 31 番だけ返すスタブにしてある。**32 番が結果に出てこない**ので、0 埋めの動作をそのまま踏める。
