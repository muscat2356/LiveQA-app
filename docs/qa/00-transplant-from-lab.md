# 本番（live-qa-app）への移植判断

> 出典: `live-qa-session-lab/docs/transplant.md`（ラボ素振りからの持ち込み）。  
> **コードは本人が手で写す**。本ファイルは「何を持っていくか」の判断表。設計の正は親の `docs/architecture.md` / `api/openapi-notes.md` 等。

---

## 持ち込むもの / 持ち込まないもの

### A. 学習 docs（持ち込む ○）

ハンズオンで積んだ理解を本番 JIT に残す。**設計ドキュメント（architecture 等）は上書きしない。**

| ラボ | 本番の置き場 | 備考 |
|------|--------------|------|
| `docs/qa/01`〜`07` | `live-qa-app/docs/qa/` | 学習メモ。矛盾時は本番 architecture / openapi 優先 |
| `docs/transplant.md`（本ファイル） | `docs/qa/00-transplant-from-lab.md` として要約コピー可 | 移植判断の索引 |
| `docs/checklist.md` の学び | 必要なら要約のみ | チェックリスト自体はラボ用 |

| 持ち込まない docs |
|-------------------|
| `docs/ai-context.md`（ラボ専用コンテキスト） |
| ラボだけの Compose / realm 手順の丸コピー（方針だけ qa に残す） |
| `.env` / secret / トークン生値 |

### B. コード・設定（方針は ○ / ファイル丸ごと ✕）

| ラボで得たもの | 本番へ | 判断 |
|----------------|--------|------|
| Cookie 名 `SESSION` / SameSite / HttpOnly | Session 設定に反映 | ○ 方針 |
| Spring Session + Redis | backend の Session 設定 | ○ |
| `oauth2Login` + `userAuthoritiesMapper(grantedAuthoritiesMapper())` | SecurityConfig | ○ **差し込み必須**（Bean だけでは呼ばれない） |
| `CookieCsrfTokenRepository` + `X-XSRF-TOKEN` | SecurityConfig | ○ |
| `/api/me`（OidcUser + **Authentication** から role） | 本番契約の Me API | ○（権限は Authentication 側） |
| 401/403 `{ status, error, message }` | exceptionHandling | ○ |
| realm の `realm_access.roles` をトークンへ載せる | Keycloak クライアント設定 | ○ 方針（本番 realm は別管理の想定） |
| `POST /api/lab/ping` | — | ✕ ラボ専用。移植しない |
| ラボ `docker-compose` / `live-qa-realm.json` | — | ✕ 丸コピーしない。redirect URI・secret は本番環境に合わせる |
| 匿名 `guestId` / `voter_key=g:...` | 参加フロー側で同趣旨 | ○ 考え方（エンドポイント名は本番 openapi に合わせる） |

### B-2. プロジェクト骨組みの移行（実施済み）

空だった `backend/` にラボから Maven + F-01 最小コードを移植した。  
**手順の正本:** [08-backend-project-migrate.md](08-backend-project-migrate.md)

要約: パッケージ `com.liveqa` / `lab/ping` 除外 / mapper 差し込み・Authentication から role を維持 / `mvn package` 確認済み。


### C. ハマりどころ（必ず頭に入れる）

| 学び | 本番での意味 |
|------|--------------|
| Mapper は `@Bean` だけでは呼ばれない | `oauth2Login(...userAuthoritiesMapper(...))` を明示 |
| mapper 結果は `Authentication` に載る | `/api/me` で `OidcUser.getAuthorities()` だけ見ると role 欠落しうる |
| CSRF は Cookie とヘッダの両方が必要 | フロントは `XSRF-TOKEN` → `X-XSRF-TOKEN` |
| 古い SESSION に古い権限が残る | ロール修正後は再ログイン |

---

## 推奨移植順（本番 F-01）

1. Redis + Spring Session + CookieSerializer  
2. SecurityFilterChain（oauth2Login / CSRF / 401 JSON / mapper **差し込み**）  
3. `GET /api/me`（権限は Authentication）  
4. Keycloak クライアント（roles クレーム）  
5.（余力）匿名セッション / `voter_key`  
6. ログアウト（本番 openapi）

確認のたびに本番 `docs/` と照合。ラボ `docs/qa/` は「なぜそうしたか」の補助。

---

## コピー手順（docs のみ）

```bash
# 例: ラボの qa を本番へ（上書き前に差分確認）
cp -n ../live-qa-session-lab/docs/qa/0*.md ../live-qa-app/docs/qa/
# README は本番側索引を手でマージ
```

`-n` は既存を上書きしない。既に本番 qa がある場合は追記マージ。
