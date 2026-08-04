# LibraShare realm への相乗り（サードパーティークライアント化）

live-qa-app 専用の realm を作るのではなく、**LibraShare の realm をそのまま使い、live-qa-app を「もう一つのクライアント」として登録**する構成に変更した。

## なぜこうするか

| 観点 | 理由 |
|------|------|
| SSO の実感 | 同じ realm = 同じログインセッション。LibraShare でログイン済みなら live-qa-app は再ログイン不要 |
| 資産の再利用 | LibraShare の realm export（user / role / client）を捨てずに使える |
| 学習テーマ | Member B の主役は「OIDC → Cookie セッション」。realm を新設する手間を削れる |

realm を分けると「別の IdP にログインし直す」形になり、サードパーティー（第三者アプリが同じ認可サーバを使う）という形にならない。

## 用語の正体

| 用語 | 何か |
|------|------|
| **realm** | Keycloak の**テナント境界**。user / role / client の入れ物。realm が違えばセッションも別 |
| **client** | realm に登録された**アプリ1つ**の設定（redirect URI / secret / フロー）。live-qa-app はここに増える |
| **confidential client** | client secret を持つ種類。サーバ（backend）がトークン交換するときに使う |
| **realm role** | realm 全体で使えるロール。今回追加した `HOST` はこれ |
| `--import-realm` | 起動時に `/opt/keycloak/data/import` の realm JSON を**取り込む**オプション |

## realm JSON に足したもの（既存は温存）

正は [`keycloak/Librashare-realm.json`](../../keycloak/Librashare-realm.json)。**既存 client（`front-client` / `back-admin` / `account` など）と user は一切消していない**。追加は次の3点だけ。

1. **client `live-qa-app`** … confidential。redirect URI は `http://localhost:8081/*`（backend）と `http://localhost:3000/*`（Next.js）
2. **realm role `HOST`** … live-qa-app の主催者ロール
3. **`admin_test@example.com` に `HOST` を付与** … 既存ユーザーの realmRoles に追記（新規ユーザーは作らない）
4. **client scope `roles` → mapper `realm roles`** … `id.token.claim` / `userinfo.token.claim` を `true`（backend の `GrantedAuthoritiesMapper` が IdToken の `realm_access` を読むため。既定は access token のみ）

> **注意:** `--import-realm` は **初回（realm 未作成時）** に効く。すでに Keycloak のボリュームに LibraShare がある場合は JSON 変更だけでは反映されない。  
> 反映手順: Admin で同等設定を直す / または `docker compose down` 後に postgres volume を消して再 import（**データ消える**ので注意）。

取り込み後の確認結果:

```text
clients: account, account-console, admin-cli, back-admin, broker,
         front-client, live-qa-app, realm-management, security-admin-console
roles  : default-roles-librashare, general_employee, admin_employee,
         uma_authorization, HOST, general_user, offline_access
```

## データの流れ（ログイン時）

```text
ブラウザ → GET http://localhost:8081/oauth2/authorization/keycloak
  → 発行: backend が 302 を返し、Keycloak の認可 URL へ送る
    （http://localhost:8080/realms/LibraShare/... client_id=live-qa-app）
  → ユーザーが LibraShare realm のログイン画面で認証
  → 交換: backend が認可コードをトークンに取り換える
    （このリクエストはコンテナ内なので http://keycloak:8080 を使う）
  → 変換: realm_access.roles の HOST を ROLE_HOST へ読み替える（mapper は実装予定）
  → 格納: SecurityContext を Redis セッションへ書き込み、SESSION Cookie を発行
```

## URL が2種類ある理由（ハマりどころ）

コンテナ内から `localhost:8080` を呼ぶと、それは**backend 自身**を指すので Keycloak に届かない（`Connection refused` で起動失敗した）。そのため役割で分けている。

| 用途 | ホスト名 | 誰が使うか |
|------|----------|-----------|
| 認可 URL（`authorization-uri`） | `http://localhost:8080` | **ブラウザ**がアクセスする |
| トークン / JWK / userinfo | `http://keycloak:8080` | **backend コンテナ**がアクセスする |

設定は [`backend/src/main/resources/application-docker.yml`](../../backend/src/main/resources/application-docker.yml)（profile `docker`）。ホストで `mvn` 起動するときは `application.yml` 側の `issuer-uri`（`http://localhost:8080/realms/LibraShare`）が使われる。

