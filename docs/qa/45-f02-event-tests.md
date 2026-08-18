# F-02 Event テスト解説（一覧・作成）

## 質問

今回のテスト（EventService / EventController）は何を検証しているか。

## なぜテストを分けるか

| 層 | ファイル | 見るもの | 見ないもの |
|----|----------|----------|------------|
| Service | `EventServiceTest` | User 復元・DB 呼び出し・DTO 変換・joinCode 衝突 | HTTP・Cookie・CSRF |
| Controller | `EventControllerTest` | `@PreAuthorize`・ステータス・JSON 形・POST の CSRF | 本物の Service 業務 |

本番 `SecurityConfig` は Import しない（`MeControllerTest` と同じ）。

## EventServiceTest（いま書いている一覧）

### 役割

```text
受け取り: 偽 OidcUser（sub）
  → 処理: requireByOidcSubject で User 復元 → Repository で Event 一覧
  → 返し: EventSummaryResponse の List
```

### データの流れ（テスト内）

```text
User.hostOf("sub-1") + id=1 を Reflection で付与
  → when(requireByOidcSubject("sub-1")) → その User
  → Event.create(...) + id 付与
  → when(findByHost_IdOrderByCreatedAtDesc(1L)) → List.of(event)
  → eventService.findAll(oidc)
  → assert: 件数・id・title・joinCode・status
```

`ReflectionTestUtils.setField` は「JPA が付ける id をテストで先に書く」ための手段。ファクトリ引数ではない。

### 型の正体

| 型 | 正体 |
|----|------|
| `@Mock` | 偽 Repository / UserService |
| `@InjectMocks` | 本物 `EventService`（偽依存を注入） |
| `OidcUser` | セッション内 principal の契約（interface） |
| `EventSummaryResponse` | 不変 DTO（record） |

### 【EJ】

```text
【EJ】依存性注入 … @InjectMocks がコンストラクタ注入を再現
【EJ】静的ファクトリ … User.hostOf / Event.create
【EJ】不変性 … Response は record
【EJ】あえて外す … Entity の id は Reflection で可変に見せる（JPA 前提）
```

## セットで足す分岐（Service）

| テスト | 検証 |
|--------|------|
| `save_...` | 作成 → `EventCreateResponse`（joinUrl・DRAFT） |
| `generateJoinCode_衝突...` | `exists` true→false で再発行 |
| `generateJoinCode_上限...` | ずっと true → `BusinessException` Conflict |

## EventControllerTest（HTTP 面）

```text
oidcLogin(ROLE_HOST) ± csrf(POST)
  → @PreAuthorize
  → 偽 EventService の戻りを JSON で検証
```

| ケース | 期待 |
|--------|------|
| GET HOST | 200・一覧 JSON・`questionCount` 無し |
| GET 非 HOST | 403 |
| POST HOST + csrf | 201 |
| POST 非 HOST + csrf | 403 |

## 一文

> Service は「復元→一覧/作成→DTO」、Controller は「誰が呼んでよいか＋HTTP 形」。DB も Keycloak も使わない。
