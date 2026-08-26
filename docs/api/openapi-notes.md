# API仕様メモ（OpenAPI）

ライブQ&A + 投票アプリのREST API / STOMP設計メモ。実装時は springdoc-openapi でコードからOpenAPI 3を自動生成する前提の、設計段階の合意事項。ER図は [../design/er-diagram.md](../design/er-diagram.md)、制約は [../design/constraints.md](../design/constraints.md)、シーケンスは [../design/websocket-sequence.md](../design/websocket-sequence.md) を参照。

- ベースパス: `/api`
- 認証: Keycloak(OIDC/SSO)でログイン → Cookieセッション（`SESSION` Cookie、Spring Session + Redis）で以降を管理するハイブリッド
- ロール: `HOST`（Keycloakでログインした主催者） / 参加者（匿名セッション、`voter_key = g:{guestId}`）
- Content-Type: リクエスト/レスポンスとも `application/json`（認証リダイレクトを除く）
- 日時: ISO 8601 / UTC（例 `2026-07-24T05:00:00Z`）

空ニックネームは DB に `"匿名"` を格納し、JSON も `"匿名"`（`null` は返さない）。

## 横断仕様

### 認可

- `HOST` 操作（イベント作成 / イベント一覧・詳細 / 名称更新・削除 / ルーム開閉 / 質問ステータス変更 / ポール作成・開閉）は `@PreAuthorize("hasRole('HOST')")` で制限。かつ対象イベントは自分が所有するものに限定
- 公開エンドポイント（参加情報取得 / 質問一覧 / ポール結果）は未認証でも参照可
- 質問投稿・いいね・ポール回答は、参加者の匿名セッション または HOST で可能

### CSRF

- stateful（Cookieセッション）のため CSRF保護を有効化
- 状態変更系（`POST` / `PATCH` / `DELETE`）は CSRFトークンを `X-XSRF-TOKEN` ヘッダで要求（`XSRF-TOKEN` Cookieと突合）
- 例外: `POST /api/auth/guest` は発行専用のため CSRF 検証しない（応答で `XSRF-TOKEN` Cookie は載る）
- 参加者（匿名セッション）にもCSRFトークンを発行する

### 共通エラーレスポンス

エラーは **`status` / `error` / `message` の3項目を基本** とする。

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "既に投票済みです。"
}
```

- `status`: HTTPステータスコード（数値）
- `error`: ステータスの英語表現（`Bad Request` / `Unauthorized` / `Forbidden` / `Not Found` / `Conflict`）
- `message`: 利用者・開発者向けの説明（日本語可）
- バリデーションエラー（400）では、任意で `fields` を付与してフィールド別エラーを返してもよい（下記）

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "入力内容に誤りがあります",
  "fields": {
    "title": "1〜100文字で入力してください",
    "options": "選択肢は2〜10個にしてください"
  }
}
```

| ステータス | error | 主な発生条件 |
|-----------|-------|-------------|
| 400 | Bad Request | 入力バリデーション違反（必須・桁数・形式・選択数） |
| 401 | Unauthorized | 未ログイン（HOST操作） / 匿名セッション未確立での投票 |
| 403 | Forbidden | ROLE 不足・CSRF 失敗など（認可フィルタ） |
| 404 | Not Found | 無効な参加コード、存在しないリソース、**他人所有のイベント等（存在を漏らさない）** |
| 409 | Conflict | ルーム/ポールが受付状態でない、二重投票・二重回答、不正な状態遷移、楽観ロック衝突 |

## 認証（SSO / OIDC・担当B）

主催者のログインは Keycloak(OIDC) に委譲する。会員登録・パスワード検証は自前で持たず、Spring Security の `oauth2Login` が提供するエンドポイントを利用する（`spring-boot-starter-oauth2-client`）。ログイン成功時にセッションを作成し、以降は `SESSION` Cookieで認証する。

### GET /oauth2/authorization/keycloak （公開・ログイン開始）

- 動作: Keycloakの認可エンドポイントへ302リダイレクト（Authorization Codeフロー開始）
- フロントの「主催者としてログイン」ボタンのリンク先。JSONボディなし

### GET /login/oauth2/code/keycloak （公開・OIDCコールバック）

