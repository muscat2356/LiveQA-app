# live-qa-app — AI Agent 向けガイド

Cursor 等向けの**索引**。規範の正は `.cursor/rules/`（**6本のみ・増やさない**）と `docs/`。

> LibraShare から共通方針（日本語・トーン・実装は本人・依頼キーワード）を継承。スコープ・技術は本リポ向け。

## 言語・トーン

日本語。コメントも日本語。明るくポジティブ。

## 概要

セミナー向け**ライブQ&A + 投票**（sli.do風）。Next.js + Spring Boot。認証は Keycloak OIDC → Cookie Session（Redis）。STOMP。PostgreSQL。

| 項目 | 内容 |
|------|------|
| フロント | Next.js App Router / TS（Member A） |
| バック | Spring Boot / Java 21（B 主・C 支援） |
| 認証 | Keycloak → Spring Session + Redis（`SESSION`） |
| リアルタイム | WebSocket / STOMP |
| DB | PostgreSQL |

| Member | 領域 |
|--------|------|
| A | Next.js・SSR・STOMP 受信 UI → [frontend/AGENTS.md](frontend/AGENTS.md) |
| B | OIDC→Session・RBAC/CSRF・CRUD・QueryDSL → [backend/AGENTS.md](backend/AGENTS.md) |
| C | STOMP 配信・Compose・CI → backend AGENTS + docs |

| ロール | 利用 |
|--------|------|
| `HOST` | Keycloak ログイン主催者 |
| 参加者（匿名） | SSO不要。匿名 Session + `voter_key` |

MVP-A（F-01〜F-09）が死守。日程・F-xx 詳細は docs。権限の正は API。

## 設計ドキュメント（正）

索引: [docs/README.md](docs/README.md)

| doc | 内容 |
|-----|------|
| [scope](docs/scope.md) | MVP-A/B・ストレッチ |
| [roles](docs/roles.md) | 役割・F-xx |
| [schedule](docs/schedule.md) | 14日・カレンダー版 |
| [architecture](docs/architecture.md) | アーキ |
| [service-spec](docs/design/service-spec.md) | サービス仕様 |
| [screen-transition](docs/design/screen-transition.md) | 画面 |
| [websocket-sequence](docs/design/websocket-sequence.md) | シーケンス |
| [er-diagram](docs/design/er-diagram.md) | ER / Flyway案 |
| [constraints](docs/design/constraints.md) | 制約 |
| [openapi-notes](docs/api/openapi-notes.md) | REST / STOMP |

学習メモ: [docs/qa/README.md](docs/qa/README.md)

## ルール 6本（役割固定）

| # | ファイル | 役割 | 適用 |
|---|----------|------|------|
| 1 | [live-qa-project.mdc](.cursor/rules/live-qa-project.mdc) | OS・優先順位・スコープ | always |
| 2 | [live-qa-keywords.mdc](.cursor/rules/live-qa-keywords.mdc) | 実装/解説/確認/修復/PR案作成 | always |
| 3 | [live-qa-learning-explain.mdc](.cursor/rules/live-qa-learning-explain.mdc) | 写経言語化・【EJ】 | always |
| 4 | [live-qa-learning-memo.mdc](.cursor/rules/live-qa-learning-memo.mdc) | `docs/qa/`・PR整理 | always |
| 5 | [live-qa-backend.mdc](.cursor/rules/live-qa-backend.mdc) | Java形の正 | glob |
| 6 | [live-qa-frontend.mdc](.cursor/rules/live-qa-frontend.mdc) | フロント確定方針 | glob |

キーワード: `実装` `解説` `確認` `修復` `PR案作成`。  
**実装・修復は本人が手書き（厳守）。** Agent は写経＋テストセット＋解説のみ。ソース自動編集は「Agentがファイルを編集してよい」明示時だけ（[keywords](.cursor/rules/live-qa-keywords.mdc)）。

## Codex（ChatGPT）

Cursor は上記 6 本の `.mdc` を自動注入する。**Codex は `AGENTS.md` しか自動で読まない。**  
作業開始時に 6 本を Read する。手順・本文の読み口: [docs/codex.md](docs/codex.md)。規範の正は `.mdc`（6本のみ・増やさない）。

## 参照リポジトリ

- `../LibraShare`（Keycloak SSO・Spring Security 資産の再利用可）
