# live-qa-session-lab → live-qa-app/backend プロジェクト移行手順

> 実施日目安: F-01 素振り完了後。設計の正は `docs/architecture.md` / `api/openapi-notes.md`。

## 前提

| 項目 | 内容 |
|------|------|
| 出典 | `../live-qa-session-lab`（練習用） |
| 行き先 | `backend/`（本リポジトリ） |
| ビルド | **Maven**（`pom.xml`） |
| パッケージ | ラボ `com.example.sessionlab` → 本番 `com.liveqa` |

## 持ち込んだもの（骨組み）

| ラボ | 本番 | 変更点 |
|------|------|--------|
| `pom.xml` | `backend/pom.xml` | artifact=`live-qa-backend` / group=`com.liveqa` |
| `SessionLabApplication` | `LiveQaApplication` | パッケージ・クラス名 |
| `SessionConfig` | 同名 | `redisNamespace = "live-qa"` |
| `SecurityConfig` | 同名 | **`/api/lab/ping` 削除**。`/api/auth/guest` は permitAll。メソッド名 `addRolesFromRealmAccess` |
| `MeController` | 同名 | **ping 削除**。`Authentication` から role。メソッド名 `me` |
| `MeResponse` | 同名 | パッケージのみ |
| （余力）Guest | `GuestSessionController` | `POST /api/auth/guest` |
| `application.yml` | 同名 | app 名・client-id 既定を本番向けに |
| — | `.env.example` / `backend/README.md` | 起動メモ |

## 持ち込まないもの

| 除外 | 理由 |
|------|------|
| `POST /api/lab/ping` | ラボ専用の疎通口 |
| ラボ `docker/` / `live-qa-realm.json` 丸コピー | 本番 Compose・realm は別管理。方針だけ参照 |
| `.env` の secret 生値 | `.env.example` のみ |
| ラボ `docs/ai-context.md` | ラボ専用 |

## 手動でやった／やる手順（再現用）

1. **空の backend を確認**  
   当初は `backend/AGENTS.md` のみ。Maven プロジェクトが無ければ骨組みを追加する。

2. **依存を揃える**（ラボ `pom.xml` と同じ starter）  
   `web` / `security` / `oauth2-client` / `data-redis` / `session-data-redis`

3. **パッケージを付け替えてコピー**  
   ```text
   com.example.sessionlab.*  →  com.liveqa.*
   ```

4. **ラボ専用を削る**  
   - Security の `/api/lab/ping`  
   - MeController の `@PostMapping("/lab/ping")`

5. **本番で残す必須ポイント**（素振りでハマった箇所）  
   - `.oauth2Login(oauth2 -> ... userAuthoritiesMapper(grantedAuthoritiesMapper()))`  
   - `/api/me` は `authentication.getAuthorities()` で role 判定  
   - CSRF: `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `X-XSRF-TOKEN`

6. **Keycloak**  
   - クライアント ID／secret／redirect URI を本番用に設定  
   - `realm_access.roles` が ID トークンに載ること（ラボ realm JSON の protocolMapper を参考）

7. **起動確認**  
   ```bash
   cd backend
   mvn -q -DskipTests package
   # Redis + Keycloak 起動後
   mvn spring-boot:run
   ```
   - `/oauth2/authorization/keycloak` → ログイン → `GET /api/me` が 200  
   - `role` が `HOST`（再ログイン必須の場合あり）

8. **学習メモ**  
   ラボ `docs/qa/` は本番 `docs/qa/` にコピー済み（01〜07 + 00-transplant）。矛盾時は設計ドキュメント優先。

## ディレクトリ対応

```text
live-qa-session-lab/
  pom.xml
  src/main/java/com/example/sessionlab/...
  src/main/resources/application.yml

live-qa-app/backend/
  pom.xml
  src/main/java/com/liveqa/...
  src/main/resources/application.yml
  .env.example
  README.md
```

## このあとの実装（本人／チーム）

| 順 | 内容 |
|----|------|
| 1 | 本番 Keycloak クライアント・Compose との接続値を確定 |
| 2 | ログアウト（openapi: `POST /api/auth/logout`） |
| 3 | CORS（Next.js オリジン） |
| 4 | Flyway / Entity（F-02 以降） |
| 5 | HOST 専用 API に `@PreAuthorize` |

## 関連

- [00-transplant-from-lab.md](00-transplant-from-lab.md) — 判断表（持ち込む／しない）
- [05-security-config.md](05-security-config.md) / [06-me-controller.md](06-me-controller.md) — ハマりどころ
- ラボ側手順の正本コピー元: `live-qa-session-lab/docs/transplant.md`