- 動作: Keycloakからの認可コードを受け取り、トークン交換 → IDトークンの `sub` で `users` をJITプロビジョニング → セッション作成
- Response: `Set-Cookie: SESSION=...; HttpOnly; Secure; SameSite=Lax` を付与し、フロントの主催者ダッシュボードへリダイレクト（JSONボディなし）
- 失敗時: ログインエラーページへリダイレクト

### GET /api/me （HOST）

現在のログインユーザー情報（フロントの認証状態確認用）。

- Request: ボディなし（Cookie `SESSION`）
- Response 200:

```json
{
  "id": 1,
  "displayName": "運営太郎",
  "email": "host@example.com",
  "role": "HOST"
}
```

- `displayName` / `email` は**ローカルDBには保持せず、KeycloakのIDトークンクレーム（`name` / `email`）から取得**して返す。`id` / `role` はローカル `users` 由来
- Response 401:

```json
{ "status": 401, "error": "Unauthorized", "message": "ログインが必要です" }
```

### POST /api/auth/logout （HOST）

- Request: ボディなし（要 `X-XSRF-TOKEN`）
- 動作: ローカルセッションを破棄。既定ではRP-initiated logoutでKeycloak側のSSOセッションも終了（Keycloakのend-session endpointへリダイレクト）
- Response: 204（ローカルのみの場合） / 302（Keycloak連動ログアウトの場合）

### POST /api/auth/guest （公開・参加者）

匿名セッションを発行（または既存を再利用）。ログイン不要。CSRF 検証はしない（発行専用）。

- Request: ボディなし（`X-XSRF-TOKEN` は任意。無くても 200。応答の `XSRF-TOKEN` Cookie を後続の質問 POST に使う）
- Response 200:

```json
{ "guestId": "3f2a9c1e-8b4d-4e2a-9c1e-8b4d4e2a9c1e", "voterKey": "g:3f2a9c1e-8b4d-4e2a-9c1e-8b4d4e2a9c1e" }
```

- `voterKey` は `g:{guestId}`（HOST ログイン時の投票キーは `u:{userId}`。組み立ては共通の `VoterKey`）
- 同じ `SESSION` で再 POST すると同じ `guestId` / `voterKey` を返す（Session 属性に保持）
- 参加者は `users` 表には登録しない

> メモ: 主催者向け認証エンドポイント（`/api/me` / logout）とは別。参加者は本 API で匿名 Session を得る。質問投稿・いいねは CSRF 必須。

## イベント API（担当B）

### POST /api/events （HOST）

- Request:

```json
{ "title": "第3回LT大会 Q&A" }
```

- Response 201:

```json
{
  "id": 10,
  "title": "第3回LT大会 Q&A",
  "joinCode": "x7K2mQ9a",
  "status": "DRAFT",
  "createdAt": "2026-07-24T05:00:00Z"
}
```

- 作成直後は `DRAFT`（開始前・受付前）。質問受付を始めるには `PATCH /status` で `OPEN` にする
- 参加 URL は応答に含めない。フロントが `joinCode` から `/join/{joinCode}` を組み立てる
- Response 400（例）:

```json
{ "status": 400, "error": "Bad Request", "message": "入力内容に誤りがあります", "fields": { "title": "1〜100文字で入力してください" } }
```

### GET /api/events （HOST）

自分のイベント一覧。

- Request: ボディなし
- Response 200:

```json
[
  {
    "id": 10,
    "title": "第3回LT大会 Q&A",
    "joinCode": "x7K2mQ9a",
    "status": "OPEN",
    "createdAt": "2026-07-24T05:00:00Z"
  }
]
```

- 一覧には `questionCount` を含めない（画面でも不要）。件数は詳細のみ
- 削除済みは行が無いので載らない

### GET /api/events/{id} （HOST, 所有者のみ）

運営用のイベント詳細。

- Response 200:

```json
{
  "id": 10,
  "title": "第3回LT大会 Q&A",
  "joinCode": "x7K2mQ9a",
  "status": "OPEN",
  "questionCount": 12,
  "createdAt": "2026-07-24T05:00:00Z",
  "updatedAt": "2026-07-24T05:30:00Z"
}
```

