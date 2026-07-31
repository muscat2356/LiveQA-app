# 学習 Q&A 索引

概念・トラブルシュートの質問と回答を残す場所。  
Agent は回答のたびにここへ **新規作成または追記**する（`.cursor/rules/live-qa-learning-memo.mdc`）。

設計の正は従来どおり `docs/` 配下の architecture / openapi 等。**ここは JIT 学習メモ**。矛盾があれば設計ドキュメントを優先する。

## 本番環境（Compose）のメモ

| ファイル | 内容 |
|----------|------|
| [09-keycloak-realm-librashare.md](09-keycloak-realm-librashare.md) | **LibraShare realm への相乗り**（realm JSON・サードパーティー・最終的な Keycloak 共有イメージ / URL 2種） |
| [10-get-api-events.md](10-get-api-events.md) | **GET /api/events**（Flyway〜一覧の写経順・EJ・データの流れ） |
| [11-jpa.md](11-jpa.md) | **JPA 解説**（仕様と Hibernate / Entity・Repository・Flyway 分担） |
| [12-delete-strategy.md](12-delete-strategy.md) | **削除方針**（物理 DELETE が少ない理由 / User は残す / status が論理相当） |
| [13-unique-name-and-test-deps.md](13-unique-name-and-test-deps.md) | **UNIQUE 制約の命名** / **テスト依存**（レビュー指摘の解説） |

> ラボ時代のメモ（04〜06）は Keycloak `8081` / realm `live-qa` 前提で書かれている。本番は Keycloak `8080` / realm `LibraShare`（09 参照）。

## session-lab から持ち込んだメモ（F-01 素振り）

出典: [`../live-qa-session-lab/docs/qa/`](../../../live-qa-session-lab/docs/qa/)（Cookieセッション + Redis / OIDC 橋渡しのハンズオン）。

| ファイル | 内容 |
|----------|------|
| [00-transplant-from-lab.md](00-transplant-from-lab.md) | **移植判断**（持ち込む／持ち込まない／ハマりどころ） |
| [08-backend-project-migrate.md](08-backend-project-migrate.md) | **backend プロジェクト移行手順**（Maven 骨組み・差分・起動確認） |
| [01-overview.md](01-overview.md) | 全体像・ハイブリッド認証 |
| [02-session-cookie.md](02-session-cookie.md) | セッション / Cookie / SameSite |
| [03-redis.md](03-redis.md) | Redis・キー・namespace |
| [04-environment.md](04-environment.md) | Docker / Keycloak / 起動切り分け（ラボ環境の話が多い） |
| [05-security-config.md](05-security-config.md) | oauth2Login / CSRF / mapper 差し込み |
| [06-me-controller.md](06-me-controller.md) | `/api/me`・認可の流れ・principal vs Authentication |
| [07-guest-session.md](07-guest-session.md) | 匿名セッション / `voter_key`（余力） |

### コード移植時に特に見る

1. [00-transplant-from-lab.md](00-transplant-from-lab.md) — 一覧判断  
2. [08-backend-project-migrate.md](08-backend-project-migrate.md) — backend へのファイル移行手順  
3. [05-security-config.md](05-security-config.md) — `userAuthoritiesMapper` 必須・Bean だけでは不足  
4. [06-me-controller.md](06-me-controller.md) — role は `Authentication` から読む  

ラボ専用 API（`POST /api/lab/ping`）や Compose / realm JSON の丸コピーは移植対象外（00 参照）。