## Compose の変更点

| 変更 | 内容 |
|------|------|
| import マウント | `./keycloak:/opt/keycloak/data/import`（LibraShare の export をそのまま入れる） |
| `KC_HOSTNAME` | `http://localhost:8080` … **フル URL 必須**。ホスト名だけだと `hostname-backchannel-dynamic must be set to false if hostname is not provided as full URL` で起動失敗する |
| healthcheck | `/health/ready` の応答は `"status": "UP"`（**大文字**）。小文字で grep すると永遠に unhealthy → backend が `Waiting` のまま止まる |
| client id / secret | `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET`（開発用の既定値を compose に置き、`.env` で上書き可） |

## 残っている作業

1. `realm_access.roles` → `ROLE_HOST` の **mapper**（`userAuthoritiesMapper`）を backend に実装 → [05-security-config.md](05-security-config.md)
2. ホスト起動時はポートを 8081 にずらす（Keycloak が 8080 を使う）
3. 試行中に作られた未使用 realm `live-qa-app` が Keycloak に残っている。admin コンソール（`admin` / `admin`）から削除してよい

## ラボ時代のメモとの差分

`04-environment.md` / `05-security-config.md` / `06-me-controller.md` は **ラボ（session-lab）時代**の値で書かれている。

| | ラボ | 本番（現在） |
|--|------|--------------|
| Keycloak | `localhost:8081` | `localhost:8080` |
| backend | `localhost:8080` | `localhost:8081` |
| realm | `live-qa` | `LibraShare` |

---

## Q: realm JSON とは？サードパーティーとは？最終的に LibraShare コンテナへ合体するイメージ？

### realm JSON の正体

`keycloak/Librashare-realm.json` は **Keycloak の設定スナップショット（エクスポート）**。  
Keycloak 本体（コンテナ）そのものではなく、「この realm に何が入っているか」を JSON で書いた設計図。

| JSON の主な中身 | 意味 |
|----------------|------|
| `"realm": "LibraShare"` | テナント名。ログイン画面・issuer URL に出る |
| `users` | ログインできる人（例: `admin_test@example.com`） |
| `roles.realm` | realm 全体のロール（`admin_employee` / **`HOST`** など） |
| `clients` | この realm を使う**アプリごとの登録**（`front-client` / `back-admin` / **`live-qa-app`**） |

`--import-realm` は起動時にこの設計図を Keycloak の DB へ **取り込む**操作。  
取り込み先は「いま動いている Keycloak コンテナ」の Postgres。JSON を編集しても、既に import 済みの realm は `IGNORE_EXISTING` で**上書きされない**（初回だけ効く）。

### サードパーティー（第三者クライアント）の意味

ここでの「サードパーティー化」＝ **認可サーバ（IdP）は LibraShare の realm のまま、live-qa-app を別アプリとして client 登録する**こと。

```text
Keycloak（IdP・認可サーバ）
  └── realm: LibraShare
        ├── client: front-client     ← LibraShare フロント（既存）
        ├── client: back-admin       ← LibraShare 管理 API（既存）
        ├── client: live-qa-app      ← live-qa-app backend（追加・第三者）
        ├── role: HOST               ← live-qa 用に追加
        └── users: admin_test@... など（既存＋ HOST 付与）
```

| 用語 | この場所での意味 |
|------|------------------|
| **第一者** | IdP を運営する側（LibraShare / Keycloak の realm） |
| **サードパーティー（第三者クライアント）** | 同じ realm に乗る**別アプリ**。live-qa-app はここに当たる |
| **client** | アプリ1つの契約（redirect URI / secret / どのフローを許可するか） |

流れ（意味つき）:

1. **受け取り** → ブラウザが live-qa-app の「ログイン」を叩く  
2. **発行** → backend が Keycloak の認可 URL（`client_id=live-qa-app`）へ 302  
3. **検証** → Keycloak が LibraShare realm のユーザー／パスワードを確かめる  
4. **交換** → 認可コードをトークンに取り換える（backend ↔ Keycloak）  
5. **格納** → live-qa-app 側は Cookie セッション（Redis）にログイン結果を載せる  

LibraShare の既存 client を消さないのは、「同じ IdP に複数アプリがぶら下がる」状態を保つため。

### 「最終的に LibraShare のコンテナに合体」のイメージは？

