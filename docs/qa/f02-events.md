# F-02 ルーム作成・一覧・Join 照会

scope: `POST/GET /api/events`、`GET /api/events/{id}`、`PATCH /api/events/{id}`（名称）、`DELETE /api/events/{id}`、`GET /api/join/{code}`。開閉は [f03-status.md](f03-status.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md) / [er-diagram](../design/er-diagram.md)。 Issue: [#70](https://github.com/muscat2356/live-qa-app/issues/70)。

## 到達点

| API | 状態 |
|-----|------|
| `POST /api/events` | HOST。201。title 1〜100、join_code 8文字 UNIQUE |
| `GET /api/events` | 自分の一覧。`questionCount` **なし** |
| `GET /api/events/{id}` | 所有者のみ。他人・無しは **404**。`questionCount` あり |
| `GET /api/join/{code}` | 公開。紹介 DTO |
| `PATCH /api/events/{id}` | **契約済み・実装未**（名称のみ。joinCode 不変。どの status でも可。`DELETED` は 404） |
| `DELETE /api/events/{id}` | **契約済み・実装未**（論理削除 `status=DELETED`。204。行は残す。`deleted_at` なし） |

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
Join: 形式不正 400（「参加コードが不正です。」）、無し 404（「該当するルームが見つかりません。」）。

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
| 名称更新 | `PATCH /api/events/{id}`。status 用パスと混ぜない。joinCode は不変 |
| イベント削除 | `status=DELETED`（論理・終端）。`PATCH /status` には載せない。`deleted_at` は足さない。OPEN 中も可（UI で確認）。STOMP 削除配信は出さない |

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

名称更新・削除のテストは実装単位で足す（Service: 所有者 404 / title 反映 / delete 後 find 無し。Controller: CSRF あり 200・204 / なし 403）。

本番 Security は Import しない。`ReflectionTestUtils.setField` は JPA が付ける id の代用。

## Q. 名称更新は `PATCH /status` に混ぜないか？

**混ぜない。** 開閉は状態機械、名称は属性の書き換え。パスを分ける。

| API | 触るもの |
|-----|----------|
| `PATCH /api/events/{id}` | `title` だけ。joinCode / status はそのまま |
| `PATCH /api/events/{id}/status` | `status` だけ（F-03） |

Entity は `changeTitle`（仮）で `title` と `updatedAt` を書く。同一 TX の managed なら `save` 省略可（F-03 と同じ）。

## Q. 削除はソフトデリートか？ `DELETED` を status に足すか？

**status に `DELETED` を足す。`deleted_at` は足さない。** 質問の `ARCHIVED` と同じ「列を増やさず状態で隠す」。物理 DELETE だと joinCode が空いて偶然再利用されうるし、配下 CASCADE は重い。

| | `deleted_at` | `status=DELETED` |
|--|--------------|------------------|
| 列 | 追加（Flyway） | 既存 `status` |
| 削除前の OPEN/CLOSED | 残る | **上書きされる**（MVP は復元しないので可） |
| このリポとの揃い | 母体ハブ用に先送り | 質問 ARCHIVED / 「status で閉じる」 |

`PATCH /api/events/{id}/status` には `DELETED` を渡さない（400）。開閉は `OPEN`/`CLOSED` だけ。削除は `DELETE` が `markDeleted` する。

```text
DELETE /api/events/{id}
  → findByIdAndHost_Id（DELETED は empty）→ 404
  → status を DELETED（同一 TX の managed なら save 省略可）
  → 204
```

一覧・Join・詳細・質問 API は `DELETED` を「無い」とみなす。OPEN 中の削除は UI で確認ダイアログ必須。STOMP `EVENT_DELETED` は F-07 にしない。

## Q. 未実装の API は設計 docs から外すか？

**外さない。** 契約の正は openapi / ER / constraints。到達点だけ「今コードにあるか」を書く。名称更新・削除は契約どおり実装する（#80 / #81）。

## Q. 一覧の日時は API で切るか？（#59）

**切らない。** API の `createdAt` は ISO 8601 / UTC のまま。表示はフロント（Asia/Tokyo、日付と時まで。例 `2026/08/21 14時`）。詳細も同じヘルパ。

## Q. `joinUrl` は API に要るか？（#69）

**要らない。** 値は常に `"/join/" + joinCode`。参加 URL の origin はフロントの関心。DTO から外し、画面は `joinCode` から組み立てる。

## Q. `/join/1/qa` がエラーになるのはなぜか？（#92）

**`1` は event id。参加パスの正は joinCode。** 以前の詳細画面が「主催者URL」として `/join/${event.id}/qa` を出していた。

| 画面 | 正 |
|------|------|
| 参加者 | `/join/{joinCode}` → `/join/{joinCode}/qa` |
| 主催者 Q&A | `/dashboard/{id}/qa` |

到達点: 詳細の主催者URLは `/dashboard/{id}/qa`（`frontend/app/(organizer)/dashboard/[id]/page.tsx`）。`/join/1/qa` を直打ちすると、いまも `GET /api/join/1` → `JoinCode` が 8 文字でないので **400** → SSR throw。Issue: [#92](https://github.com/muscat2356/live-qa-app/issues/92)。

```text
【EJ】enum … 開閉は canTransitionTo。DELETED は DELETE 専用
【EJ】静的ファクトリ … Event.create / JoinCode.generate
【EJ】不変性 … Request/Response は record
【EJ】依存性注入 … EventService コンストラクタ
【EJ】あえて外す … Entity は可変。行は残す（物理 DELETE しない）
```
