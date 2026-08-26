# スコープ（MVP-A / MVP-B / ストレッチ）

14日・3名・アジャイル。**MVP-A が死守ライン**、MVP-B は第2イテレーション、それ以外はストレッチ。テーマはリアルタイム ライブQ&A + 投票（sli.do風）。機能 ID と担当は [../README.md](../README.md) の機能テーブルが正。

## MVP-A（必須・死守ライン・F-01〜F-09）

### F-01 認証・セッション（担当B主 / A=UI / C=Compose）

- 主催者ログイン: KeycloakのSSO(OIDC)でログイン → Cookieセッション（Spring Session + Redis）で管理。会員はOIDCクレームから自動作成（自前の登録画面は持たない）。ログアウトあり
- RBAC（HOST=主催者 / 参加者=匿名セッション）
- CSRF保護の有効化（stateful化に伴い有効化）

### F-02 / F-03 ルーム（担当B主 / A=UI）

- イベント作成（主催者）・参加コード / URL発行
- イベント名の更新・イベント削除（所有者のみ。削除は物理 DELETE。配下は FK CASCADE）
- ルーム状態遷移 `DRAFT → OPEN ⇔ CLOSED`（作成直後は `DRAFT`）

### F-04 参加（担当A主 / B=匿名セッション）

- 参加ページ（参加コードまたはURL）。ログイン不要
- 匿名セッション（`SESSION` Cookie）で「1人1票」を識別（`voter_key`）

### F-05 / F-06 / F-08 Q&A CRUD（担当B主 / A=UI / C=整合性支援）

- 質問投稿・いいね（question × セッションの一意制約で1人1票）
- 人気順 / 新着順（QueryDSL）
- 主催者による質問ステータス変更（回答済み / アーカイブ）
- マイイベント（主催者が自分のイベントを管理）

### F-07 WebSocket（担当C主 / A=受信UI）

- 質問投稿・いいね・ステータス変更のリアルタイム反映
- 配信イベント: `QUESTION_CREATED` / `QUESTION_UPVOTED` / `QUESTION_ANSWERED`
- WebSocketハンドシェイクのCookie認証

### F-09 基盤（担当C主 / A=フロント手順）

- Docker一括起動（`nextjs` / `api` / `postgres` / `redis` / `keycloak`）
- GitHub Actions CI（backend test / frontend lint・type-check・build）

## MVP-B（第2イテレーション・F-10〜F-13）

MVP-A が E2E で通ってから着手。API の正は [api/openapi-notes.md](api/openapi-notes.md)。ER は [design/er-diagram.md](design/er-diagram.md)。制約は [design/constraints.md](design/constraints.md)。

| ID | 内容 | API（要約） | Issue |
|----|------|-------------|-------|
| **F-10** | ポール作成・開閉。設問＋選択肢。`DRAFT → OPEN → CLOSED`（終端）。Q&A と同一ルーム | `POST/GET /api/events/{id}/polls`、`PATCH /api/polls/{pollId}/status` | [#74](https://github.com/muscat2356/live-qa-app/issues/74) |
| **F-11** | ポール回答・得票集計。ライブ更新は C が `POLL_UPDATED` | `POST /api/polls/{pollId}/responses`、`GET /api/polls/{pollId}/results` | [#21](https://github.com/muscat2356/live-qa-app/issues/21) |
| **F-12** | 質問一覧の status フィルタ（QueryDSL 拡充） | `GET /api/events/{id}/questions?status=` | [#22](https://github.com/muscat2356/live-qa-app/issues/22) |
| **F-13** | 簡易モデレーション。**新パスは作らない。** F-08 の `ARCHIVED` ＋ F-12 で参加者から隠す。確認用 `?status=ARCHIVED` は所有者のみ | `PATCH /api/questions/{qid}/status` `{ "status": "ARCHIVED" }` | [#23](https://github.com/muscat2356/live-qa-app/issues/23) |

アンケート型（正解なし）。`SINGLE` / `MULTI`。明細の個別 DELETE は持たない。イベント削除は物理 DELETE（配下 CASCADE）。

## さらなるストレッチ（余力・F-14 / F-15）

- **F-14 参加者数プレゼンス**: 各イベントの同時接続数をライブ表示
- **F-15 QRコード参加**: 参加URLをQRで表示

## 対象外

- 決済連携
- メール / プッシュ通知
- 画像・動画・ファイル添付
- チャット（1:1 DM）
- モバイルアプリ
- 動画配信・画面共有

## 削る順番（時間不足時）

1. ストレッチ（F-14 / F-15）を全カット
2. MVP-B（F-10〜F-13）をカット（Poll → フィルタ → モデレーションの順で削る）
3. 主催者ダッシュボードの操作を最小化
4. 人気順ライブ更新 → 一定間隔の定期リフレッシュへ縮退（最終手段）

**質問・いいねのWebSocketライブ反映とCookieセッション認証、Docker/CI（MVP-A）は最後まで死守する。**
