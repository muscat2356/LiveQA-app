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
| [14-f01-finish.md](14-f01-finish.md) | **F-01 仕上げ**（JIT・`/api/me` ローカル id・logout）＋ **MeController 動作確認手順** |
| [15-jit-provisioning.md](15-jit-provisioning.md) | **JIT プロビジョニング**（何をするか / なぜ / データの流れ） |
| [16-userservice-when-oidcsubject.md](16-userservice-when-oidcsubject.md) | **UserService の動くタイミング** / `oidcSubject` の出所 |
| [17-host-displayname-idp.md](17-host-displayname-idp.md) | **HOST 表示名**（IdP 日本語・ローカル列は要るか） |
| [18-delegate-loaduser.md](18-delegate-loaduser.md) | **`delegate.loadUser`**（標準 OidcUserService への委譲） |
| [19-dto-record.md](19-dto-record.md) | **Response/Request は record か** |
| [20-github-actions-ci.md](20-github-actions-ci.md) | **GitHub Actions CI**（backend `mvn test`） |
| [21-response-entity.md](21-response-entity.md) | **ResponseEntity を使う／使わない** |
| [22-var-vs-immutable.md](22-var-vs-immutable.md) | **`var` とイミュータブル（EJ）は別軸** |
| [23-dto-mapping-layer.md](23-dto-mapping-layer.md) | **DTO 変換は Service か Controller か（三層）** |
| [24-test-deps-jacoco.md](24-test-deps-jacoco.md) | **テスト依存・JaCoCo**（今あるもの／後から） |
| [25-userservice-unit-test.md](25-userservice-unit-test.md) | **UserService / MeController テスト**（Mock・oidcLogin・OAuth2 exclude） |
| [26-securityconfig-data-flow.md](26-securityconfig-data-flow.md) | **SecurityConfig 流れ／メソッド／型カタログ**（+ Bruno token・IdToken に role 無し・mapper は loadUser の外 等 Q&A） |

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
