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
| `PATCH /api/events/{id}` | **実装済**（#94）。名称のみ。joinCode 不変。無い／他人所有は 404 |
| `DELETE /api/events/{id}` | **物理 DELETE**（CASCADE）。Issue: [#99](https://github.com/muscat2356/live-qa-app/issues/99) |

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
Join: 形式不正 400（「参加コードが不正です。」）、無しは 404（「該当するルームが見つかりません。」）。

## テスト

| 層 | 置き場 | 見るもの |
|----|--------|----------|
| Service | `EventServiceTest` | 名称 trim。物理 DELETE 後は行が無い。楽観ロック 409 |
| Controller | `EventControllerTest` | 名称 PATCH 200 / CSRF なし 403 / 楽観ロック 409。DELETE 204 / CSRF なし 403 |
| enum | `EventStatusTest` | `canTransitionTo`。`DELETED` は無い |

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
| イベント削除 | **物理 DELETE**（配下 CASCADE）。`status=DELETED` は持たない。OPEN 中も可（UI で確認）。STOMP 削除配信は出さない。`join_code` は空く |

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

## Q. 削除はソフトデリートか？ 物理 DELETE か？

**正は物理 DELETE。** 旧:#94 は `status=DELETED`。

| | 旧（論理） | 新（物理） |
|--|-----------|-----------|
| 行 | 残す | `events` を消す。配下は CASCADE |
| 404 | `status <> DELETED` | 行が無い |
| joinCode | UNIQUE のまま再利用されない | 空く。生成時の exists 再試行で衝突を避ける |
| 名称との同時 | `@Version` で 409（DELETED が OPEN に戻る穴） | 両方ロード済みなら `@Version` で 409。削除が先に確定したあとの PATCH は 404 |

`PATCH /api/events/{id}/status` は `OPEN`/`CLOSED` だけ。削除は `DELETE` が `repository.delete` する。

## Q. 名称更新の前に「削除済み」を見るか？

**行が無ければ 404。** `findByIdAndHost_Id` が empty。`status <> DELETED` は不要。

名称 PATCH 同士、および名称・開閉・削除が同じ行をロードして同時に書いたときは `@Version` で 409。削除が先に確定したあとの PATCH は行が無く 404。

## Q. 楽観ロックはまだ要るか？

**同じ行をロードして書く操作のため残す。** 名称・開閉・削除が同時なら 409。削除が先に確定したあとは 404。論理削除が OPEN に戻る穴は、物理 DELETE では消える。`question_count` の加算は版を進めない。悲観は付けない。

```text
DELETE /api/events/{id}
  → findByIdAndHost_Id → empty なら 404
  → eventRepository.delete(event)  （配下 CASCADE）
  → 204
```

OPEN 中の削除は UI で確認ダイアログ必須。STOMP `EVENT_DELETED` は出さない。

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

## Q. ダッシュボード詳細だけエラーで開かない？（2026-08-26）

**API ではなく、詳細ページだけ `NEXT_PUBLIC_URL` が無いと throw する。** 一覧・主催者 Q&A はこの変数を見ない。

```text
const baseURL = process.env.NEXT_PUBLIC_URL;
if (!baseURL) throw new Error("URLが存在しません");
```

`error.tsx` がメッセージを隠すので「イベントの取得に失敗しました。」に見える。`compose.yml` も `frontend/.env.local` も `NEXT_PUBLIC_URL` を置いていない。コピー用 URL のために 2026-08-24 に入った。

開発なら `frontend/.env.local` と compose の frontend に:

```text
NEXT_PUBLIC_URL=http://localhost:3000
```

バックの `GET /api/events/{id}` は一覧と同じ Cookie SSR。ポール GET の Lazy とは別件（詳細は polls を呼ばない）。

Issue: [#114](https://github.com/muscat2356/live-qa-app/issues/114)（front。env 追加 / headers フォールバック / error.tsx）。

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
【EJ】enum … 開閉は canTransitionTo。削除は物理 DELETE（enum に載せない）
【EJ】静的ファクトリ … Event.create / JoinCode.generate
【EJ】不変性 … Request/Response は record
【EJ】依存性注入 … EventService コンストラクタ
【EJ】あえて外す … Entity は可変。行は残す（物理 DELETE しない）
```
