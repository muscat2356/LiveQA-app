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
| `PollResponse.form` | **済み**（count / myOptionIds は F-11 まで 0） |
| Service / Controller テスト | **済み**（create / GET / PATCH） |
| GET 一覧 | **済み**（所有者=全件、未ログイン=OPEN、404。permitAll） |
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

**trim の防御的コピーはよい。重複の IAE は Request に置かない。** `JoinCode` は Service が catch する。Request は Jackson が `new` するので IAE は `HttpMessageNotReadableException` → 今の Handler では **500**。要素 `null` で `String::trim` すると NPE も 500。

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
