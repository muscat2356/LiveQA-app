# POST /api/events（F-02 最小単位）

> 依頼キーワード **実装**。正: `docs/api/openapi-notes.md` / `docs/design/constraints.md`（title 1〜100、join_code `[A-Za-z0-9]{8}`）。

## なぜこの単位か

F-01（認証・JIT）済み。次はイベント作成で `host_id`＝ローカル `users.id` を使い始める。  
一覧・status 変更・join 公開 API は後続。

## データの流れ

```text
POST /api/events + SESSION + X-XSRF-TOKEN + { "title": "..." }
  → Session 復元 → @PreAuthorize HOST
  → UserService.requireByOidcSubject(sub) … DB 復元
  → title 検証（trim 後 1〜100）
  → joinCode 発行（8文字・衝突なら再試行）
  → Event.create(host, title, joinCode) → save
  → EventResponse へ 変換 → 201
```

## 写経ファイル

| ファイル | 役割 |
|----------|------|
| `EventRepository` | save / existsByJoinCode |
| `CreateEventRequest` / `EventResponse` | request/response record |
| `EventService#create` | 検証・発行・格納・DTO |
| `EventController` | HOST・CSRF・`ResponseEntity` 201 |
| `SecurityConfig` | `/api/events/**` を `authenticated()`（任意だが 401 JSON 揃え） |
| `EventServiceTest` / `EventControllerTest` | 正常＋衝突再試行／HOST 201・非HOST 403 |

## 後続（この単位の外）

- GET 一覧（`questionCount` は載せない）
- GET 詳細（`questionCount`＝`events.question_count`）・PATCH status・GET /api/join/{code}
- 質問 POST 時に `events.question_count` 原子的 +1
- `@Valid` + `spring-boot-starter-validation` + B-19（400 `fields`）
