# Event 作成写経の設計判断（StringBuilder / @Valid / 404）

## 質問

1. joinCode 生成は for より StringBuilder のほうが EJ 的によいのでは？  
2. Service の if バリデーションは不要では？  
3. null を出さない設計なら 404 の例外処理は不要か。Handle したほうがよいか？

## 回答

### 1. StringBuilder と for

**対立ではない。** EJ（文字列連結）が戒めるのはループ内の `s = s + x`（毎回新 String）。  
**for + StringBuilder** が定石で、前回写経の `randomJoinCode` もその形。

```text
悪い: for の中で String +
よい: StringBuilder に append（通常は for と併用）
```

Stream で無理に寄せる必要はない（可読性優先）。

### 2. Service の if バリデーション

**Controller 経由なら `@Valid` に任せてよい。** Service の title if は削って問題ない。

残すとよいのは任意で **保存前の `trim()` だけ**（constraints は「trim 後」）。長さ再判定まで Service に書くなら二重になるので、どちらか一方に寄せる。

| 層 | 役割 |
|----|------|
| `@Valid` + Handler | 400 + `fields`（openapi） |
| Service `trim` | 正規化してから DB 格納（任意だが推奨） |

### 3. 404 と Handler

**この単位（POST 作成・GET 自分の一覧）では 404 は不要。** クライアントが「無い id」を指定していない。

| 状況 | HTTP | 扱い |
|------|------|------|
| title 不正 | 400 | 既存 `validationHandler` |
| joinCode 発行不能 | 409 | `BusinessException` → `businessHandler` |
| 後続 GET/PATCH `/{id}` で無い | 404 | **その単位で** `BusinessException` + 既存 Handler |
| JIT 後なのに users 無し | 500 相当 | 不変条件破り。404 にしない（クライアント向け「資源なし」ではない） |

「null を出さない」＝ `Optional.orElseThrow` 等で **戻り値 null を避ける**こと。それと「リソース無しを 404 で返す」は別問題。  
**専用の 404 Handler メソッドは増やさなくてよい** — 既に `BusinessException` を `businessHandler` が status どおり返すので、後続で `new BusinessException(NOT_FOUND, "Not Found", ...)` を投げれば足りる。
