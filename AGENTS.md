# live-qa-app — AI Agent 向けガイド

このファイルは Cursor 等の AI Agent がプロジェクトを理解するための共通ルールです。`.cursor/rules/` のルールと併用してください。

> LibraShare の AI 規定から**共通方針**（言語・トーン・「実装は本人が手で行う」・依頼時の進め方）を引き継ぎ、本プロジェクト向けにスコープ・分担・技術を差し替えています。

## 言語・トーン

- 常に**日本語**で回答する
- コードのコメントも日本語
- 明るく、ポジティブな口調

## プロジェクト概要

セミナー・勉強会向けの**リアルタイム ライブQ&A + 投票**アプリ（sli.do風）。Next.js + Spring Boot 分離構成。

| 項目 | 内容 |
|------|------|
| フロント | Next.js（App Router / SSR）+ TypeScript（Member A） |
| バック | Spring Boot + Java 21（Member B 主・Member C 支援） |
| 認証 | Keycloak SSO(OIDC)ログイン → Cookieセッション（Spring Session + Redis） |
| リアルタイム | WebSocket / STOMP |
| DB | PostgreSQL |
| 期間 | 約14日・3名・アジャイル |

## チーム分担

| 担当 | 領域 |
|------|------|
| Member A | Next.js 全画面、SSR、STOMP受信UI |
| Member B | Keycloak OIDC→セッション橋渡し、RBAC/CSRF、主要CRUD API、QueryDSL |
| Member C | STOMP配信、ハンドシェイクCookie認証、Docker Compose、CI |

機能 ID と主担当の対応は [README.md](README.md) の機能テーブル、詳細は [docs/roles.md](docs/roles.md)。

## 開発フロー

**アジャイル（イテレーション型）**。学習フェーズ → MVP-A → MVP-B → 仕上げ。設計の正は `docs/` と `README.md`。

| フェーズ | 期間 | 内容 |
|---------|------|------|
| 学習フェーズ | Day1-4 | 各自の主役技術をJIT学習 + 基盤疎通ゲート |
| MVP-A（Sprint 1） | Day5-9 | ライブQ&Aコア（F-01〜F-09）。末尾 Go/No-Go |
| MVP-B（Sprint 2） | Day10-13 | Poll + 運用強化（F-10〜F-13） |
| 仕上げ | Day14 | E2E・デモ・UX |

- **MVP-A が死守ライン**。未達なら MVP-B に進まず仕上げを優先する
- 毎朝15分の進捗共有。各イテレーション末にデモ + ふりかえり

## スコープ

### MVP-A（必須・死守ライン）

- F-01  Keycloak SSO + Cookieセッション認証(RBAC/CSRF)
- F-02  イベント作成・参加コード/URL発行
- F-03  ルーム状態 `DRAFT → OPEN ⇔ CLOSED`
- F-04  参加者の匿名参加
- F-05  質問投稿・いいね（1人1票）
- F-06  人気順 / 新着順
- F-07  WebSocket(STOMP)ライブ反映
- F-08  質問ステータス変更（回答済み / アーカイブ）
- F-09  Docker一括起動 + GitHub Actions CI

### MVP-B（第2イテレーション）

- F-10  ライブ投票(Poll) 作成・開閉
- F-11  Poll回答・得票分布ライブ更新
- F-12  質問フィルタ
- F-13  簡易モデレーション

### さらなるストレッチ（余力）

- F-14  参加者数プレゼンス
- F-15  QR参加

### 対象外

決済、メール/プッシュ、ファイル添付、1:1チャット、モバイルアプリ、動画配信

## ロール

| ロール | 利用 |
|--------|------|
| `HOST` | Keycloakでログインした主催者。イベント作成・開閉・質問ステータス・Poll運営 |
| 参加者（匿名） | ログイン不要。匿名セッション + `voter_key` で1人1票 |

認証（Keycloakログイン）と以降の認可（Cookieセッション + RBAC）を分離する。参加者はSSO不要。

## 設計ドキュメント（正）

索引: [docs/README.md](docs/README.md)

