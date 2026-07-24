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

## AI Agent への依頼方針（LibraShareから継承）

### 共通

- **実装は本人が手で行う**。Agent は下書きの一括生成より、差分整理・設計確認・エラー原因の説明を優先する
- 依頼時は**機能 ID（F-01 / F-07 等）**か `docs/` への参照を添える
- 設計の正は `docs/` と `README.md`。矛盾があれば docs を優先し、差分を明示する

### 「実装」と送られた場合の進め方（共通）

- ① **現状のコードを確認**: 関連する `docs/` と担当領域の実装を読み、現在地（未実装・暫定・TODO）を特定する
- ② **次タスクを提示**: 設計と差分が最小になる順で、次にやるべき作業を具体化する
- ③ **写経コード + 解説**: 写経できるコード例 + 次を中心に説明する（基本構文の説明は不要）
  - **機能**: 各単位が何をするか（入力→処理→出力 / HTTPステータス）
  - **依存**: どのライブラリ・既存モジュールをなぜその層で使うか
  - **データの流れ**: 状態 / Cookie / API / STOMP がどこから来てどこへ渡るか
  - **初出の要素の解説**: 初めて登場する注釈・API・構文は直後に「何をするか / なぜ使うか / どこに効くか」を詳細に追記する

### Member A（フロント / Next.js）

- 既存（またはこれから整える）App Router 構成に合わせる
- セキュリティの正は API 側。フロントの権限制御は UI 出し分け + 401/403 ハンドリング
- Cookie（`SESSION` / `XSRF-TOKEN`）付きで REST を呼ぶ。STOMPも同じセッションCookie

### Member B（バック / Session+API）

- レイヤ構成（controller / service / repository / domain / dto / Flyway）に合わせる
- エラーレスポンスは `{ status, error, message }`（400は任意で `fields`）。詳細は openapi-notes
- DB変更は Flyway 追加のみ（既存 V* を書き換えない）

### Member C（WebSocket+Docker）

- STOMPトピック・配信ペイロードは [docs/api/openapi-notes.md](docs/api/openapi-notes.md) と [docs/design/websocket-sequence.md](docs/design/websocket-sequence.md) が正
- Compose（`nextjs` / `api` / `postgres` / `redis` / `keycloak`）と CI の骨組みを主導

フロント詳細は [frontend/AGENTS.md](frontend/AGENTS.md) と `.cursor/rules/live-qa-frontend.mdc`。  
バック詳細は [backend/AGENTS.md](backend/AGENTS.md) と `.cursor/rules/live-qa-backend.mdc`。

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
