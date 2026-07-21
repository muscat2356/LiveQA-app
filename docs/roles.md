# 役割分担（3名・できるだけ均等に）

各自が主担当分野を学習 → 共有する「学習 → 共有 → 設計 → 実装」フローを踏襲。Day5前後で相手分野の習得（ペア）も実施する。

## Member A — フロント・インフラ

- Next.js全画面（一覧 / 詳細 / 出品 / 入札 / マイページ / ログイン・SSR）
- 入札WebSocketクライアントUI（現在価格・残り時間カウントダウン）
- TanStack Query、Tailwind CSS + shadcn/ui
- インフラ: Docker本番構成 ／ （MVP-B）CD・デプロイ・リバースプロキシ

## Member B — バックエンド・セキュリティ

- Spring Security + Spring Session(Redis) + Cookieセッション / RBAC / CSRF
- 会員・出品・商品の主要CRUD API
- QueryDSLによる動的検索
- （MVP-B）Spring Batch・サーバーサイドページング

## Member C — WebSocket・バックエンド・環境構築

- WebSocket / STOMP 入札配信 + 同時実行制御
- 入札ドメインのCRUD API
- 環境構築: ローカルDocker Compose・DB / Flyway基盤・GitHub Actions CI・Testcontainers

## 負荷均等化のポイント

- フロントが厚いAはインフラを軽め（CD等はMVP-B送り）
- 開発基盤の初期構築はCが主導
- バックエンドCRUDは B（会員 / 出品 / 商品）と C（入札）で分担
