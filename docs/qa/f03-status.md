# F-03 ルーム開閉

scope: `PATCH /api/events/{id}/status`。`DRAFT → OPEN ⇔ CLOSED`。`DRAFT` へは戻さない。論理削除 `DELETED` は [f02-events.md](f02-events.md) の `DELETE`（この PATCH には載せない）。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md)。

## 到達点

| 項目 | 状態 |
|------|------|
| 遷移 `canTransitionTo` | あり（タイポ `canTransitioinTo` は修正済み） |
| 要求 DRAFT | 400 |
| 不正遷移 | 409 |
| 所有者 | SQL `findByIdAndHost_Id`。0件は **404**（存在漏洩しない） |
| Flyway CHECK `ck_events_status` | 未（アプリが正なら MVP 可） |

## 流れ

```text
HOST + SESSION + CSRF
  → PATCH /api/events/{id}/status { "status": "OPEN"|"CLOSED" }
  → 要求 DRAFT → 400
  → findByIdAndHost_Id → empty → 404
  → canTransitionTo 否 → 409
  → Event.changeStatus（同一 TX の managed なら save 省略可）
  → EventStatusResponse { id, status }
```

同一 status への PATCH（OPEN→OPEN）は現状 409。冪等 200 にはしていない。

## Q. 所有者チェックを Java の if で 403 にしない理由は？

**SQL 条件（id + host_id）で絞り、0件なら 404 一本。** `getHost()` の LAZY を判定だけで触らない。存在しない／他人を HTTP で区別しない。403 は ROLE / CSRF。

## Q. `changeStatus` のあと `save` は要るか？

**同じ TX で managed なら省略可。** Persistence Context が dirty checking し、コミット時に UPDATE する。新規（transient）だけ `save` が必要。明示 `save` は読み手向けで害は少ない。詳細は [java.md](java.md#jpa管理下の-save)。

```text
【EJ】enum … 遷移可否を EventStatus に閉じる
【EJ】公開面最小化 … 「自分のイベント」だけ復元
【EJ】あえて外す … Entity.changeStatus は可変。他人と無しを 404 一本
【EJ】不変性 … Request/Response は record
```