**半分 YES、半分 NO。**

| 合体するもの | イメージ | 理由 |
|--------------|----------|------|
| **Keycloak（+ その Postgres）** | ✅ 最終的に **1つに寄せる** | 同じ realm JSON を別インスタンスにコピーしただけでは **SSO セッションは共有されない**。本当の相乗り＝**同じ Keycloak プロセス**を見る |
| **live-qa-app の backend / frontend / Redis** | ❌ LibraShare の Compose に丸ごと飲み込まれない | アプリは別リポジトリ・別ポートのまま。Keycloak の **client として接続**するだけ |
| **realm JSON** | ✅ LibraShare 側の import に **client `live-qa-app` をマージして正とする** | 設計図の正を1か所にするとメンテしやすい |

いま（開発中）:

```text
live-qa-app Compose          LibraShare Compose
  Keycloak ← realm JSON コピー   Keycloak（別プロセス）
  backend / redis / ...          backend / frontend / ...
```

→ 定義は似ているが **インスタンスが別**。ポートも 8080/5432 で衝突する。Day2–4 の素振りには十分。

完了イメージ（推奨）:

```text
          ┌─ Keycloak（1つ・LibraShare realm）← 正の realm JSON
          │
LibraShare backend/front ── client: front-client / back-admin
live-qa-app backend/front ── client: live-qa-app
```

やり方の例:

1. LibraShare の Keycloak を起動したままにする  
2. live-qa-app の Compose から **keycloak（と Keycloak 用 DB）を外す**（またはプロファイルで起動しない）  
3. live-qa-app backend の `issuer-uri` / token-uri を **その共有 Keycloak** に向ける  
4. realm JSON の正を LibraShare リポジトリ（または共有置き場）に置き、`live-qa-app` client をマージ済みにする  

「合体」＝アプリを1つの Docker プロジェクトに統合する、ではない。  
**合体するのは IdP（Keycloak）と realm の設計図。アプリはサードパーティーのまま別コンテナで動く。**

---

## Q: 実装環境はこのまま、本番では Keycloak を独立させる認識でよいか？

### 結論

**だいたいその認識でよい。**  
ただし「独立」＝ **live-qa 専用の別 realm を新設する**ではなく、**IdP（Keycloak）をアプリ Compose から切り出して共有インフラにする**、という意味。

### 段階ごとの絵

| 段階 | Keycloak の置き方 | 目的 |
|------|-------------------|------|
| **いま（実装・学習）** | live-qa-app の Compose 内に Keycloak あり。LibraShare realm JSON をコピーして import | 素振りを単独で完結させる。ポート衝突に注意すれば OK |
| **仕上げ・デモ／本番相当** | Keycloak を **アプリから独立した1プロセス**にする（LibraShare 側の Keycloak を共有、または IdP 専用 Compose） | 本当の SSO・サードパーティー体験。live-qa は client として接続するだけ |

```text
【実装中】
live-qa Compose: postgres / redis / keycloak / backend / (frontend)
  ※ realm 定義は LibraShare のコピー。インスタンスは別 → SSO は擬似

【仕上げ・本番相当】
共有 Keycloak（独立）── realm LibraShare（正の JSON・live-qa-app client 込み）
   ↑                          ↑
LibraShare アプリ           live-qa-app アプリ
（Compose から Keycloak を外すか、起動しない）
```

### 「独立」でやること／やらないこと

| | 内容 |
|--|------|
| ✅ やること | live-qa の Compose から keycloak（と Keycloak 用 DB）を外す／別起動にする。backend の issuer / token URL を共有 Keycloak に向ける |
| ✅ やること | realm JSON の正を1か所に（client `live-qa-app` マージ済み） |
| ❌ やらないこと | 本番だからといって realm を `live-qa-app` に戻して IdP を分ける（サードパーティー方針と逆） |
| ❌ やらないこと | live-qa の backend/frontend を LibraShare リポジトリに丸ごと統合する必要はない |

### このプロジェクトでの「本番」

14日ポートフォリオではクラウド本番サーバが無くても、**デモ・仕上げの目標構成＝上表の「仕上げ・本番相当」**と読んでよい。  
実装フェーズ（F-01 素振り〜MVP-A）は **今の Compose 内 Keycloak のままで問題ない**。合体（共有 IdP）は、認証が通ったあと・両アプリを並べて見せるタイミングでよい。