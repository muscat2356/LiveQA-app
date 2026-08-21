# F-08 質問ステータス変更

scope: `PATCH /api/questions/{qid}/status`。`OPEN → ANSWERED → ARCHIVED`（`OPEN → ARCHIVED` 可）。`ARCHIVED` からは戻さない。STOMP は [F-07](../design/websocket-sequence.md)（#33）。UI は #42。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md)。所有者 404 の型は [f03-status.md](f03-status.md)。Issue: [#73](https://github.com/muscat2356/live-qa-app/issues/73)。

## 到達点

| 項目 | 状態 |
|------|------|
| `QuestionStatus.canTransitionTo` | **あり** |
| `PATCH /api/questions/{qid}/status` | **あり** |
| 所有者 SQL | `findByIdAndEvent_Host_Id`。0件は **404** |
| CSRF | あり→200、なし→403（`QuestionControllerStatusTest`） |
| STOMP `QUESTION_ANSWERED` | F-07。REST では出さない |

## 流れ

```text
HOST + SESSION + CSRF
  → PATCH /api/questions/{qid}/status { "status": "ANSWERED"|"ARCHIVED" }
  → @PreAuthorize HOST（非 HOST は 403。未ログインは 401）
  → oidc.sub → users.id
  → findByIdAndEvent_Host_Id(qid, hostId) → empty → 404（無し／他人を区別しない）
  → canTransitionTo 否 → 409
  → Question.changeStatus（同一 TX の managed なら save 省略可）
  → QuestionResponse（votedByMe は今の HOST セッション）
```

ルームが `CLOSED` でもホストはステータスを変えられる（受付の話ではない）。

## Q. 未ログインを 404 にするか？

**しない。F-03 と同じ。** Filter の `authenticated()` で 401。ROLE 不足は 403。SQL 0件だけ 404。#73 の「未ログインは 404」は存在漏洩防止の意図で、HTTP は F-03 に合わせる。

## Q. 所有者チェックを質問の `getEvent().getHost()` でやらない理由は？

**SQL 条件で絞る。** LAZY を判定だけで触らない。存在しない／他人所有を HTTP で区別しない。

```text
【EJ】enum … 遷移可否を QuestionStatus に閉じる
【EJ】公開面最小化 … 「自分のイベントの質問」だけ復元
【EJ】あえて外す … Entity.changeStatus は可変。STOMP は F-07
【EJ】不変性 … Request/Response は record
【EJ】依存性注入 … QuestionService に UserService
```
