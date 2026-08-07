# POST /api/events が 500（JoinCode 正規表現 typo）

## 症状

CSRF・SESSION は通る（`GET /api/me` 200）が、`POST /api/events` が:

```json
{ "status": 500, "error": "INTERNAL_SERVER_ERROR", "message": "サーバーエラーが発生しました。" }
```

## 原因

`JoinCode` の compact constructor:

```java
!code.matches("~[a-zA-Z0-9]+$")  // ← 先頭がリテラル ~
```

`generate()` は `POOL` から 8 文字を作るが、`~` で始まらないため **毎回** `IllegalArgumentException`。  
Advice の catch-all が握りつぶして 500（当時はログも無し）。

## 修正

```java
!code.matches("^[A-Za-z0-9]+$")
```

あわせて `GlobalExceptionHandler` の catch-all で `log.error` する。

## 確認

```bash
# XSRF 取り直し後
curl -sS -X POST http://localhost:8081/api/events \
  -H 'Content-Type: application/json' \
  -H "Cookie: SESSION=...; XSRF-TOKEN=..." \
  -H "X-XSRF-TOKEN: ..." \
  -d '{"title":"curlテストイベント"}'
# 期待: 201
```
