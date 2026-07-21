# 30日スケジュール（学習 → 共有 → 設計 → 実装）

前回同様「未理解のまま詳細設計しない」方針。3名は A=フロント/インフラ、B=バックエンド/セキュリティ、C=WebSocket/バックエンド/環境構築。

## Phase1 学習（Day1-5）

- Day1: キックオフ。テーマ・MVP-A/MVP-B・ロール分担・リポジトリ / ブランチ運用を合意
- Day2: A=Next.js App Router + SSRの基礎 / B=Spring Security + Spring Session(Redis)の基礎 / C=WebSocket + STOMPの基礎
- Day3: A=TanStack Query + Tailwind/shadcn / B=Cookieセッション/CSRF/RBAC + QueryDSL / C=環境構築(Docker Compose・Flyway・GitHub Actions CI)の基礎
- Day4: A=Docker/インフラ基礎 + Next.jsからCookie付きREST・WS接続の素振り / B=主要CRUD設計 +（予習）Spring Batch / C=STOMP配信 + 入札同時実行制御の予習
- Day5: 各自ミニ検証（商品一覧SSR / ログイン疎通 / STOMPエコー）を持ち寄り + 相手分野の習得ペアを実施

## Phase2 共有（Day6-7）

- Day6: 各自の学習成果を発表（認証 × WebSocket × フロントの接点、CookieセッションでのWebSocketハンドシェイク認証方式を合意）
- Day7: 設計論点整理（入札の同時実行制御方式、締切処理のトリガ、検索条件の一覧確定）、MVP-Aスコープ再確認

## Phase3 設計（Day8-11）

- Day8: ER図（users / items / bids / categories 等）、状態遷移（出品中 → 締切 → 落札 / 流札）
- Day9: OpenAPI（会員・出品・商品検索・入札）、動的検索のクエリパラメータ確定
- Day10: 画面一覧・遷移図（一覧 / 詳細 / 出品 / マイページ / ログイン）、WebSocket / バッチのシーケンス図
- Day11: 設計レビュー + Docker構成図。CI(GitHub Actions)の方針確定、Flywayの初期スキーマ着手

## Phase4 実装（Day12-30）

- Day12-14 [MVP-A 基盤通し]: C=環境構築(Docker Compose/DB/Flyway/CI稼働) + B=ログイン(Cookieセッション/RBAC/CSRF) + A=フロント土台(レイアウト/ログイン画面)。3名ペアで疎通
- Day15-18 [MVP-A 主要CRUD]: B=出品/商品CRUD + 動的検索(QueryDSL) / A=一覧・詳細(SSR)/出品UI / C=入札ドメインCRUDの土台
- Day19-23 [MVP-A 入札コア]: C=WebSocket/STOMP現在価格ライブ反映 + 同時実行制御 / A=入札UI・残り時間カウントダウン / B=締切→落札確定(オンデマンド)
- Day24 Go/No-Go: MVP-A（ログイン → 出品 → 検索 → 入札 → 締切 → 落札）がE2Eで通れば MVP-B着手、未達なら仕上げ優先
- Day24-27 [MVP-B]: B=Spring Batch(自動締切/集計) + サーバーサイドページング / A=(余力)CD・ウォッチリストUI / C=(余力)通知配信
- Day28-30: E2Eテスト、README/スクリーンショット、相互デモ、バグ修正・UX仕上げ

## 情報交換セッション

- 毎朝15分の進捗共有、週1回1hのSYNC
- EX(ペア実装): Day12-14で認証 × 環境の通し、Day19-23で入札のWebSocket × UI連携をペアで
