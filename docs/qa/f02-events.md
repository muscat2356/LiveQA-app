# F-02 ルーム作成・一覧・Join 照会

scope: `POST/GET /api/events`、`GET /api/events/{id}`、`GET /api/join/{code}`。開閉は [f03-status.md](f03-status.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md) / [er-diagram](../design/er-diagram.md)。

## 到達点

| API | 状態 |
|-----|------|
| `POST /api/events` | HOST。201。title 1〜100、join_code 8文字 UNIQUE |
| `GET /api/events` | 自分の一覧。`questionCount` **なし** |
| `GET /api/events/{id}` | 所有者のみ。他人・無しは **404**。`questionCount` あり |
| `GET /api/join/{code}` | 公開。紹介 DTO |

## データの流れ（作成）

```text
POST /api/events + SESSION + X-XSRF-TOKEN + { "title": "..." }
  → HOST 検証
  → requireByOidcSubject → users.id
  → JoinCode 発行（衝突なら再試行、上限で 409）
  → Event.create → save
  → EventCreateResponse 201
```

一覧: `findByHost_IdOrderByCreatedAtDesc` → `EventSummaryResponse[]`。  
Join: 形式不正 400、無し 404。

## questionCount

| API | 件数 |
|-----|------|
| 一覧 | 載せない |
| 詳細 | `events.question_count`（スナップショット列） |
| 更新 | 質問 POST（F-05）で +1。応答組み立てに COUNT クエリは使わない |

## 設計判断（残したもの）

| 論点 | 結論 |
|------|------|
| joinCode 生成 | for + StringBuilder。ループ内 `+` は NG。Stream 必須ではない |
| title 検証 | Controller `@Valid`。Service は trim だけ寄せてよい |
| 作成単位の 404 | 不要（id を受け取らない）。後続 GET/PATCH で `BusinessException(NOT_FOUND)` |
| 手動バリデーション | `MethodArgumentNotValidException` に無理統一しない。手チェックは `BusinessException`。統一するのは JSON 形 |
| Request record | Bean Validation。compact ctor で `BusinessException` を投げない。値オブジェクト（JoinCode）は別 |
| `save` 失敗 | 予防できる UNIQUE は先に exists。業務の競合（いいね）は制約名で 409。想定外は 500 |

## POST が通らないとき

最低ヘッダ: `Cookie: SESSION` + `X-XSRF-TOKEN`（Cookie と同じ値）+ `Content-Type: application/json`。GET `/api/me` は CSRF 不要。

| 結果 | 多い原因 |
|------|----------|
| 403 CSRF なし | ヘッダ欠落。message「CSRFトークンがありません」 |
| 403 CSRF 不一致 | 値の食い違い。取り直し |
| 403 権限 | 非 HOST |
| 500 JoinCode | 正規表現 typo（`[A-Za-z0-9]{8}`） |
| 起動失敗 `EventService` Bean 無し | `@Service` 未付与 |

## テスト

| 層 | 検証 |
|----|------|
| `EventServiceTest` | 一覧 DTO、作成、joinCode 再発行/上限、詳細 404、Join 400/404 |
| `EventControllerTest` | HOST 200/201、非 HOST 403、POST は csrf。一覧に `questionCount` 無し |
| `JoinControllerTest` | 公開 200（`addFilters=false`） |
| `JoinCodeTest` | generate / form |

本番 Security は Import しない。`ReflectionTestUtils.setField` は JPA が付ける id の代用。

```text
【EJ】静的ファクトリ … Event.create / JoinCode.generate
【EJ】enum … EventStatus
【EJ】不変性 … Request/Response は record
【EJ】依存性注入 … EventService コンストラクタ
```