- [docs/scope.md](docs/scope.md) — MVP-A / MVP-B / ストレッチ
- [docs/roles.md](docs/roles.md) — 役割・F-xx 対応
- [docs/schedule.md](docs/schedule.md) — 14日アジャイル
- [docs/architecture.md](docs/architecture.md) — アーキテクチャ
- [docs/design/service-spec.md](docs/design/service-spec.md) — サービス仕様
- [docs/design/screen-transition.md](docs/design/screen-transition.md) — 画面遷移
- [docs/design/websocket-sequence.md](docs/design/websocket-sequence.md) — シーケンス
- [docs/design/er-diagram.md](docs/design/er-diagram.md) — ER / Flyway案
- [docs/design/constraints.md](docs/design/constraints.md) — 制約仕様
- [docs/api/openapi-notes.md](docs/api/openapi-notes.md) — REST / STOMP契約

## AI Agent への依頼方針（索引）

詳細は `.cursor/rules/` に分割（漏れなく読むときは下表の順）。

| 順 | ファイル | 内容 |
|----|----------|------|
| 1 | [live-qa-project.mdc](.cursor/rules/live-qa-project.mdc) | スコープ・チーム・言語 |
| 2 | [live-qa-keywords.mdc](.cursor/rules/live-qa-keywords.mdc) | **二文字キーワード**（実装 / 解説 / 確認 / 修復） |
| 3 | [live-qa-learning-explain.mdc](.cursor/rules/live-qa-learning-explain.mdc) | 操作の意味・型の正体・写経の最低構成 |
| 4 | [live-qa-learning-memo.mdc](.cursor/rules/live-qa-learning-memo.mdc) | 質問回答 → `docs/qa/` 出力／追記 |
| 5 | [live-qa-backend.mdc](.cursor/rules/live-qa-backend.mdc) / [live-qa-frontend.mdc](.cursor/rules/live-qa-frontend.mdc) | 領域別規約 |

### 共通（要約）

- **実装は本人が手で行う**。Agent は差分整理・写経単位・解説を優先
- 依頼は**機能 ID（F-01 等）**か `docs/` 参照 + 必要なら二文字キーワード
- 設計の正は `docs/` と `README.md`。矛盾時は docs 優先し差分を明示
- **学習メモ**: 仕組み・用語・トラブル原因などの回答は `docs/qa/` へ md 新規作成または追記（秘密情報は書かない）

### 二文字キーワード（4種）

`実装` `解説` `確認` `修復`  
→ 詳細・旧語エイリアスは [live-qa-keywords.mdc](.cursor/rules/live-qa-keywords.mdc)

### Member メモ

| Member | 要点 | 詳細 |
|--------|------|------|
| A | Cookie 付き REST / STOMP。権限制御は UI + 401/403 | [frontend/AGENTS.md](frontend/AGENTS.md) |
| B | controller→service→repository。Flyway 追加のみ。エラー JSON | [backend/AGENTS.md](backend/AGENTS.md) |
| C | STOMP・Compose・CI。契約は openapi-notes / websocket-sequence | 同上 backend + docs |

## フロントエンド規約（概要）

- Next.js App Router + TypeScript
- データ取得: TanStack Query を基本（SSRは Server Component / fetch）
- バリデーション: Zod
- UI: Tailwind CSS + shadcn/ui
- STOMPクライアントで `/topic/events/{eventId}/questions`（MVP-Bは `/polls` も）を購読
- CSRF: 状態変更系は `X-XSRF-TOKEN` ヘッダ

## バックエンド規約（概要）

- Java 21 + Spring Boot（controller / service / repository / domain / dto）
- 認証: `oauth2Login`（Keycloak）→ Spring Session(Redis)。以降は `SESSION` Cookie
- 認可: `@PreAuthorize` + 所有者チェック。参加者は匿名セッション
- CSRF有効（stateful）
- DB変更は Flyway 追加のみ
- エラー: `{ status, error, message }`。入力 400 / 未認証 401 / 認可 403 / なし 404 / 競合 409

## 参照リポジトリ

- 前回ポートフォリオ: `../LibraShare`（Keycloak SSO設定・Spring Securityの資産を再利用可）
