# 役割分担（3名・1人1テーマの新規学習を主役に）

各自が担当する新規技術を主役として学習しながら実装する。前回LibraShareで実証済みの実力（Spring Boot / 基本CRUD / React / JWTの考え方）をベースに、新規技術を1本ずつ深める。機能 ID の正は [../README.md](../README.md)。

| メンバー | 主役技術 / 領域 | 主担当（MVP-A） | 主担当（MVP-B・ストレッチ） |
|---------|----------------|----------------|---------------------------|
| **Member A** | Next.js（フロント） | 全画面UI / SSR / STOMP受信（F-01〜F-08のUI） | Poll UI・棒グラフ / フィルタUI（F-10〜F-13, F-14/F-15） |
| **Member B** | Cookieセッション+Redis（バック・セキュリティ） | 認証・セッション橋渡し / 主要CRUD API / QueryDSL（F-01〜F-06, F-08） | Poll API・集計 / フィルタAPI / モデレーション（F-10〜F-13） |
| **Member C** | WebSocket+Docker（リアルタイム・環境） | STOMP配信 / Cookie認証 / Compose・CI（F-05, F-07, F-09） | Poll配信（`POLL_*`）/ プレゼンス配信（F-10・F-11, F-14） |

## Member A — Next.js（フロント）

- Next.js全画面（イベント一覧 / 詳細 / 参加ページ / 質問投稿 / ログイン導線・HOST メール登録 UI・SSR）
- ライブ質問リストUI（人気順のリアルタイム更新、STOMPクライアント受信）
- 主催者ダッシュボード（回答済み / アーカイブ操作のUI、ルーム開始/締切）
- TanStack Query、Tailwind CSS + shadcn/ui、Zod
- MVP-B: Poll投票UI・結果分布。ストレッチ: プレゼンス表示 / QR

## Member B — Cookieセッション+Redis（バックエンド・セキュリティ）

- Spring Security + Spring Session(Redis) + Cookieセッション / RBAC / CSRF
- KeycloakのOIDCログイン連携（`oauth2Login`）→ ログイン成功後にセッションへ橋渡し（LibraShareのSSO資産を再利用）
- OIDCクレーム(`sub`)からのユーザーJITプロビジョニング、RP-initiated logout
- 主催者ログイン、**HOST メール登録**（Admin API で Keycloak ユーザーを作り、パスワード設定リンクを発行。パスワードは Spring が持たない）、参加者の匿名セッション（1人1票の識別キー）
- イベント・質問・いいねの主要CRUD API、ルーム状態遷移
- QueryDSLによる軽めの動的クエリ（質問の並び替え / フィルタ）
- 質問ステータス変更API（回答済み / アーカイブ）
- MVP-B: Poll作成・回答・集計API

## Member C — WebSocket+Docker（リアルタイム・環境構築）

- WebSocket / STOMP 配信（質問・いいね・ステータス変更のライブ反映）
- WebSocketハンドシェイクのCookie認証・STOMP認可
- 環境構築: Docker Compose（`nextjs` / `api` / `postgres` / `redis` / `keycloak`）・Flyway基盤・GitHub Actions CI・Testcontainers
- いいねの整合性（一意制約 + カウント更新）まわりのバックエンド支援
- MVP-B: `POLL_OPENED` / `POLL_UPDATED` / `POLL_CLOSED` 配信。ストレッチ: プレゼンス配信

## 負荷均等化のポイント

- フロントが厚いAは、ストレッチ（F-14/F-15）を後送りにして負荷調整
- 開発基盤の初期構築はCが主導し、学習フェーズ末（Day4）の基盤疎通ゲートまでに整える
- バックエンドCRUDはB（イベント/質問/いいね）が主導、Cはリアルタイム配信と整合性を担当
- 学習フェーズで「認証 × 環境」、MVP-A中盤で「WebSocket × UI」をペアで実施し、相手分野も習得する

## AI Agent への依頼

実装は本人が手で行う。依頼時は機能 ID（F-01 等）を添える。詳細は [../AGENTS.md](../AGENTS.md)。
