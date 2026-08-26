# F-10 ポール作成・開閉

scope: `POST/GET /api/events/{id}/polls`、`PATCH /api/polls/{pollId}/status`。回答・集計は [f11-polls.md](f11-polls.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [er-diagram](../design/er-diagram.md) / [constraints](../design/constraints.md)。 Issue: [#74](https://github.com/muscat2356/live-qa-app/issues/74)。

## 到達点（2026-08-25 確認）

`back/feature/10` は #100（物理削除）を取り込み済み。`EventRepository.findByIdAndHost_Id` あり。

| 項目 | 状態 |
|------|------|
| `PollType` / Flyway `V5__polls.sql` | **済み** |
| `Poll` / `PollOption` / `Poll.create` | **済み** |
| `PollRepository` finder 3本 | **済み** |
| `PollStatus.canTransitionTo` | **済み**（CLOSED は終端） |
| `PollService.create` + `requireOwnedEvent` + `normalizeLabels` | **済み**（trim 後重複は Service 400） |
| `POST /api/events/{id}/polls` | **済み**（HOST / `@Valid` / 201） |
| `PollResponse.form` | **済み**（count / myOptionIds は一覧・結果で実集計。作成は 0） |
| Service / Controller テスト | **済み**（create / GET / PATCH / 不正 enum 400） |
| GET 一覧 | **済み**（所有者=全件、未ログイン=OPEN。`@Transactional(readOnly = true)` で `form` まで同一 TX） |
| PATCH 開閉 | **済み**（DRAFT→OPEN / DRAFT送り400 / CLOSED→OPEN 409 / 404。HOST・CSRF）
| STOMP `POLL_*` | 口だけ残す。配信は C |

## 単位（この順）

1. ~~V5 + `PollType`~~
2. ~~Entity / Repository~~
3. ~~`POST` 作成 201~~
4. ~~`GET` 一覧~~
5. ~~`PATCH /status`~~

F-11（回答 POST / 結果 GET）は混ぜない。`poll_responses` テーブルだけ V5 で先に作る（#74）。

## ルームとの違い

| | ルーム | ポール |
|--|--------|--------|
| 往復 | `OPEN ⇔ CLOSED` | `CLOSED` は終端。再開しない |
| 削除 | 物理 DELETE（CASCADE） | ポール単独の DELETE API は持たない |

イベントが無ければ作成・一覧は質問と同じく 404。

## Q. Request の compact で trim・重複するか？

**trim の防御的コピーはよい。重複の IAE は Request に置かない。** `JoinCode` は Service が catch する。Request は Jackson が `new` するので IAE は `HttpMessageNotReadableException`（ハンドラ追加後は 400）。要素 `null` で `String::trim` すると NPE も同じ。重複判定は Service のまま。

`JoinCode` は Service が `form` するので IAE を catch できる。`PollCreateRequest` は Jackson が `new` するので、Service の try-catch には届かない。null 要素は `@NotBlank`（400 + fields）。compact の IAE は付けない。

```text
【EJ】不変性 … compact で trim 済みリストに差し替えは沿う（項目50）
【EJ】あえて外す … Request で IAE。stream 必須ではない（Poll.create と同じ for）
```

```text
【EJ】enum … 遷移は canTransitionTo。CLOSED は終端
【EJ】静的ファクトリ … Poll.create。DTO 変換は載せない
【EJ】あえて外す … 配信は C。今は口だけ
```

## Q. 存在しない enum が 500？（#107 指摘）

**指摘どおり。`@Valid` より前に Jackson が落とす。** `status: "INVALID"` / `type: "INVALID"` は Bean Validation に届かず `HttpMessageNotReadableException`。`ExceptionHandler(Exception)` が未処理を **500** にする。

入力不正なので **400**。ハンドラは `GlobalExceptionHandler`（ポール専用にしない。Event の PATCH も同じ穴）。壊れた JSON も同じ例外なのでまとめて 400。

`PollControllerTest` は本番 Handler を Import していなかった。400 の JSON を見るなら `EventControllerTest` と同じく `@Import(GlobalExceptionHandler.class)`。

```text
【EJ】例外 … 読めない JSON はクライアント誤り。catch-all の 500 に混ぜない
【EJ】公開面最小化 … Controller に enum 判定を書かない。Advice が箱を返す
```

## Q. poll_responses の FK が分かれている？（#107 指摘）

**分かれている。別ポールの選択肢 ID を組み合わせた行も、独立 FK だけだと DB は通す。** F-11 の Service でも弾くが、不変条件は DB が最後の砦（constraints の 3層）。

V5 は未マージなので **V5 を直す**（V6 は足さない）。`poll_options(id, poll_id)` の UNIQUE を複合 FK の参照先にする。

```text
【EJ】あえて外す … 回答 API は F-11。所属の不変だけ V5 で先に閉じる
```

ローカルで V5 を既に当てているなら、マイグレーション履歴が食い違う。開発 DB は `docker compose down -v` で作り直す。

## Q. GET 一覧は本番で 500 にならないか？（2026-08-25 バグチェック）

**当時はなる可能性が高かった。`findByEventId` に `@Transactional(readOnly = true)` を付けて直した。** `open-in-view: false`。Repository の `find` が終わると Session が閉じる。`PollResponse.form` が `poll.getOptions()`（LAZY）を読む。質問一覧は `eventId` を引数で渡すので LAZY を触らない。

```text
【EJ】例外 … OSIV を切っているので、LAZY を読む参照は TX 内
【EJ】あえて外す … 質問一覧は TX なしでよい（関連を触らない）
```
