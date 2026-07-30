# 環境まわり（Docker / Keycloak / 起動失敗）

## Docker が WSL で見つからない

メッセージ例:

```text
The command 'docker' could not be found in this WSL 2 distro.
We recommend to activate the WSL integration in Docker Desktop settings.
```

### 原因

Docker Desktop は Windows 側にあるが、**WSL 連携が生きていない**ことが多い。

- `/usr/bin/docker` → `/mnt/wsl/docker-desktop/...` へのリンクが壊れている  
- `docker.sock` が無い → デーモン未接続  

### 直し方

1. Windows で **Docker Desktop を起動**（Running まで待つ）  
2. Settings → Resources → **WSL integration**  
   - default distro ON  
   - **Ubuntu** など使用中 distro ON  
   - Apply & restart  
3. PowerShell で `wsl --shutdown` → Ubuntu を開き直す  
4. `docker version` で **Server:** が出ること  

公式: https://docs.docker.com/go/wsl2/

その後:

```bash
cd ~/workspace/live-qa-session-lab
docker compose -f docker/docker-compose.yml up -d
```

---

## Java / `mvn spring-boot:run` が exit code 1

外側のメッセージ:

```text
Failed to execute goal ... spring-boot-maven-plugin:3.4.2:run
Process terminated with exit code: 1
```

### 本質的な原因（会話で出たログ）

```text
Unable to resolve Configuration with the provided Issuer of
"http://localhost:8081/realms/live-qa"

Caused by: Connection refused
GET .../realms/live-qa/.well-known/openid-configuration
```

**Keycloak が `localhost:8081` で動いていない**（または realm が無い）。  
`SessionConfig` の写経ミスではない。

### 流れ（意味つき）

```text
mvn spring-boot:run
  → issuer-uri を見て Keycloak の
    /.well-known/openid-configuration を GET しようとする
  → 8081 が閉じている（Connection refused）
  → OAuth2 の Bean 作成失敗
  → プロセス終了
```

`SecurityConfig` 未実装でも、`oauth2-client` + `issuer-uri` があるだけで起動時にこの取得が走る。

### Redis / Keycloak 両方落ちているとき

| 確認 | Connection refused なら |
|------|-------------------------|
| `localhost:8081` | Keycloak 未起動 |
| `localhost:6379` | Redis 未起動 |
| `docker` / sock | Docker 未連携が先 |

**直す順番:** Docker Desktop → compose up → Keycloak 疎通 → `mvn spring-boot:run`

---

## curl が `302` と `404`

| 応答 | 意味 |
|------|------|
| `302`（`http://localhost:8081/`） | Keycloak プロセスは動いている（リダイレクト） |
| `404`（`.../realms/live-qa/.well-known/...`） | **realm `live-qa` が未作成** |

### やること

1. http://localhost:8081 → `admin` / `admin`  
2. Realm **`live-qa`** を Create  
3. 再確認:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8081/realms/live-qa/.well-known/openid-configuration
```

→ **`200`** が目標。

4. Client **`live-qa-session-lab`**  
   - Client authentication: On  
   - Valid redirect URI: `http://localhost:8080/login/oauth2/code/keycloak`  
   - Client secret を `.env` の `KEYCLOAK_CLIENT_SECRET` に合わせる  

5. （任意）テストユーザー作成  

6. `mvn spring-boot:run`

---

## 起動成功したら何ができたか

1. **取得** → OpenID Configuration を Keycloak から読む  
2. **変換** → Spring の `ClientRegistration` にする  
3. **登録／格納** → Bean としてプロセス内に保持（まだユーザーセッションではない）  
4. **待受** → `localhost:8080`  

起動成功時点では Redis のセッションキーが空でも普通。  
リクエスト後に `spring:session:sessions:...` が見えれば Day2「Redis に書ける」クリア寄り。

---

## チェックコマンド早見

```bash
# Docker
docker version
docker compose -f docker/docker-compose.yml ps

# Keycloak
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8081/realms/live-qa/.well-known/openid-configuration

# Redis
docker exec -it $(docker compose -f docker/docker-compose.yml ps -q redis) redis-cli PING
docker exec -it $(docker compose -f docker/docker-compose.yml ps -q redis) redis-cli KEYS 'spring:session:*'
```

---

## Q: 「404 かつ起動失敗」の切り分け（2026-07-29）

### 観測

| 対象 | 結果 | 意味 |
|------|------|------|
| `mvn compile` | SUCCESS | Java コードのコンパイルは通っている |
| Redis / Keycloak コンテナ | Up | Compose は動いている |
| `http://localhost:8081/` | 302 | Keycloak プロセスは生きている |
| `.../realms/live-qa/.well-known/...` | **404** + `{"error":"Realm does not exist"}` | **realm `live-qa` が無い** |
| `localhost:8080` | 000 | アプリ未起動（起動失敗 or 未実行） |

### 原因仮説（起動失敗）

Spring は起動時に `issuer-uri`（`http://localhost:8081/realms/live-qa`）から OpenID Configuration を取る。  
realm が無い → 取得失敗 → OAuth2 の Bean 作成失敗 → **起動失敗**。

ラボ Compose の Keycloak は `start-dev` で **永続ボリューム無し**のため、コンテナ作り直しで手作成した realm / client が消えることがある。

### 404 の二種類（混ぜない）

| 404 | 意味 |
|-----|------|
| Keycloak の realm URL | realm 未作成（上記） |
| ログイン後の `GET /api/me` | アプリは動いているが **`MeController` 未実装**（別問題） |

いまの第一優先は **realm を作り直してアプリ起動**。`/api/me` の 404 は起動後の話。

### 直し方

1. http://localhost:8081 → `admin` / `admin`  
2. Realm **`live-qa`** を Create  
3. Client **`live-qa-session-lab`**（secret・redirect URI を `.env` に合わせる）  
4. ユーザー + realm role **`HOST`**  
5. 確認:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8081/realms/live-qa/.well-known/openid-configuration
# → 200
```

6. `mvn spring-boot:run` → `Started SessionLabApplication`  
7. 未ログイン `GET /api/me` → **401**（404 ではない）。ログイン後まだ Controller 無しなら **404** → 次は `実装` で `MeController`

---

## realm JSON の import（推奨）

手で realm を作ると、コンテナ作り直しで消えて起動失敗しやすい。  
ラボでは `docker/keycloak/import/live-qa-realm.json` を `--import-realm` で取り込む。

```bash
docker compose -f docker/docker-compose.yml down
docker compose -f docker/docker-compose.yml up -d
# 少し待ってから
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8081/realms/live-qa/.well-known/openid-configuration
```

詳細は [docker/README.md](../../docker/README.md)。client secret は JSON / `.env.example` とも `change-me`。