- `questionCount` は `events.question_count`（非正規化スナップショット）。質問投稿時に原子的 +1（`UPDATE ... SET question_count = question_count + 1`）。一覧には出さない
- Response 404（存在しない、自分の所有でない。区別しない）:

```json
{ "status": 404, "error": "Not Found", "message": "イベントが見つかりません。" }
```

### PATCH /api/events/{id} （HOST, 所有者のみ）

イベント名の更新。`joinCode` / `status` は変えない。どのルーム状態でも可（誤字修正用）。

- Request（作成と同じ。trim 後 1〜100、空白のみ不可。要 `X-XSRF-TOKEN`）:

```json
{ "title": "第3回LT大会 Q&A（修正）" }
```

- Response 200: 詳細と同じ形（`EventDetailResponse`）
- Response 400: 作成と同じ `fields.title`
- Response 404: 詳細 GET と同じ（存在しない／他人所有を区別しない）
- Response 409（同じ行への同時書き込み。`events.version` の楽観ロック。名称同士だけでなく、名称・開閉・削除の同時も含む）:

```json
{ "status": 409, "error": "Conflict", "message": "他の操作と衝突しました。画面を更新して再度お試しください。" }
```

### DELETE /api/events/{id} （HOST, 所有者のみ）

イベントの物理削除。`events` 行を消す。配下の質問・いいね・ポールは FK `ON DELETE CASCADE`。どのルーム状態（`DRAFT` / `OPEN` / `CLOSED`）からでも可。復元しない（MVP）。

- Request: ボディなし（要 `X-XSRF-TOKEN`）
- Response 204: ボディなし
- Response 404: 詳細 GET と同じ（既に消えていれば 404）
- 削除後の見え方: 一覧に出ない。`GET /api/events/{id}` / `GET /api/join/{joinCode}` / 質問・ポール API は 404。`join_code` は空く（生成時の UNIQUE 再試行で衝突を避ける）
- STOMP `EVENT_DELETED` は出さない（参加者は次回 REST で 404）

### GET /api/join/{joinCode} （公開）

参加用のイベント情報取得。参加コード入力時の有効性検証にも使用。

- Response 200:

```json
{ "eventId": 10, "title": "第3回LT大会 Q&A", "status": "OPEN" }
```

- `status` は `DRAFT`（受付前） / `OPEN`（受付中） / `CLOSED`（締切）。フロントは `DRAFT` / `CLOSED` のとき投稿UIを無効化する
- Response 404（存在しない）:

```json
{ "status": 404, "error": "Not Found", "message": "該当するルームが見つかりません。" }
```

### PATCH /api/events/{id}/status （HOST, 所有者のみ）

ルームの開始・締切・再開。遷移は `DRAFT → OPEN ⇔ CLOSED`（`DRAFT` へは戻せない）。

- Request（受け付ける値は `OPEN` / `CLOSED`。開始=`OPEN`、締切=`CLOSED`、再開=`CLOSED`→`OPEN`）:

```json
{ "status": "OPEN" }
```

- Response 200:

```json
{ "id": 10, "status": "OPEN" }
```

- Response 400（不正な値）:

```json
{ "status": 400, "error": "Bad Request", "message": "status は OPEN / CLOSED のいずれかです。" }
```

- Response 409（不正な状態遷移。例: `CLOSED` から `DRAFT` へ戻す）:

```json
{ "status": 409, "error": "Conflict", "message": "この状態遷移はできません。" }
```

- Response 409（楽観ロック衝突）: 名称 PATCH と同じメッセージ
- Response 404（存在しない、または自分の所有でない。詳細 GET と同じ）:

```json
{ "status": 404, "error": "Not Found", "message": "イベントが見つかりません。" }
```

## 質問 API（担当B）

### GET /api/events/{id}/questions （公開）

質問一覧。QueryDSLで動的に並び替え/フィルタを組み立てる。イベントが無ければ 404。キー未確立でも 200（`votedByMe` は全部 false）。DRAFT/CLOSED でも返す。

