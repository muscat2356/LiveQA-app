# F-01 仕上げ（JIT・`/api/me`・logout）

## 質問 / 依頼

F-01 仕上げを **実装** する（B-01）。

## 現状（2026-08 確認）

| 項目 | 状態 |
|------|------|
| Session / Redis / oauth2Login / mapper 差し込み / CSRF | ✅ |
| JIT（`UserRepository` / `UserService.provisionHost` / `OidcUserService` 差し込み） | ✅ |
| `GET /api/me` | ✅ ローカル PK `Long` + `currentHost` + `@PreAuthorize("hasRole('HOST')")` |
| 単体テスト | ✅ `UserServiceTest` 7 / `MeControllerTest` 3（HOST 200・非HOST 403・未認証 401） |
| `POST /api/auth/logout` | ✅ `logoutUrl` + `OidcClientInitiatedLogoutSuccessHandler`（単体は Keycloak 依存のため Compose 疎通向け） |
| 401/403 JSON 文言 | ✅ SecurityConfig が openapi どおり（`Unauthorized` / 「ログインが必要です」、`Forbidden`） |
| CORS / GlobalExceptionHandler | B-13 / B-19（仕上げ本丸の外でも可） |

### 401/403 と GlobalExceptionHandler の役割分担

| 経路 | 担当 | 例 |
|------|------|-----|
| フィルタ連鎖の未認証 / 認可失敗 | **SecurityConfig** の `AuthenticationEntryPoint` / `AccessDeniedHandler` | Cookie 無し `/api/me`、`@PreAuthorize` 失敗 |
| Controller 到達後の業務・検証例外 | **GlobalExceptionHandler**（B-19） | 404 NotFound、`@Valid` 400、409 Conflict |

B-12: 「401/403 の JSON は SecurityConfig 既存を流用・統一」。B-19: 「Security の 401/403 と形が揃っている、または役割分担がコメントで明確」。  
→ **統一するのはレスポンス形（`{ status, error, message }`）と文言契約**。Handler が Security の 401/403 を置き換える想定ではない。

## 推奨写経順

1. `UserRepository` → 2. `UserService`（JIT）→ 3. `OidcUserService` 差し込み  
4. `MeResponse` / `MeController` → 5. logout + end-session → 6. 401/403 文言合わせ

受け入れの正: ラボ `docs/issues/member-b/B-01-f01-auth-finish.md` + `docs/api/openapi-notes.md` 認証節。

## 疎通チェック

1. 初回ログイン後 DB: `SELECT * FROM users;` に HOST 行  
2. `GET /api/me` → `id` が数値、`role` が `HOST`  
3. 未ログイン `/api/me` → 401 `{ status, error:"Unauthorized", message:"ログインが必要です" }`  
4. `POST /api/auth/logout` + CSRF → セッション破棄（＋ Keycloak へ 302 ならなお良い）  
5. HOST 以外で `@PreAuthorize("hasRole('HOST')")` → 403

## 学習の要点（PR向け）

- JIT はログイン時（`OidcUserService`）に `users` へ。`/api/me` は復元＋DTO組み立てのみ
- 応答の `role` は DB、認可の `ROLE_HOST` は `Authentication`（混ぜない）
- 401/403（フィルタ）は SecurityConfig、業務例外は将来 GlobalExceptionHandler（形だけ揃える）
- `@WebMvcTest` は本番 Security を無理に Import せず、OAuth2Client は exclude（issuer 解決回避）
