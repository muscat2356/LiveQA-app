# F-05 質問投稿・いいね — 到達点と写経順

## Q. F-05 で B がやることは？

scope: **質問投稿・いいね**（一覧の本格 QueryDSL は F-06、ステータス変更は F-08、STOMP は F-07/C）。

| API | 備考 |
|-----|------|
| `POST /api/events/{id}/questions` | OPEN のみ。guest/HOST。201 |
| `GET /api/events/{id}/questions` | 公開。暫定ソート可（F-06 で QueryDSL） |
| `POST /api/questions/{qid}/vote` | 1人1票。二重 409 |
| `DELETE /api/questions/{qid}/vote` | 取消 |

## 到達点（`back/feature/05` 開始時）

| 項目 | 状態 |
|------|------|
| Entity `Question` / `Vote` | あり（ファクトリあり） |
| Flyway questions / votes | あり |
| Repository / Service / Controller | **未** |
| `VoterKey` / Guest Service（F-04） | **このブランチに未マージの可能性** → 先に develop/F-04 を取り込む |
| Security | `/api/events/**` が **authenticated** → 匿名投稿のため **質問・いいね経路を許可する必要あり** |
| STOMP | F-05 では **出さない**（後続） |

## 写経順（小単位）

0. **前提**: F-04（`VoterKey` / guest）をブランチに載せる  
1. **`VoterKeyResolver`** … Session `guestId` → `g:` / OIDC+User → `u:`。無しは empty → 呼び出し側で 401  
2. **Repository** … `QuestionRepository` / `VoteRepository`  
3. **POST 質問** … Request/Response + Service + Controller + Security + テスト  
4. **GET 一覧** … 暫定 `created_at DESC`、`votedByMe`（voter 無ければ false）  
5. **いいね POST/DELETE** … UNIQUE 409、`upvote_count` 更新、ルーム OPEN チェック  

## 契約メモ

- body: trim 後 1〜500（`@NotBlank` + `@Size`）
- nickname → `displayName`（任意。constraints は display_name 0〜50。openapi は nickname）
- ルーム非 OPEN → 409「このルームは現在質問を受け付けていません」
- `events.question_count` を投稿時 +1（原子的）
- STOMP は後回し

## 【EJ】

```text
【EJ】静的ファクトリ … Question.post / Vote.cast
【EJ】公開面最小化 … voter_key 組み立ては VoterKey(+Resolver)
【EJ】あえて外す … F-05 では QueryDSL・STOMP・PATCH status しない
```