- Query:
  - `sort` = `popular`（既定, `upvote_count DESC, created_at DESC`） / `newest`（`created_at DESC`）。不正は 400。**F-06 実装済**
  - `status` = `OPEN` / `ANSWERED` / `ARCHIVED`（任意）。未指定は **`ARCHIVED` 以外**。不正は 400。**F-12 実装済**
  - `status=ARCHIVED` は **イベント所有者 HOST のみ**。ゲスト・他 HOST は 403（`"この操作は主催者のみ可能です。"`）。一覧 GET は `permitAll` のままなので 401 にはしない。**F-13 実装済**
- Response 200:

```json
[
  {
    "id": 42,
    "eventId": 10,
    "body": "登壇資料は公開されますか？",
    "displayName": "とくめい花子",
    "upvoteCount": 8,
    "status": "OPEN",
    "votedByMe": true,
    "createdAt": "2026-07-24T05:01:00Z"
  },
  {
    "id": 43,
    "eventId": 10,
    "body": "懇親会の場所はどこですか？",
    "displayName": "匿名",
    "upvoteCount": 3,
    "status": "ANSWERED",
    "votedByMe": false,
    "createdAt": "2026-07-24T05:02:00Z"
  }
]
```

### POST /api/events/{id}/questions （参加者 / HOST）

- Request（`nickname` は任意。省略・空白は DB に `"匿名"` を格納し、応答の `displayName` も `"匿名"`）

```json
{ "body": "登壇資料は公開されますか？", "nickname": "とくめい花子" }
```

- Response 201:

```json
{
  "id": 42,
  "eventId": 10,
  "body": "登壇資料は公開されますか？",
  "displayName": "とくめい花子",
  "upvoteCount": 0,
  "status": "OPEN",
  "votedByMe": false,
  "createdAt": "2026-07-24T05:01:00Z"
}
```

- Response 409（ルームが `OPEN` でない = `DRAFT` 受付前 / `CLOSED` 締切）:

```json
{ "status": 409, "error": "Conflict", "message": "このルームは現在質問を受け付けていません" }
```

- 副作用: STOMP `QUESTION_CREATED` を `/topic/events/{id}/questions` へ配信

### PATCH /api/questions/{qid}/status （HOST）

- Request:

```json
{ "status": "ANSWERED" }
```

- Response 200: `QuestionDTO`（`status` が更新された内容）

```json
{
  "id": 42,
  "eventId": 10,
  "body": "登壇資料は公開されますか？",
  "displayName": "とくめい花子",
  "upvoteCount": 8,
  "status": "ANSWERED",
  "votedByMe": true,
  "createdAt": "2026-07-24T05:01:00Z"
}
```

- Response 409（不正な状態遷移）:

```json
{ "status": 409, "error": "Conflict", "message": "ARCHIVED から OPEN へは変更できません" }
```

- 副作用: STOMP `QUESTION_ANSWERED` を配信
- F-13（簡易モデレーション）もこの PATCH の `ARCHIVED`。専用パスは作らない

## いいね API（担当B）

### POST /api/questions/{qid}/vote （参加者 / HOST）

- Request: ボディなし（`voter_key` はセッションから算出。要 `X-XSRF-TOKEN`）
- Response 200:

```json
{ "questionId": 42, "upvoteCount": 8, "votedByMe": true }
```

- Response 409（既に投票済み）:

```json
{ "status": 409, "error": "Conflict", "message": "既に投票済みです。" }
```

- 副作用: STOMP `QUESTION_UPVOTED` を配信

### DELETE /api/questions/{qid}/vote （参加者 / HOST）

- Request: ボディなし（要 `X-XSRF-TOKEN`）
- Response 200:

```json
{ "questionId": 42, "upvoteCount": 7, "votedByMe": false }
```

- 副作用: STOMP `QUESTION_UPVOTED` を配信

## ポール API（MVP-B・担当B）

主催者が設問と選択肢を作り、参加者が選択肢から回答する「アンケート型」ポール（正解なし）。Q&Aと同一ルームで共存する。  
**F-10** = 作成・一覧・開閉。**F-11** = 回答・集計。STOMP 配信は Member C（口だけ F-10/F-11 で残す）。

イベントが無ければ作成・一覧・開閉・回答・結果はすべて **404**（質問 API と同じ。存在しないと区別しない）。ルームが `DRAFT` / `CLOSED` でも **作成と開閉は可**（下書きのうちに設問を用意する）。**回答だけ** ルーム `OPEN` かつポール `OPEN`。

