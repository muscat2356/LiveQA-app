# POST 確認時に付けるヘッダー

## 質問

Bruno 等で `POST /api/events` を確認するとき、ヘッダーは何を付けるか。

## 結論（最低セット）

| 名前 | 置き場 | 値の例 | 役割 |
|------|--------|--------|------|
| `Cookie` | Cookie / Headers | `SESSION=...; XSRF-TOKEN=...` | 認証の鍵＋ CSRF 合い言葉（Cookie 側） |
| `X-XSRF-TOKEN` | Headers | Cookie の `XSRF-TOKEN` と**同じ文字列** | CSRF 合い言葉（ヘッダ側） |
| `Content-Type` | Headers | `application/json` | ボディを JSON として読む |

GET 確認（[33](33-bruno-session-get.md)）との差: **POST だけ CSRF の Cookie＋ヘッダが必須**。

## なぜこの3つか

```text
ブラウザなりすまし対策（CSRF）と、誰が呼んでいるか（SESSION）は別物。

SESSION
  → 復元: Redis の箱 → SecurityContext（HOST か）
  → 無い / 無効 → 401
  → HOST でない → 403（@PreAuthorize）

XSRF-TOKEN Cookie と X-XSRF-TOKEN ヘッダ
  → 検証: 一致するか（CsrfFilter）
  → 不一致 / 欠落 → 403（Controller 未到達）

Content-Type: application/json
  → 変換: ボディ JSON → EventCreateRequest（@RequestBody）
  → 無いと 415 やパース失敗になりやすい
```

Bearer `Authorization` は付けない（この API は Cookie セッション設計）。

## Bruno 手順（POST /api/events）

### 1. Cookie を揃える

1. ブラウザで OIDC ログイン（backend オリジン、例 `http://localhost:8081`）
2. DevTools → Application → Cookies → `localhost:8081`
3. コピー:
   - `SESSION`（HttpOnly・認証）
   - `XSRF-TOKEN`（HttpOnly ではない・CSRF）

`XSRF-TOKEN` がまだ無いときは、先に **GET**（例 `/api/me`）を一度叩き、応答の `Set-Cookie` で発行させる。

### 2. Bruno に載せる

**Headers**

| Name | Value |
|------|--------|
| `Content-Type` | `application/json` |
| `X-XSRF-TOKEN` | （`XSRF-TOKEN` Cookie の値をそのまま） |
| `Cookie` | `SESSION=<値>; XSRF-TOKEN=<同じ値>` |

Cookie タブを使う場合も、**ヘッダ `X-XSRF-TOKEN` は別途必須**（Cookie だけでは CSRF は通らない）。

**Body**（JSON）

```json
{ "title": "第3回LT大会 Q&A" }
```

### 3. 期待と切り分け

| 結果 | 疑うヘッダー／状態 |
|------|-------------------|
| 201 | セット成功 |
| 401 | `SESSION` 欠落・期限切れ |
| 403（CSRF） | `X-XSRF-TOKEN` 欠落、または Cookie と不一致 |
| 403（認可） | CSRF は通ったが HOST でない |
| 400 | ヘッダは通った・`title` 検証 |
| 415 | `Content-Type` 不足／誤り |

## GET との対照

| | GET `/api/me` 等 | POST `/api/events` 等 |
|--|------------------|------------------------|
| `Cookie: SESSION` | 要 | 要 |
| `XSRF-TOKEN` + `X-XSRF-TOKEN` | 不要 | **要** |
| `Content-Type` | 不要（ボディ無し） | JSON ボディなら **要** |

定義の正: [openapi-notes](../api/openapi-notes.md)（状態変更は `X-XSRF-TOKEN`）／実装の置き場: [35](35-csrf-header-how-to-define.md)。

## 403 で通らないとき（切り分け）

このプロジェクトの `AccessDeniedHandler` は **CSRF 失敗も認可不足も同じ JSON** を返す:

```json
{ "status": 403, "error": "Forbidden", "message": "権限がありません" }
```

メッセージだけでは原因が分からない。次の順で見る。

### 1. 先に GET `/api/me`（同じ `SESSION`）

| GET `/api/me` | 意味 | POST で次に疑うこと |
|---------------|------|---------------------|
| **200** で `role: "HOST"` | ログイン・HOST は OK | **CSRF**（ヘッダ欠落／不一致が最多） |
| **401** | SESSION 無効 | ブラウザで再ログインして Cookie を取り直す |
| **500** | B-22 系（Advice が認可例外を飲み込み） | SESSION はあるが HOST でない等 |

### 2. CSRF の付け忘れパターン（最多）

| Bruno の状態 | 結果 |
|--------------|------|
| `Cookie: SESSION` だけ（GET のまま） | **403** CSRF |
| Cookie に `XSRF-TOKEN` はあるが **ヘッダ `X-XSRF-TOKEN` が無い** | **403** CSRF |
| ヘッダと Cookie の値が違う（古いトークン混在） | **403** CSRF |

直し方:

1. DevTools → `localhost:8081` の Cookie から **いまの** `XSRF-TOKEN` をコピー  
2. Headers に `X-XSRF-TOKEN: <その値>` を追加  
3. Cookie も `SESSION=...; XSRF-TOKEN=<同じ値>`  
4. 直前の POST 失敗応答で `Set-Cookie: XSRF-TOKEN=...` が出たら、**新しい値に更新**して再送

### 3. CSRF が通ったあとの別症状

| 結果 | 意味 |
|------|------|
| **201** | 成功 |
| **500**（`INTERNAL_SERVER_ERROR`） | CSRF は通過。未ログイン／非 HOST で `@PreAuthorize` が落ち、Advice の catch-all に入った可能性（B-22） |
| **400** | 認可・CSRF は通過。`title` 検証 |

見分け: CSRF 失敗の 403 は `Content-Type: application/json;charset=UTF-8`（Filter の `AccessDeniedHandler`）。Advice の JSON は charset 無しのことが多い。

### Bruno / Hoppscotch で「例どおり」なのに 403 のとき

画面の Cookie 欄が `X-XSRF-TOKEN` と**同じ短い文字列だけ**なら、まだ NG です。

| 欄 | NG（403 になる） | OK |
|----|------------------|-----|
| Cookie | `a-bf88-f49057b52df4` | `SESSION=YjJI...; XSRF-TOKEN=2fa1aaa4-...` |
| X-XSRF-TOKEN | `a-bf88-f49057b52df4` | `2fa1aaa4-...`（名前なし・UUID 本体だけ） |

セルフチェック:

1. Cookie の**先頭**が `SESSION=` で始まっているか  
2. 途中に `; XSRF-TOKEN=` があるか  
3. `X-XSRF-TOKEN` ヘッダに `XSRF-TOKEN=` や `SESSION=` を書いていないか  
4. Cookie とヘッダが同じ値だけ、になっていないか  

curl で同じ誤りを再現すると Cookie 値だけ → **403**、正しい `XSRF-TOKEN=` 付き → CSRF 通過（未ログインなら現状 500）。

## 一文

> POST 確認は「誰か」=`SESSION`、「なりすましでない」=`XSRF-TOKEN`＋`X-XSRF-TOKEN`、「JSON だ」=`Content-Type`。ヘッダ名 `X-XSRF-TOKEN` の値は Cookie `XSRF-TOKEN` と同一。403「権限がありません」は CSRF 欠落でも出るので、先に `/api/me` 200 を確認してから CSRF を疑う。
