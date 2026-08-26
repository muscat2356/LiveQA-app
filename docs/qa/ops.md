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

### Q. OCI 無料枠で Keycloak + Redis ごと Compose、SSO は通る？

**通る。マネージド IdP は不要。** ブラウザが見る URL を `localhost` から公開 IP（またはドメイン）に揃えるのが本体。トークン交換は今どおり `keycloak:8080` のまま。

手順の正本は [#44 の手順コメント 1本](https://github.com/muscat2356/live-qa-app/issues/44#issuecomment-5422566268) と [deploy-oci.md](deploy-oci.md)。

無料枠は **Ampere A1（`VM.Standard.A1.Flex`、目安 2 OCPU / 12GB 以上）** を使う。x86 の Always Free Micro（1GB）は Keycloak 26 + Postgres + Redis + Java 21 + Next で足りない。

SSO の役割分担（ローカルと同じ）:

```text
ブラウザ
  → Next :3000（画面）
  → Spring :8081（/oauth2/authorization/keycloak）
  → Keycloak :8080（ログイン画面・認可コード）
  → Spring が code を token-uri（コンテナ名 keycloak:8080）で交換
  → SESSION を Redis に格納、Cookie を発行
  → :3000/dashboard へ戻す
```

公開時に `localhost` を置換する箇所:

| 誰が見る | いま | 公開後（例 `http://203.0.113.10`） |
|----------|------|-------------------------------------|
| `KC_HOSTNAME` | `http://localhost:8080` | `http://203.0.113.10:8080` |
| `authorization-uri`（docker プロファイル） | `http://localhost:8080/.../auth` | 同じ公開ホスト `:8080` |
| `NEXT_PUBLIC_KEYCLOAK_LOGIN_URL` | `http://localhost:8081/oauth2/...` | `http://203.0.113.10:8081/oauth2/...` |
| `LOGIN_SUCCESS_REDIRECT_URI` / `POST_LOGOUT` / CORS | `http://localhost:3000` | `http://203.0.113.10:3000` |
| realm の redirect / webOrigins / post logout | `localhost:8081` と `:3000` | 同じ公開ホストの `:8081` と `:3000` を追加 |
| `token-uri` / `jwk-set-uri` / Redis host | `keycloak` / `redis` | **変えない**（Docker 内部） |

`--import-realm` はボリューム初回だけ。redirect を後から足すなら Admin Console か、`postgres_data` を消して再 import（データも消える）。

無料枠デモでやってよい割り切り:

- Keycloak は当面 `start-dev` のまま（`start` + HTTPS は後）
- Cookie `Secure=false`（HTTP）。HTTPS にしたら `true`
- フロントは `npm run dev` のまま（重いが追加作業が少ない）

無料枠でも閉じること:

- Security List は **3000 / 8081 / 8080** だけ（SSO に Keycloak の 8080 はブラウザが踏むので必要）
- **5432 / 6379 は publish しない**（または 0.0.0.0 に出さない）
- Keycloak Admin と client secret はデフォルトのまま公開しない
- Redis は volume が無いと再起動で SESSION が消える。デモが続くなら `appendonly` + volume を足す

ホスト名は **フロント・Spring コールバック・Keycloak で同じ文字列**（IP なら全部 IP）。Cookie はポートを見ないので `:3000` と `:8081` は同じホストなら SESSION を共有できる。IP とドメインを混ぜるとログイン後に 401 になる。

### Q. HTTPS にすると何が変わる？（フロント本番も）

**HTTPS は「ブラウザ ↔ 公開口」の暗号化。** コンテナ同士（Next→backend、Spring→Keycloak/Redis）は今どおり Docker 内部の HTTP でよい。証明書を Spring / Keycloak / Next の3つに配らない。前段に **Caddy（または nginx）を1つ**置き、80/443 だけ外に出す。

#### なぜ必要か

| 現象 | HTTP のまま | HTTPS |
|------|-------------|--------|
| 通信の盗み見 | パスワード・SESSION が平文 | TLS で隠せる |
| Cookie `Secure` | `false` でないとブラウザが捨てる | `true` にして初めて乗る |
| ブラウザの警告 | 「保護されていない」 | 鍵マーク |
| Let's Encrypt | 使えない（HTTP-01 は入口だけ） | Caddy が自動更新しやすい |

設計 docs も Cookie は `HttpOnly` / `Secure` / `SameSite`（[architecture](../architecture.md)）。いまのコードはローカル HTTP 用に **`Secure=false` であえて外している**。

#### データの流れ（無料枠の推奨形）

公開は **1ホスト名** に寄せる。ポート 3000/8081/8080 をそのまま HTTPS 化すると、サブドメイン分割で Cookie が割れやすい。

```text
ブラウザ
  HTTPS https://qa.example.com/          → Caddy → frontend:3000
  HTTPS https://qa.example.com/api/...     → Caddy または Next rewrite → backend:8080
  HTTPS https://qa.example.com/oauth2/...  → Caddy → backend:8080
  HTTPS https://auth.example.com/...       → Caddy → keycloak:8080
       （Keycloak はパス切りが苦手なのでサブドメインが楽）

Caddy が付与: X-Forwarded-Proto: https / X-Forwarded-Host: qa.example.com
Spring は server.forward-headers-strategy: framework で
  「自分は http://backend:8080」ではなく「公開 URL は https://qa.example.com」と復元
  → OIDC の {baseUrl}/login/oauth2/code/keycloak が https になる
```

Redis / Postgres / token-uri（`keycloak:8080`）は **変えない**。

#### 変更点と理由

| どこ | 変更 | 理由 |
|------|------|------|
| OCI Security List | **80 と 443 だけ**公開。3000/8081/8080/5432/6379 は閉じる | 証明書とアプリはプロキシ経由。DB を出さない |
| 新規 `caddy` サービス | TLS 終端。Let's Encrypt か、まず自己署名 | コンテナ3つに証明書を配らない |
| `SessionConfig` | `setUseSecureCookie(true)` | `Secure` 付き Cookie は HTTPS でしか送れない。HTTP のまま true にするとログインしても SESSION が乗らない |
| CSRF Cookie | リクエストが secure なら付く。プロキシの `X-Forwarded-Proto` が必要 | `request.isSecure()` が false のままだと `XSRF-TOKEN` も Secure にならず、または不一致 |
| `KC_HOSTNAME` | `https://auth.example.com` | ログイン画面・redirect の発行元。localhost のままだと SSO が戻らない |
| Keycloak コマンド | デモなら `start-dev` でも可。揃えるなら `start` + `KC_PROXY_HEADERS=xforwarded` + `KC_HTTP_ENABLED=true` | TLS は Caddy。Keycloak 自身は HTTP 待ち受け |
| `authorization-uri` | `https://auth.example.com/realms/.../auth` | **ブラウザ**が踏む。token/jwk は `http://keycloak:8080` のまま |
| realm redirect / webOrigins | `https://qa.example.com/*` | HTTP の redirect は HTTPS コールバックと一致しない |
| `LOGIN_SUCCESS` / `POST_LOGOUT` / CORS | `https://qa.example.com` | 着地とオリジン。CORS は同一オリジン化すれば実質不要 |
| `NEXT_PUBLIC_KEYCLOAK_LOGIN_URL` | `https://qa.example.com/oauth2/authorization/keycloak` | ログインボタン。**本番ビルド時に焼き込む**（後述） |
| SameSite | 同一ホストなら **Lax のまま** | フロントと API を別サブドメインにすると Lax では fetch に SESSION が付かない → 避けた方がよい |

【EJ】不変性 … 公開 URL は環境変数。コードに IP を書かない  
【EJ】あえて外す … ローカルは `Secure=false`。HTTPS 用プロファイルか環境で切り替える

#### フロントを本番に揃える

いまの `frontend/Dockerfile` は `CMD ["npm", "run", "dev"]`。開発サーバなので HMR・ソースマップ・メモリを食う。本番は **ビルドしてから起動**する。

| | `next dev`（いま） | `next build` + `next start` |
|--|---------------------|------------------------------|
| 役割 | 保存のたびに再コンパイル | 事前に最適化した成果物を配信 |
| `NEXT_PUBLIC_*` | 起動時に読めることが多い | **`next build` の瞬間の値を JS に埋め込む**。あとから compose の environment を変えてもクライアントには届かない |
| メモリ | 大きい | 小さい（無料枠向き） |

流れ:

```text
docker build 時
  ARG で NEXT_PUBLIC_KEYCLOAK_LOGIN_URL を渡す
  npm ci → next build  … ログイン URL をバンドルへ格納
コンテナ起動
  next start  … 配信。rewrite /api → http://backend:8080 はサーバ側なので内部 HTTP のまま
ブラウザ
  https://qa.example.com を開く
  fetch("/api/...", { credentials: "same-origin" })
  Cookie SESSION はホスト qa.example.com、Secure、HttpOnly
```

`next.config.ts` の rewrite は本番でも有効。ブラウザは `/api` だけ見え、backend ホストは外に出さない。

写経の骨（本人が `frontend/Dockerfile` へ書く。Agent は代筆しない）:

```dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_KEYCLOAK_LOGIN_URL
ENV NEXT_PUBLIC_KEYCLOAK_LOGIN_URL=$NEXT_PUBLIC_KEYCLOAK_LOGIN_URL
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./
EXPOSE 3000
CMD ["npm", "run", "start"]
```

compose では `build.args` に公開ログイン URL を渡し、`ports: "3000:3000"` は **Caddy 導入後は外す**（内部通信だけ）。

#### やりがちな失敗

1. Cookie だけ `Secure=true` にして HTTP で試す → SESSION が乗らない  
2. プロキシの `X-Forwarded-Proto` 漏れ → `{baseUrl}` が `http://backend:8080` → Keycloak が redirect 拒否  
3. フロントだけ HTTPS、Keycloak が HTTP → mixed content / リダイレクト不一致  
4. `NEXT_PUBLIC_*` を実行時 env だけ変える → 本番ビルドでは古い URL のまま  
5. `app.nip.io` と `api.nip.io` に分ける → SameSite=Lax でホスト Cookie が横断できない  

HTTP の公開 IP デモを先に通してから、Caddy + ドメイン + `Secure=true` + フロント本番ビルド、の順が安全。

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

### Q. Swagger の南京錠を SecurityConfig と揃えると？

**実行時の 401/403 は変わらない。** 変わるのは UI の鍵アイコンだけ。いまは `OpenApiConfig` の `.addSecurityItem(SESSION)` が全 operation に付く。

**方針（yaml 手書きは採らない）:** グローバル指定を外し、scheme の定義は残す。HOST 専用はクラスに `@SecurityRequirement(name = "SESSION")`。公開 GET / guest 発行は何も付けない。ゲスト Cookie が要る POST（質問・いいね・ポール回答）はメソッドに同じアノテーション。

`application.yml` の `springdoc:` は UI のパスだけ。LibraShare 式の `openapi.yaml` 正は notes と二重になるのでやめる。

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
