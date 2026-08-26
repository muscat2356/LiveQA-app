# 運用（Keycloak / CI / Docker / エディタ）

F-09 寄りの作業メモ。認証の学習は [f01-auth.md](f01-auth.md)。Compose の正はリポジトリルート。

## Keycloak（LibraShare realm 相乗り）

専用 realm は作らない。**同じ realm に client `live-qa-app` を足す**（SSO・資産再利用）。

| 項目 | 値 |
|------|-----|
| realm | `LibraShare` |
| client | `live-qa-app`（confidential） |
| Keycloak | `http://localhost:8080` |
| backend（ホスト起動） | `http://localhost:8081`（KC と 8080 が被る） |
| 正の JSON | [`keycloak/Librashare-realm.json`](../../keycloak/Librashare-realm.json) |

追加したもの: client、realm role `HOST`、`admin_test@example.com` に HOST、`roles` mapper で **ID token に realm roles**。

`--import-realm` は **初回のみ**。既存ボリュームがあるときは JSON 変更だけでは入らない。

### URL が 2 種類ある理由

コンテナ内の `localhost:8080` は backend 自身。Keycloak に届かない。

| 用途 | ホスト名 | 誰 |
|------|----------|----|
| 認可 URL | `http://localhost:8080` | **ブラウザ** |
| トークン / JWK / userinfo | `http://keycloak:8080` | **backend コンテナ** |

ホスト `mvn` は `application.yml` の `issuer-uri`（localhost）。Compose は `application-docker.yml`。

`KC_HOSTNAME` はフル URL。healthcheck は `"status": "UP"`（大文字）。

IdToken に `realm_access` が無い → Client scopes（Clients の中ではない）→ `roles` → `realm roles` → Add to ID token ON。403 の定番。

## Swagger UI（#25）

ホスト起動（port 8081）の例:

| 何 | URL |
|----|-----|
| UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs` |

- **正は** [openapi-notes.md](../api/openapi-notes.md)。生成は閲覧用
- Cookie `SESSION`。状態変更は `X-XSRF-TOKEN`（`POST /api/auth/guest` だけ CSRF 検証しない）
- `POST /api/auth/logout` は Security の logoutUrl で、RestController ではないので UI に出ない
- STOMP は出ない → openapi-notes の STOMP 節
- F-11 の `POST /api/polls/{id}/responses` と `GET .../results` は [#109](https://github.com/muscat2356/live-qa-app/pull/109) マージ後に UI へ載る

## GitHub Actions（F-09）

PR / `main` / `develop` で Runner が checkout → Java 21 → `mvn test`。手元の「通った」を環境差で落とさないための門。CD（デプロイ）は MVP 必須ではない。正: `.github/workflows/ci.yml`。

### Q. テストが自動実行されるのは何故？

**誰かが毎回押しているのではなく、リポジトリに置いたレシピ（`.github/workflows/ci.yml`）が GitHub に「このタイミングで走らせて」と登録されているから。**

```yaml
on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main, develop]
```

| きっかけ（`on`） | いつ走るか |
|------------------|------------|
| `pull_request` | `main` / `develop` 向けの PR を開く・更新する（feature ブランチへの push で PR が更新されても走る） |
| `push` | `main` / `develop` 自体へ直接 push / merge したとき |

流れ:

```text
GitHub がイベントを検知（PR 更新など）
  → クラウド上の Runner（ubuntu）を用意
  → そのコミットを checkout
  → Java 21 を入れて `backend` で mvn -B test
  → PR に ✓ / ✗ が付く
```

**止めるには** YAML の `on:` を変える（または workflow を disable）。コードの `@Test` があるから自動起動するわけではない。テストが「ある」ことと、CI が「いつ回すか」は別。

F-09 の意図: 自分の PC だけで通っても、他人・別 OS で落ちることがある。PR の門として同じ `mvn test` を GitHub 側で回す。

## Docker backend が Bean 無しで落ちる

`EventService` 等に `@Service` が無いと DI 失敗でコンテナが起動しない。アノテーション忘れを疑う。

## ChatGPT Codex

Cursor と同じルール 6 本を Codex で使う手順は [`docs/codex.md`](../codex.md)。`.cursor/rules/` は増やさない。Codex は `.mdc` を自動注入しない。

## `.vscode/settings.json` の個人パス

リポジトリに自分のホームパスを置かない。User 設定へ移すか、ワークスペースは相対・変数にする。チームの Java 設定（formatter 等）だけ残す。
