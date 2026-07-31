# GET /api/events 実装メモ（F-02 前半）

> 依頼キーワード **実装**（2026-07-31）。設計の正は `docs/api/openapi-notes.md` / `docs/design/er-diagram.md`。  
> Effective Java の意識は `.cursor/rules/live-qa-backend.mdc`。

## ドメイン Entity を揃え切る（2026-07-31）

### ① 現状

| あり | 無し / 要修正 |
|------|----------------|
| `User` / `Event` + ファクトリ | `Question` / `Vote` |
| enums 3種 | **`QuestionStatus` の typo**（`OPNE` / `ANSEWERD` → `OPEN` / `ANSWERED`） |
| `V1__init.sql`（中身 OK） | **パスが `db/migration/migration/` 二重** → `db/migration/` へ |
| | Poll 系は MVP-B。今やるなら Entity のみ可（V2 DDL は後で） |

### ② 推奨順

1. Flyway パス修正 + `QuestionStatus` 修正  
2. `Question` / `Vote`（MVP-A 必須）  
3. （任意）`PollType` enum + `Poll` / `PollOption` / `PollResponse`  
4. 次は Repository → Service（ドメインの外）

### ③ ファクトリ方針（User / Event に合わせる）

- `Question.post(event, body, displayName)` … status=OPEN, upvoteCount=0  
- `Vote.cast(question, voterKey)` … いいね1票  
- Poll 系も `create` / `choose` など名前付き生成

## ③ 写経の要点と EJ

| 要素 | 操作の意味 | EJ / 型の正体 |
|------|------------|----------------|
| DTO `record` | API 応答の形。Entity を外に出さない | **不変性**。可変 Entity と役割分離 |
| `EventStatus` enum | ルーム状態を型で表す | **enum over int/String 連打**。JPA は `EnumType.STRING` |
| Entity の equals | 一覧では使わない想定なら未実装で可 | 安易な全フィールド equals は避ける |
| `CurrentUserService` | `sub` → DB 行を**復元**（無ければ**作成**） | 公開面は Service。Controller は薄く |
| `@PreAuthorize` | 呼び出し前に HOST を**検証** | 認可の門番。業務分岐は Service |
| `@Transactional(readOnly=true)` | 読み取りトランザクション | 一覧の一貫した読み取り |

### データの流れ

```text
GET /api/events + SESSION
  → Redis から SecurityContext を復元
  → HOST を検証（未ログイン 401 / 非 HOST 403）
  → OidcUser.sub → users.id（JIT）
  → events WHERE host_id = ? ORDER BY created_at DESC
  → questionCount を付与して EventSummaryResponse[] を JSON
```

### Security 差分（既存）

当時は `/api/me` のみ `authenticated()`、他は `permitAll()`。  
`@PreAuthorize` と合わせて `/api/events/**` を `authenticated()` にすると未ログインが 401 JSON になる。

## 動作確認

```bash
# ログイン後
curl -s -b cookies.txt http://localhost:8081/api/events
# 未ログイン → 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/api/events
```
