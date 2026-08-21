# 学習 Q&A 索引

概念・トラブルシュートの質問と回答。**機能（F-xx）ごとに 1 ファイル。** 新しい説明は既存の機能メモへ追記する（番号連番の新規ファイルは増やさない）。

設計の正は `docs/` の architecture / openapi / ER / constraints。**ここは JIT 学習メモ。** 到達点（何がコードにあるか）は各 `f0x`。未実装の契約は消さず、実装で合わせる。

## 機能メモ（ここを開く）

| ファイル | 機能 |
|----------|------|
| [f01-auth.md](f01-auth.md) | **F-01** 認証・セッション（JIT / `/api/me` / CSRF / Cookie） |
| [f02-events.md](f02-events.md) | **F-02** ルーム作成・一覧・Join 照会 |
| [f03-status.md](f03-status.md) | **F-03** ルーム開閉・所有者 404 |
| [f04-guest.md](f04-guest.md) | **F-04** 匿名セッション / `voter_key` |
| [f05-questions.md](f05-questions.md) | **F-05** 質問投稿・いいね |
| [f08-status.md](f08-status.md) | **F-08** 質問ステータス（回答済み / アーカイブ） |

## 横断

| ファイル | 内容 |
|----------|------|
| [java.md](java.md) | 三層・DTO record・JPA・Javadoc 粒度・`var`・UNIQUE・削除・テスト |
| [ops.md](ops.md) | Keycloak 相乗り・CI・Docker・エディタ |

F-06 / F-07 は実装が始まったら同型で `f06-*.md` / `f07-*.md` を足す。

## ラボ移植アーカイブ（深いカタログ）

出典: session-lab。本番の正は上の機能メモ。Filter 列の全文などは残している。

| ファイル | 内容 |
|----------|------|
| [00-transplant-from-lab.md](00-transplant-from-lab.md) | 移植判断 |
| [08-backend-project-migrate.md](08-backend-project-migrate.md) | backend プロジェクト移行 |
| [01-overview.md](01-overview.md) | 全体像・ハイブリッド認証 |
| [02-session-cookie.md](02-session-cookie.md) | セッション / Cookie / SameSite |
| [03-redis.md](03-redis.md) | Redis・namespace |
| [04-environment.md](04-environment.md) | Docker / Keycloak（ラボ環境の話が多い。ポートは 09 当時） |
| [05-security-config.md](05-security-config.md) | oauth2Login / CSRF / mapper |
| [06-me-controller.md](06-me-controller.md) | `/api/me`・principal vs Authentication |
| [07-guest-session.md](07-guest-session.md) | 匿名セッション素振り（正は [f04](f04-guest.md)） |
| [26-securityconfig-data-flow.md](26-securityconfig-data-flow.md) | SecurityConfig 型カタログ・Q&A |

ラボ当時は Keycloak `8081` / realm `live-qa` 前提の記述が残る。本番は Keycloak `8080` / realm `LibraShare`（[ops](ops.md)）。

---

## 用語（グロッサリ）

### 認証・OIDC / Keycloak

| 用語 | 一言 | 出典 |
|------|------|------|
| IdP / OIDC / oauth2Login | Keycloak でログインし Session にする | [f01](f01-auth.md) [05](05-security-config.md) |
| realm / client | テナント境界／アプリ1つの設定。`live-qa-app` は相乗り | [ops](ops.md) |
| `sub` / oidcSubject / JIT | IdP の一意 ID。初回ログインで `users` 行を作る | [f01](f01-auth.md) |
| `realm_access` / mapper | IdToken のロール袋 → `ROLE_HOST` | [f01](f01-auth.md) [26](26-securityconfig-data-flow.md) |

### セッション・Cookie

| 用語 | 一言 | 出典 |
|------|------|------|
| SESSION / Redis | 箱の鍵と置き場 | [f01](f01-auth.md) [02](02-session-cookie.md) [03](03-redis.md) |
| guest / `voter_key` | 匿名参加者。`g:{id}` / `u:{id}` | [f04](f04-guest.md) |
| Cookie 跨ぎ | CORS とは別。BFF 時の Cookie 転送 | [f01](f01-auth.md) |

### CSRF・認可

| 用語 | 一言 | 出典 |
|------|------|------|
| CSRF / `X-XSRF-TOKEN` | POST 等のなりすまし防止。Controller には書かない | [f01](f01-auth.md) |
| `@PreAuthorize` / `ROLE_HOST` | 認可用。応答の `role` は DB | [f01](f01-auth.md) |
| 所有者 404 | SQL `id`+`host_id`。他人も無しも 404 | [f03](f03-status.md) |

### JPA・DTO

| 用語 | 一言 | 出典 |
|------|------|------|
| record / `ResponseEntity` | DTO は不変。Controller が HTTP 箱へ | [java](java.md) |
| ユーティリティクラス | `new` しない。static メソッドの置き場（EJ 項目4） | [java](java.md) |
| 静的ファクトリ / managed | Entity の入り口。同一 TX なら save 省略可 | [java](java.md) [f03](f03-status.md) |
| UNIQUE 名 | Flyway と `@UniqueConstraint` を揃える | [java](java.md) |
