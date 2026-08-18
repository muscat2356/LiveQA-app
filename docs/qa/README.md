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
| [27-post-api-events.md](27-post-api-events.md) | **POST /api/events**（作成・写経順） |
| [29-vscode-settings-personal-path.md](29-vscode-settings-personal-path.md) | **`.vscode/settings.json` 個人パス指摘**（残す／User へ移す） |
| [30-event-create-design-choices.md](30-event-create-design-choices.md) | **Event 作成の設計判断**（StringBuilder / @Valid / 404） |
| [31-login-success-redirect.md](31-login-success-redirect.md) | **ログイン後リダイレクト**（Member A 連携・dashboard） |
| [32-cookie-cross-origin-next.md](32-cookie-cross-origin-next.md) | **Cookie 跨ぎと Next BFF**（CORS との違い・転送） |
| [33-bruno-session-get.md](33-bruno-session-get.md) | **Bruno + SESSION で GET 確認**（`/api/me`） |
| [34-preauthorize-500-via-advice.md](34-preauthorize-500-via-advice.md) | **@PreAuthorize×Advice 500**／Handler 分割 vs if |
| [35-csrf-header-how-to-define.md](35-csrf-header-how-to-define.md) | **CSRF ヘッダ検証**（メソッド定義は不要・Filter／テスト） |
| [36-question-count-jpa.md](36-question-count-jpa.md) | **questionCount**（COUNT vs スナップショット列・CASCADE） |
| [37-list-without-question-count.md](37-list-without-question-count.md) | **一覧に questionCount は必要か**（UI vs openapi） |
| [41-docker-backend-no-service-bean.md](41-docker-backend-no-service-bean.md) | **Docker backend 起動失敗**（`@Service` 無しで DI 失敗） |
| [42-post-confirm-headers.md](42-post-confirm-headers.md) | **POST 確認時ヘッダー**（SESSION / XSRF / Content-Type） |
| [43-post-403-server-side.md](43-post-403-server-side.md) | **POST 403 サーバー側切り分け**（CSRF message 分離） |
| [44-post-events-500-joincode-regex.md](44-post-events-500-joincode-regex.md) | **POST /api/events 500**（JoinCode 正規表現 typo） |
| [45-f02-event-tests.md](45-f02-event-tests.md) | **F-02 Event テスト解説**（Service / WebMvcTest） |
| [46-jpa-managed-save-optional.md](46-jpa-managed-save-optional.md) | **JPA 管理下で save 省略可の理由** |
| [47-f03-status-review.md](47-f03-status-review.md) | **F-03 status 変更レビュー**（遷移・docs照合・残課題） |
| [48-owned-event-sql-404.md](48-owned-event-sql-404.md) | **所有者 SQL+404**（openapi/constraints 反映済み） |
| [49-f04-guest-voter-key.md](49-f04-guest-voter-key.md) | **F-04 匿名セッション / voter_key**（到達点・写経順） |

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

---

## 質問に出てきた用語一覧（グロッサリ）

`docs/qa` の **Q / 質問** 見出しから拾った単語。詳細はリンク先。五十音・英字はざっくりカテゴリ分け。

### 認証・OIDC / Keycloak

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| IdP | 認証を担う側（Keycloak） | [05](05-security-config.md) [09](09-keycloak-realm-librashare.md) |
| OIDC / oauth2Login | ブラウザログインでトークンを取る流れを有効化する設定 | [05](05-security-config.md) |
| Authorization Code | 認可コード。トークンそのものではない短い許可証 | [05](05-security-config.md) |
| トークン交換 | コードを IdP に渡し ID/Access トークンを得る | [05](05-security-config.md) |
| ID Token / Access Token | ログイン材料（ID）／API用（Access）。F-01 以降の媒体は Session | [26](26-securityconfig-data-flow.md) |
| realm / realm JSON | Keycloak のテナント境界／その設定のエクスポート | [09](09-keycloak-realm-librashare.md) |
| サードパーティー（client） | 既存 realm に載せる別アプリ（`live-qa-app`） | [09](09-keycloak-realm-librashare.md) |
| `realm_access` / realm role | トークン内の realm ロール袋。`HOST` など | [26](26-securityconfig-data-flow.md) |
| `sub` / oidcSubject | IdP が付与するユーザー一意 ID | [16](16-userservice-when-oidcsubject.md) |
| JIT | 初回ログイン時にローカル `users` へ行を作る | [15](15-jit-provisioning.md) |
| `provisionHost` | JIT の実装メソッド | [16](16-userservice-when-oidcsubject.md) [26](26-securityconfig-data-flow.md) |
| RP-initiated logout / `{baseUrl}` | アプリ起点のログアウト。`{baseUrl}` は backend オリジン | [14](14-f01-finish.md) |
| Bruno / token endpoint | 手動でトークンを取る確認用（Bearer だけでは `/api/me` 不可） | [26](26-securityconfig-data-flow.md) |

