# 全体像の解説

## 一言でいうと

このラボは **「Keycloak で一度だけログイン → あとは `SESSION` Cookie + Redis で本人確認」** を素振りする場所。  
本番 F-01 の心臓部だけで、CRUD / STOMP / Next.js はやらない。

## 本番との位置づけ

```text
live-qa-app（本番）     … イベント・質問・いいね・STOMP・画面
        ↑ 移植
live-qa-session-lab     … いまここ。認証・セッションだけ
```

| | ラボ | 本番 |
|--|------|------|
| 目的 | Member B の JIT | ポートフォリオ本体 |
| 認証 | やる（F-01 素振り） | やる + CRUD 等 |
| 設計の正 | 要約 docs | `live-qa-app/docs/` |

LibraShare との違い: あちらは **毎回 JWT を検証（stateless）**。  
こちらは **入口だけ OIDC、以降はセッション（stateful）**。

## ハイブリッド認証の全体像

```text
主催者: ブラウザ → Keycloak(OIDC) → oauth2Login 成功
       → SecurityContext をセッションへ格納
       → 以降は SESSION Cookie のみ（JWT を毎回検証しない）

参加者: ログイン不要。匿名セッション（ラボ余力）
```

### 操作語の意味

| 操作 | 意味 |
|------|------|
| **交換** | 認可コードをトークンに取り換える |
| **変換** | クレームをアプリのユーザー情報・ロールに読み替える |
| **格納** | SecurityContext などを Redis セッションへ書き込む |
| **発行** | ブラウザへ `SESSION` Cookie を Set-Cookie で渡す |
| **復元** | Cookie の ID を鍵に Redis から前回内容を読み戻す |
| **検証** | CSRF トークンや `hasRole('HOST')` を確かめる |

### シーケンス（イメージ）

```text
ブラウザ → GET /oauth2/authorization/keycloak
       → Keycloak ログイン
       → 認可コードがコールバック
       → 交換（コード→トークン）
       → 変換（クレーム→ユーザー）
       → 格納（SecurityContext を Redis へ）
       → 発行（Set-Cookie: SESSION=...）
       → 以降 GET /api/me は Cookie だけで復元
```

## 登場人物（部品の正体）

| 部品 | 正体 | 役割 |
|------|------|------|
| Keycloak | OIDC プロバイダ（別プロセス） | 主催者の本人確認の入口 |
| `spring-boot-starter-oauth2-client` | ライブラリ | `oauth2Login` で認可コードフローを面倒見る |
| `spring-boot-starter-security` | ライブラリ | 認可・CSRF・フィルタチェーン |
| `spring-session-data-redis` | ライブラリ | セッション本体を Redis に置く |
| Redis | 外部ストア | セッションの置き場 |
| `SESSION` Cookie | ブラウザ側の鍵 | ID だけ。中身は Redis |
| `XSRF-TOKEN` / `X-XSRF-TOKEN` | CSRF 用 Cookie / ヘッダ | 状態変更 POST のなりすまし防止 |

## ラボで書く3ファイルの地図

| ファイル | 担う仕事 |
|----------|----------|
| `SessionConfig` | Cookie 名 `SESSION`・SameSite 等。発行／格納／復元の「置き方」 |
| `SecurityConfig` | `oauth2Login`・誰がどの URL に入れるか・CSRF・401 JSON |
| `MeController` | `GET /api/me`・`POST /api/lab/ping`（HOST+CSRF 確認） |

## Cookie が2種類ある理由

| 名前 | 何のため | 誰が読む |
|------|----------|----------|
| `SESSION` | 「どの箱か」を識別 | サーバのみ（HttpOnly） |
| `XSRF-TOKEN` | POST 時の CSRF 検証用 | JS が読んで `X-XSRF-TOKEN` ヘッダに載せる |

GET `/api/me` は Cookie だけでよい。  
POST `/api/lab/ping` は **SESSION + CSRF ヘッダ + HOST ロール** が揃って初めて通る確認用。

## Day の道のり

| Day | 到達イメージ |
|-----|----------------|
| 2 | Redis にセッションが載る（`SessionConfig`） |
| 3 | ログイン → `SESSION` → `/api/me`、CSRF POST 1本 |
| 4 | 通し確認 → 学びを本番 F-01 へ移植 |

完了の目安: ログインできる / Cookie だけで `/api/me` が 200 / CSRF 付き POST が通る / 未ログインは 401・権限不足は 403。
