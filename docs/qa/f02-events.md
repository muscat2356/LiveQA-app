# F-02 ルーム作成・一覧・Join 照会

scope: `POST/GET /api/events`、`GET /api/events/{id}`、`PATCH /api/events/{id}`（名称）、`DELETE /api/events/{id}`、`GET /api/join/{code}`。開閉は [f03-status.md](f03-status.md)。  
正: [openapi-notes](../api/openapi-notes.md) / [constraints](../design/constraints.md) / [er-diagram](../design/er-diagram.md)。 Issue: [#70](https://github.com/muscat2356/live-qa-app/issues/70)。

## 到達点

| API | 状態 |
|-----|------|
| `POST /api/events` | HOST。201。title 1〜100、join_code 8文字 UNIQUE。`joinUrl` **なし**（#69） |
| `GET /api/events` | 自分の一覧。`questionCount` **なし** |
| `GET /api/events/{id}` | 所有者のみ。他人・無しは **404**。`questionCount` あり。`joinUrl` **なし**（#69） |
| `GET /api/join/{code}` | 公開。紹介 DTO |
| `PATCH /api/events/{id}` | **実装済**（#94）。名称のみ。joinCode 不変。どの status でも可。`DELETED` は 404 |
| `DELETE /api/events/{id}` | **実装済**（#94）。論理削除 `status=DELETED`。204。行は残す。`deleted_at` なし |

## データの流れ（作成）

```text
POST /api/events + SESSION + X-XSRF-TOKEN + { "title": "..." }
  → HOST 検証
  → requireByOidcSubject → users.id
  → JoinCode 発行（衝突なら再試行、上限で 409）
  → Event.create → save
  → EventCreateResponse 201
```

一覧: `findByHost_IdAndStatusNotOrderByCreatedAtDesc`（`DELETED` 除く）→ `EventSummaryResponse[]`。  
Join: 形式不正 400（「参加コードが不正です。」）、無し・`DELETED` は 404（「該当するルームが見つかりません。」）。

## テスト

| 層 | 置き場 | 見るもの |
|----|--------|----------|
| Service | `EventServiceTest` | 名称 trim・論理削除・DELETED は 404。`PATCH /status` に DELETED は 400 |
| Controller | `EventControllerTest` | 名称 PATCH 200 / CSRF なし 403 / 楽観ロック 409。DELETE 204 / CSRF なし 403 |
| enum | `EventStatusTest` | `canTransitionTo` は DELETED に行かない。DELETED からは全部 false |

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

## Q. Service は `String title` と Request のどちらか？

**title だけなら Controller で剥がして `String` を渡す。** 作成 `save(oidc, request.title())` と開閉 `updateStatus(id, request.status(), oidc)` がすでにそう。HTTP の箱は Controller。Service は業務に要る値だけ。

質問 POST が Request ごとなのは `body` と `nickname` の2つだから。名称 PATCH は title 1つなので剥がしてよい。trim は Service。桁数は `@Valid` の Request。

| 層 | 受け取るもの |
|----|-------------|
| Controller | `EventCreateRequest`（`@Valid @RequestBody`） |
| Service | `String title`（`request.title()`） |
| Entity | trim 済みの `String` |

## Q. 削除はソフトデリートか？ `DELETED` を status に足すか？

**status に `DELETED` を足す。`deleted_at` は足さない。** 質問の `ARCHIVED` と同じ「列を増やさず状態で隠す」。物理 DELETE だと joinCode が空いて偶然再利用されうるし、配下 CASCADE は重い。

| | `deleted_at` | `status=DELETED` |
|--|--------------|------------------|
| 列 | 追加（Flyway） | 既存 `status` |
| 削除前の OPEN/CLOSED | 残る | **上書きされる**（MVP は復元しないので可） |
| このリポとの揃い | 母体ハブ用に先送り | 質問 ARCHIVED / 「status で閉じる」 |

`PATCH /api/events/{id}/status` には `DELETED` を渡さない（400）。開閉は `OPEN`/`CLOSED` だけ。削除は `DELETE` が `changeDeleted` する。

## Q. 名称更新の前に DELETED を見るか？ 同時リクエストは行ロックで平気か？

**見る。ただし SELECT 時点の確認。** `requireOwnedEvent` の `status <> DELETED` がそれ。Java の if をもう一段足す必要はない（同じ TX のメモリを見るだけになる）。

**`@Transactional` では行ロックにならない。** 既定の `find` は SELECT だけ。PostgreSQL の行ロックは UPDATE のとき。2本が両方 SELECT できたあと、後から COMMIT した UPDATE が勝つ。

Hibernate 既定は `@DynamicUpdate` なしなので、名称更新の UPDATE に **status も載る**。メモリ上はまだ OPEN のまま。削除のあと名称がコミットすると **DELETED が OPEN に戻る**ことがある。これが「同時に来るとおかしい」の本体。

| やり方 | この機能 |
|--------|----------|
| SQL で DELETED を empty | 直列なら足りる。同時だけ穴。**それだけは不足** |
| `@Version`（楽観） | **付ける。** 後勝ちを 409 |
| `PESSIMISTIC_WRITE` | 待たせる。名称×削除には重い |

名称 PATCH と論理 DELETE は同じ `events` 行を書く。Hibernate 既定の全列 UPDATE だと DELETED が OPEN に戻る。**この機能は楽観ロックを付ける。** `changeTitle` の直前 if はロック無しなら同じ穴。質問の `question_count` は原子的 UPDATE（版を触らない）。悲観は付けない。

1つの PATCH に title と DELETED を混ぜない（パスが分かれている）。フロントは JSON `{ "title" }` だけ。

## Q. 楽観ロックと悲観ロックの違いは？

**衝突の扱い方が違う。** 楽観は「たぶん被らない。書いたときに検証する」。悲観は「書くまで他を待たせる」。

```text
楽観（@Version）
  両方 SELECT（version=1）
  削除 COMMIT → version=2
  名称 UPDATE ... WHERE version=1 → 0件 → 例外

悲観（PESSIMISTIC_WRITE / SELECT FOR UPDATE）
  名称が行を掴む
  削除の SELECT は待つ
  名称 COMMIT → 削除が再開 → もう DELETED なら 404
```

| | 楽観 | 悲観 |
|--|------|------|
| いつ争うか | UPDATE 時 | SELECT 時 |
| 他 TX | 読める・書ける | その行の書き込みは待つ |
| 失敗 | 後勝ちが例外 | 待ち（デッドロックもあり） |
| 向く仕事 | 名称変更のように稀な同時 | 在庫のように必ず直列したい |

JPA の型: `@Version` はメタデータ（列が版）。`LockModeType.PESSIMISTIC_WRITE` は enum（ロックの指定）。どちらも今の Entity には無い。

【EJ】あえて外す … 悲観は付けない。質問件数は原子的 UPDATE  
【EJ】例外 … 楽観の失敗は 409 Conflict  

## Q. 悲観のほうが堅牢では？

**書き込みの直列化は悲観の方が硬い。** 同じ行を2本が同時に書けない。名称が DELETED を OPEN に戻す穴は、待たせるので潰れる。

堅牢さはそれだけではない。

| | 悲観 | 楽観 |
|--|------|------|
| 同時書き込み | 待たせる。後勝ちの上書きは起きない | 後勝ちは例外。リトライか 409 |
| 待ち | ロック中は他 TX が止まる | 待たない |
| 失敗モード | デッドロック、コネクション占有 | 衝突時だけ例外 |
| 読み取りが多い画面 | ロックが長いと遅い | 向いている |

主催者1人が名称と削除を同時に叩くのは稀でも、穴は「削除が復活する」なのでロックは付ける。待たせる悲観までは不要。衝突は 409。件数加算は版と混ぜない。

```text
DELETE /api/events/{id}
  → findByIdAndHost_Id（DELETED は empty）→ 404
  → status を DELETED（同一 TX の managed なら save 省略可）
  → 204
```

一覧・Join・詳細・質問 API は `DELETED` を「無い」とみなす。OPEN 中の削除は UI で確認ダイアログ必須。STOMP `EVENT_DELETED` は F-07 にしない。

## Q. 未実装の API は設計 docs から外すか？

**外さない。** 契約の正は openapi / ER / constraints。到達点だけ「今コードにあるか」を書く。名称更新・削除は #94（Closes #80）。

## Q. 一覧の日時は API で切るか？（#59）

**切らない。** API の `createdAt` は ISO 8601 / UTC のまま。表示はフロント（Asia/Tokyo、日付と時まで。例 `2026/08/21 14時`）。詳細も同じヘルパ。

## Q. `joinUrl` は API に要るか？（#69）

**要らない。** 値は常に `"/join/" + joinCode`。参加 URL の origin はフロントの関心。back は DTO から外す（`back/refactor/02`）。画面は `joinCode` から組み立てる（front は #69 残り）。

## Q. `/join/1/qa` がエラーになるのはなぜか？（#92）

**`1` は event id。参加パスの正は joinCode。** 以前の詳細画面が「主催者URL」として `/join/${event.id}/qa` を出していた。

| 画面 | 正 |
|------|------|
| 参加者 | `/join/{joinCode}` → `/join/{joinCode}/qa` |
| 主催者 Q&A | `/dashboard/{id}/qa` |

到達点: 詳細の主催者URLは `/dashboard/{id}/qa`（`frontend/app/(organizer)/dashboard/[id]/page.tsx`）。`/join/1/qa` を直打ちすると、いまも `GET /api/join/1` → `JoinCode` が 8 文字でないので **400** → SSR throw。[#92](https://github.com/muscat2356/live-qa-app/issues/92) は修正済みでクローズ。

## Q. `findByJoinCode` の `@Transactional(readOnly = true)` は要るか？（#94）

**必須ではない。** レビュー指摘どおり、ここは参照1本（`findByJoinCodeAndStatusNot`）で、DTO は `id` / `title` / `status` だけ。`host`（LAZY）は触らない。

| | 要る？ |
|--|--------|
| 書き込み（`save` / `updateTitle` / `delete` / `updateStatus`） | **要る**（同一 TX で変更をまとめる。`@Modifying` も TX 内） |
| `findByJoinCode` | 無くても動く。Spring Data の `find` が短い TX を自分で持つ |
| `findAll` / `findById` | 同じ理由で必須ではない。残しているのは参照系の揃え |

`QuestionService.findByEventId` はもともと TX なし。Join だけ外すなら指摘どおりでよい。参照系を全部外すかは好み（`readOnly` は flush 抑制の印）。

```text
【EJ】公開面最小化 … 要らない注釈は足さない
```

```text
【EJ】enum … 開閉は canTransitionTo。DELETED は DELETE 専用
【EJ】静的ファクトリ … Event.create / JoinCode.generate
【EJ】不変性 … Request/Response は record
【EJ】依存性注入 … EventService コンストラクタ
【EJ】あえて外す … Entity は可変。行は残す（物理 DELETE しない）
```
