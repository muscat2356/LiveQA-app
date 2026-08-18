# F-03 ルーム status 変更レビュー

## Q. F-03（PATCH /api/events/{id}/status）は docs と合っているか？

**結論: MVP として十分な実装。遷移・層・HTTP は openapi / constraints と整合。残課題は DB CHECK・文言の句点・テストの薄い枝。**

### なぜ作るか

ルーム作成（F-02）直後は `DRAFT`。主催者が開始・締切・再開できるようにするのが F-03。

### 役割と流れ

```
HOST + SESSION + CSRF
  → PATCH /api/events/{id}/status { "status": "OPEN"|"CLOSED" }
  → EventController（@PreAuthorize HOST, ResponseEntity）
  → EventService.updateStatus
       ① 要求が DRAFT → 400
       ② requireOwnedEvent（存在・所有者）→ 404 / 403
       ③ EventStatus.canTransitionTo → 否なら 409
       ④ Event.changeStatus + save（managed なら save 省略可）
  → EventStatusResponse { id, status }
```

Cookie / Redis / SecurityContext は F-01 と同じ（SESSION 復元 → OIDC principal → ROLE_HOST）。

### docs 照合

| 項目 | 正（docs） | 実装 | 判定 |
|------|------------|------|------|
| パス | `PATCH /api/events/{id}/status` | 同左 | OK |
| 遷移 | `DRAFT→OPEN`、`OPEN⇔CLOSED`、`DRAFT` へ戻さない | `canTransitionTo` | OK |
| Request | `OPEN` / `CLOSED` のみ | DRAFT 明示 400 | OK |
| 200 body | `{ id, status }` | `EventStatusResponse` | OK |
| 400 / 409 message | openapi 文言 | ほぼ同文＋末尾 `。` | 微差（アプリ既存方針） |
| 所有者のみ | constraints | `requireOwnedEvent` | OK |
| HOST | `@PreAuthorize` | あり | OK |
| DB CHECK | constraints 案 | Flyway に未追加 | 未（後続で可） |

### タイポ修正（今回）

`canTransitioinTo` → `canTransitionTo`（enum / Service / テスト）。

### よい点

- 遷移ルールを enum に閉じ、Service が HTTP に変換（層の分担がきれい）
- Controller は薄い・DTO 変換は Service
- テスト: 許可遷移3本・DRAFT要求400・DRAFT→CLOSED 409・Controller 200/403+CSRF

### 気になる点（必須ではない）

1. **同一 status への PATCH**（OPEN→OPEN）は `canTransitionTo` が false → **409**。冪等 200 にはしていない（仕様未記載なら現状で可）
2. **`Event.changeStatus` は遷移検証しない**（Service が正）。Entity 直呼びはテスト用のみ意識
3. **Service テスト不足**: 他ホスト 403・未存在 404・OPEN→DRAFT は enum テストで間接カバー
4. **Flyway CHECK**（`ck_events_status`）未。アプリが正なら MVP は問題なし
5. openapi は句点なし、実装は `。` 付き（F-02 と同じ方針なら docs 側を合わせるか現状維持）

### 【EJ】

```text
【EJ】enum … 遷移可否を EventStatus に閉じる（switch で網羅）
【EJ】公開面最小化 … Controller は status を渡し、遷移詳細は Service/enum
【EJ】あえて外す … Entity.changeStatus は可変フィールド更新のみ（JPA）
【EJ】不変性 … Request/Response は record
```