### セッション・Cookie・Redis

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| SESSION Cookie | セッション箱の**鍵**（中身ではない） | [02](02-session-cookie.md) [05](05-security-config.md) |
| Redis / namespace | セッション箱の置き場。キー接頭辞 | [03](03-redis.md) [05](05-security-config.md) |
| SecurityContext | リクエスト中の認証状態の箱 | [05](05-security-config.md) [06](06-me-controller.md) |
| HttpSession | サーバ側セッション。永続先が Redis | [05](05-security-config.md) |
| `SessionRepositoryFilter` | SESSION→Redis で HttpSession を復元／格納する Filter | [05](05-security-config.md) |
| `SecurityContextHolderFilter` / `HttpSessionSecurityContextRepository` | Session 属性 ↔ SecurityContext の読み戻し（毎リクエスト） | [05](05-security-config.md) |
| `securityFilterChain` メソッド | **起動時**に Filter 列を組み立てる `@Bean`。復元本体ではない | [05](05-security-config.md) |
| guest / `voter_key` | 匿名参加者。`g:{id}` 形式 | [07](07-guest-session.md) |

### CSRF・認可

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| CSRF / `XSRF-TOKEN` | 状態変更のなりすまし防止。合い言葉 Cookie＋ヘッダ | [05](05-security-config.md) |
| `csrfRepo` / `csrfHandler` | CSRF 用のリポジトリとリクエスト処理（自前 `new`） | [05](05-security-config.md) |
| `GrantedAuthoritiesMapper` / mapper | IdP ロールを `ROLE_*` に変換するフック | [05](05-security-config.md) [26](26-securityconfig-data-flow.md) |
| `userAuthoritiesMapper` | mapper を oauth2Login に**接続**する DSL | [05](05-security-config.md) |
| `ROLE_HOST` / `hasRole` / `@PreAuthorize` | 認可用の権限文字列／式 | [06](06-me-controller.md) |
| Authentication / principal / OidcUser | 認証全体／主体／OIDC クレーム保持 | [06](06-me-controller.md) |
| `UNKNOWN`（role） | 旧実装で authorities に HOST が無いときの応答。現行は DB `role` | [06](06-me-controller.md) [14](14-f01-finish.md) |
| EntryPoint / AccessDenied | 未認証 401／権限不足 403 を Filter 層で書く | [14](14-f01-finish.md) [26](26-securityconfig-data-flow.md) |

### ログイン処理の部品

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| `OidcUserRequest` / `userRequest` | トークン交換後の材料。Spring が渡す | [18](18-delegate-loaduser.md) |
| `delegate.loadUser` | 標準 `OidcUserService` への委譲 | [18](18-delegate-loaduser.md) |
| `oidcUserService`（カスタム） | 標準の前後に JIT を足すラッパ | [16](16-userservice-when-oidcsubject.md) |
| `loadUser` と mapper の順序 | loadUser（＋JIT）の**後**に Provider が mapper | [26](26-securityconfig-data-flow.md) |

### JPA・DTO・レイヤ

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| JPA / Entity / Hibernate | オブジェクト↔行の仕様／実装 | [11](11-jpa.md) |
| `hostOf` / 静的ファクトリ / Builder | HOST 行生成の入り口。Builder ではない | [11](11-jpa.md) |
| カプセル化（protected コンストラクタ） | 勝手な `new` を防ぎ生成経路を固定 | [11](11-jpa.md) |
| record / DTO | 不変の応答形。Service で組み立て | [19](19-dto-record.md) [23](23-dto-mapping-layer.md) |
| `ResponseEntity` | Controller が HTTP 箱に載せる | [21](21-response-entity.md) |
| `var` / イミュータブル（EJ） | 型推論と不変性は別軸。明示型優先 | [22](22-var-vs-immutable.md) |
| UNIQUE 制約名 | Flyway と `@UniqueConstraint` を揃える | [13](13-unique-name-and-test-deps.md) |

### テスト・CI・環境

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| `@WebMvcTest` / Mockito / `oidcLogin` | Controller／Service の単体テスト手段 | [25](25-userservice-unit-test.md) |
| `src/test` | 自動生成されない。自分で置く | [24](24-test-deps-jacoco.md) |
| JaCoCo | カバレッジ計測（後からでも可） | [24](24-test-deps-jacoco.md) |
| GitHub Actions / `ci.yml` | PR 等で `mvn test` などを回す | [20](20-github-actions-ci.md) |
| issuer / ポート衝突 | Keycloak 未起動や 8080 競合の切り分け | [04](04-environment.md) |

### 表示名・その他

| 用語 | 一言 | 主な出典 |
|------|------|----------|
| displayName | 応答の表示名。クレーム由来で DB 列ではない（現行） | [16](16-userservice-when-oidcsubject.md) [17](17-host-displayname-idp.md) |
| UTF-8 / `getWriter()` | Filter 直書き JSON は encoding を先に | [14](14-f01-finish.md) |