変更系（POST / PATCH）は要 `X-XSRF-TOKEN`。

JSON の形は **PollDTO 一つ**（作成・一覧・開閉・結果で同じ）。`PollResultDTO` は別名にしない。

### F-10 作成・一覧・開閉

#### POST /api/events/{id}/polls （HOST, 所有者のみ）

設問＋選択肢をまとめて作成。作成直後は `DRAFT`。

- Request（`type` = `SINGLE` / `MULTI`。`questionText` trim 後 1〜200。選択肢は 2〜10、各ラベル trim 後 1〜100、同一ポール内で重複禁止）:

```json
{
  "questionText": "次のLTで聞きたいテーマは？",
  "type": "SINGLE",
  "options": ["AI", "Web", "インフラ"]
}
```

- Response 201:

```json
{
  "id": 5,
  "eventId": 10,
  "questionText": "次のLTで聞きたいテーマは？",
  "type": "SINGLE",
  "status": "DRAFT",
  "options": [
    { "id": 31, "label": "AI", "count": 0 },
    { "id": 32, "label": "Web", "count": 0 },
    { "id": 33, "label": "インフラ", "count": 0 }
  ],
  "totalResponses": 0,
  "myOptionIds": []
}
```

- Response 400（選択肢数）:

```json
{ "status": 400, "error": "Bad Request", "message": "選択肢は2〜10個にしてください" }
```

- Response 404: イベントが無い／他人所有

#### GET /api/events/{id}/polls （HOST=全件 / 参加者=OPENのみ）

- HOST（所有者）: `DRAFT` / `OPEN` / `CLOSED` を返す
- 参加者・未ログイン: `OPEN` のみ
- Response 200: `PollDTO[]`
- Response 404: イベントが無い（HOST の他人所有も 404）

```json
[
  {
    "id": 5,
    "eventId": 10,
    "questionText": "次のLTで聞きたいテーマは？",
    "type": "SINGLE",
    "status": "OPEN",
    "options": [
      { "id": 31, "label": "AI", "count": 12 },
      { "id": 32, "label": "Web", "count": 7 },
      { "id": 33, "label": "インフラ", "count": 3 }
    ],
    "totalResponses": 22,
    "myOptionIds": [31]
  }
]
```

未確立セッションの `myOptionIds` は `[]`。

#### PATCH /api/polls/{pollId}/status （HOST, 所有者のみ）

遷移は `DRAFT → OPEN → CLOSED`。`CLOSED` は終端（ルームと違い再開しない）。`DRAFT` へは戻せない。

- Request（受け付ける値は `OPEN` / `CLOSED`）:

```json
{ "status": "OPEN" }
```

- Response 200: PollDTO（`status` 更新済み）
- Response 400: `DRAFT` など受け付けない値。メッセージは `"status は OPEN / CLOSED のいずれかです。"`
- Response 409（不正な状態遷移。例: `CLOSED` → `OPEN`）:

```json
{ "status": 409, "error": "Conflict", "message": "この状態遷移はできません。" }
```

- Response 404: ポールが無い／イベント他人所有／イベントが無い
- 副作用（C）: `OPEN`→`POLL_OPENED` / `CLOSED`→`POLL_CLOSED`。REST 実装は配信口を残し、中身は F-07 と同様に後続

### F-11 回答・集計

#### POST /api/polls/{pollId}/responses （参加者 / HOST）

選択肢へ回答（`voter_key` はセッションから算出）。ポールとルームが両方 `OPEN` のときだけ可。

- Request（`SINGLE` はちょうど 1 件、`MULTI` は 1〜選択肢数。全て当該ポールの選択肢）:

```json
{ "optionIds": [31] }
```

- Response 200: PollDTO（最新集計）
- Response 401: 匿名セッション未確立
- Response 409（既に回答済み）:

```json
{ "status": 409, "error": "Conflict", "message": "既に回答済みです。" }
```

- Response 409（ポールまたはルームが `OPEN` でない）:

```json
{ "status": 409, "error": "Conflict", "message": "このポールは現在回答を受け付けていません" }
```

