# POST 403（入力は正しい前提）のサーバー側切り分け

## 前提

クライアントの `Cookie: SESSION=...; XSRF-TOKEN=...` と `X-XSRF-TOKEN` は正しいとする。

## サーバーが 403 を返す経路（このプロジェクト）

403 JSON を書くのは **`SecurityConfig` の `AccessDeniedHandler` だけ**（Advice に 403 ハンドラは無い）。

| 原因 | 例外 | 備考 |
|------|------|------|
| CSRF 欠落 | `MissingCsrfTokenException` | Cookie `XSRF-TOKEN` なし |
| CSRF 不一致 | `InvalidCsrfTokenException` | Cookie とヘッダが一致しない |
| HTTP 認可不足 | その他 `AccessDeniedException` | 例: 認証済みだが権限不足 |

`@PreAuthorize` 失敗は Controller 到達後 → Advice の catch-all で **500** になりやすく、この 403 とは別経路。

## 本番の message 分岐

Cookie 文字列の詳細診断は切り分け用だったため外し、例外型の分岐だけ残す。

| 例外 | message |
|------|---------|
| `MissingCsrfTokenException` | `CSRFトークンがありません` |
| `InvalidCsrfTokenException` | `CSRFトークンが一致しません` |
| その他 AccessDenied | `権限がありません` |

未ログインは CSRF 通過後 **401**（`/api/events` は `authenticated()`）。

## 入力ミス以外で多い原因（Invalid）

API クライアントの **Cookie jar と Headers の二重送信**。  
`WebUtils.getCookie` は先頭の `XSRF-TOKEN` を採用するため、画面上は揃えていてもサーバー採用値がずれることがある。

対処: jar を空にする／curl で Cookie を明示する。手順は [42](42-post-confirm-headers.md)。
