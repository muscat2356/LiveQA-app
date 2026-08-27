# Backend（Spring Boot / Maven）

F-01 骨組みは `live-qa-session-lab` から移植済み。手順・差分は:

- [docs/qa/00-transplant-from-lab.md](../docs/qa/00-transplant-from-lab.md)
- [docs/qa/08-backend-project-migrate.md](../docs/qa/08-backend-project-migrate.md)

認証先は **LibraShare realm に相乗り**（サードパーティークライアント）。要点は [docs/qa/ops.md](../docs/qa/ops.md)。学習メモの入口は [docs/qa/README.md](../docs/qa/README.md)。

| 項目 | 値 |
|------|-----|
| realm | `LibraShare` |
| client | `live-qa-app`（confidential） |
| Keycloak | `http://localhost:8080`（Compose が公開） |
| backend | Compose では `http://localhost:8081` |

## 起動（例）

```bash
# Redis / Keycloak / Postgres は本番 Compose で起動
docker compose up -d postgres redis keycloak

cd backend
cp .env.example .env   # 値を環境に合わせる
# .env を export するか IDE の Run Configuration で渡す
# Keycloak が 8080 を使うので、ホスト起動時はポートをずらす
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

- ログイン開始: `http://localhost:8081/oauth2/authorization/keycloak`
- `GET /api/me`（SESSION Cookie）
- `POST /api/auth/guest`（CSRF 検証なし。応答で `XSRF-TOKEN` Cookie が載る）
- Swagger UI: `http://localhost:8081/swagger-ui.html`（OpenAPI JSON は `/v3/api-docs`）

契約の正は [docs/api/openapi-notes.md](../docs/api/openapi-notes.md)。Swagger は実装の閲覧用。状態変更の Try it out には `SESSION` と `X-XSRF-TOKEN` が要る（Swagger UI は CSRF ヘッダを自動では付けない）。STOMP は UI に出ない。

realm の client `live-qa-app` は redirect URI に `http://localhost:8081/*` と `http://localhost:3000/*` を許可済み。`realm_access.roles` の `HOST` を `ROLE_HOST` に読み替える mapper は backend 側の実装（[docs/qa/f01-auth.md](../docs/qa/f01-auth.md) / [docs/qa/05-security-config.md](../docs/qa/05-security-config.md)）。