- **置換しない。** 一度入れたら 409（いいねと同じ）。`SINGLE` の「既存があれば置換」は採らない
- 副作用（C）: STOMP `POLL_UPDATED`

#### GET /api/polls/{pollId}/results （公開）

- Response 200: PollDTO（上記と同形）
- Response 404: ポールが無い／イベントが無い
- HOST 以外は `DRAFT` の結果を 404（一覧と同じ見え方。下書きの得票は見せない）

## DTOスキーマ

### QuestionDTO

```json
{
  "id": 42,
  "eventId": 10,
  "body": "登壇資料は公開されますか？",
  "displayName": "とくめい花子",
  "upvoteCount": 8,
  "status": "OPEN",
  "votedByMe": true,
  "createdAt": "2026-07-24T05:01:00Z"
}
```

- `displayName`: **常に文字列。** 未入力は `"匿名"`（DB にも `"匿名"` を格納。`null` は返さない）
- `votedByMe`: リクエスト元セッションの `voter_key` が投票済みか

### PollDTO（MVP-B）

作成・一覧・開閉・回答・結果で同じ形。

```json
{
  "id": 5,
  "eventId": 10,
  "questionText": "次のLTで聞きたいテーマは？",
  "type": "SINGLE",
  "status": "OPEN",
  "options": [
    { "id": 31, "label": "AI", "count": 12 },
    { "id": 32, "label": "Web", "count": 7 },
    { "id": 33, "label": "インフラ", "count": 3 }
  ],
  "totalResponses": 22,
  "myOptionIds": [31]
}
```

- `options[].count` は `poll_responses` の集計（F-11 まで常に 0 でよい）
- `myOptionIds`: 今のセッションが選んだ選択肢。STOMP には載せない
- 旧称 `PollResultDTO` は使わない（同じ JSON）

### エラーDTO

```json
{ "status": 409, "error": "Conflict", "message": "既に投票済みです。" }
```

- 400のみ任意で `fields`（`{ フィールド名: メッセージ }`）を付与可

## STOMP（WebSocket）配信

- 購読先: `/topic/events/{eventId}/questions`
- 送信ペイロード:

```json
{
  "type": "QUESTION_UPVOTED",
  "question": {
    "id": 42,
    "eventId": 10,
    "body": "登壇資料は公開されますか？",
    "displayName": "とくめい花子",
    "upvoteCount": 8,
    "status": "OPEN",
    "createdAt": "2026-07-24T05:01:00Z"
  }
}
```

| type | 発火タイミング | クライアント動作 |
|------|--------------|----------------|
| `QUESTION_CREATED` | 質問投稿 | リストに追加 |
| `QUESTION_UPVOTED` | いいね追加/取消 | カウント更新 + 人気順再ソート |
| `QUESTION_ANSWERED` | ステータス変更 | 表示更新（回答済みバッジ / 非表示） |

- 配信ペイロードの `question` にはセッション依存の `votedByMe` を含めない（各クライアントがローカルの投票状態で表示）

### ポール（MVP-B）

- 購読先: `/topic/events/{eventId}/polls`
- 送信ペイロード:

```json
{
  "type": "POLL_UPDATED",
  "poll": {
    "id": 5,
    "status": "OPEN",
    "options": [
      { "id": 31, "label": "AI", "count": 13 },
      { "id": 32, "label": "Web", "count": 7 },
      { "id": 33, "label": "インフラ", "count": 3 }
    ],
    "totalResponses": 23
  }
}
```

| type | 発火タイミング | クライアント動作 |
|------|--------------|----------------|
| `POLL_OPENED` | 主催者がポール開始 | 回答UIを表示 |
| `POLL_UPDATED` | 誰かが回答 | 得票分布をライブ更新（棒グラフ） |
| `POLL_CLOSED` | 主催者が締切 | 回答受付を締切、最終結果を表示 |

- ペイロードの `poll` にはセッション依存の `myOptionIds` を含めない

## 関連ドキュメント

- ER図: [../design/er-diagram.md](../design/er-diagram.md)
- 制約仕様: [../design/constraints.md](../design/constraints.md)
- サービス仕様: [../design/service-spec.md](../design/service-spec.md)
- シーケンス図: [../design/websocket-sequence.md](../design/websocket-sequence.md)
