# ログイン後リダイレクト（Member A 連携）

## 質問 / 要求

Member A より「ログイン後のリダイレクト先 URL を送ってほしい」。backend 側はどう実装するか。

## 契約（正）

openapi（OIDC コールバック）:

> `Set-Cookie: SESSION=...` を付与し、**フロントの主催者ダッシュボードへリダイレクト**

画面遷移: Keycloak コールバック後 → **主催者ダッシュボード**（Next `/dashboard`）

## いまの穴

| 段階 | 誰が決める | 現状 |
|------|------------|------|
| Keycloak → backend | `redirect-uri: {baseUrl}/login/oauth2/code/...` | ✅ 例: `:8081` |
| backend → **フロント** | oauth2Login の **successHandler** | ❌ 未設定 → 既定で backend の `/` に着地しがち |
| logout 後 | `app.logout.post-logout-redirect-uri` | ✅ `:3000/` |

## Member A に伝える URL（Compose 開発）

| 用途 | URL |
|------|-----|
| ログイン開始（A がボタンに貼る） | `http://localhost:8081/oauth2/authorization/keycloak` |
| **ログイン成功後の着地（B が successHandler に設定）** | **`http://localhost:3000/dashboard`** |
| ログアウト後（既存） | `http://localhost:3000/` |

A 側ルート: `frontend/app/(organizer)/dashboard/page.tsx` → パス `/dashboard`

## 実装方針

`app.login.success-redirect-uri` を追加し、`SimpleUrlAuthenticationSuccessHandler` で常にそこへ 302。  
`setAlwaysUseDefaultTargetUrl(true)` … SavedRequest が API URL のまま残ってもフロントへ戻す。
