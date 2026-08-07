# 手動バリデーションは MethodArgumentNotValidException で統一すべきか

## 質問

自身でバリデーションチェックして分岐で投げたいとき、別例外の方がよいか。できれば統一したい。

## 結論

**例外クラスを `MethodArgumentNotValidException` に無理統一しない。**  
**統一するのは HTTP 応答の形（`ErrorResponse`）**。

| 経路 | 例外 | 誰が投げる |
|------|------|------------|
| `@Valid` / Bean Validation | `MethodArgumentNotValidException` | Spring MVC |
| Service などの手チェック | **`BusinessException`（400 + 任意 fields）** など自前 | アプリ |

`MethodArgumentNotValidException` は `MethodParameter` + `BindingResult` 前提の **Web 層の例外**。Service から new すると Web 依存が染み、引数も不自然（`null` の MethodParameter など）。

## 推奨（このプロジェクト）

すでに `BusinessException` + Handler があるので、手チェックはそちらへ寄せる。

```text
@Valid 失敗
  → MethodArgumentNotValidException
  → validationHandler → 400 + fields

手チェック失敗
  → BusinessException(BAD_REQUEST, "Bad Request", message)  ※必要なら fields 付きに拡張
  → businessHandler → 同じ ErrorResponse 形
```

「統一」のイメージ:

```text
クライアントから見た JSON は同じ
{ status, error, message, fields? }
```

例外の継承ツリーを1本にする必要はない。

## fields も手チェックで揃えたいとき

`BusinessException` に `Map<String, String> fields` を足し、Handler で `ErrorResponse.withField(...)` する（または静的ファクトリ `badRequest(message, fields)`）。

これで `@Valid` と手チェックの **見た目は統一**、投げ方は層ごとに自然。

## やらない方がよい

| やり方 | 理由 |
|--------|------|
| Service で `new MethodArgumentNotValidException(...)` | Web API 依存・コンストラクタが重い |
| 手チェックも全部 `@Valid` に押し込む | trim 後判定・他エンティティ参照など表現しにくいものがある |
| 手チェックだけ独自 JSON | openapi のエラー形が割れる |

## Q: record（業務層でもない）でも BusinessException でよいか

**原則いいえ。** `BusinessException` は **ユースケース／ドメインの業務失敗**用。Request の `record` は入力の形であり業務層ではない。

| 置き場 | バリデーションの出し方 |
|--------|------------------------|
| `CreateEventRequest` 等の **record** | `@NotBlank` / `@Size` など **Bean Validation**。自分で例外を投げない |
| **Service** | 手チェック → `BusinessException`（400/404/409 等） |
| **Domain Entity** | 不変条件はファクトリで弾く。HTTP を知らない例外（または後で Service が変換）が理想 |

record の compact constructor で `throw new BusinessException(...)` すると:

- DTO が HTTP / API エラー契約に依存する
- `@Valid` 経路と二重・形がズレやすい
- 「業務例外」という名前と役割が合わない

## Q: `AccessCode` のような値オブジェクト record は？

Request DTO の record とは別物。**ドメインの値オブジェクト**なら compact constructor での検査は妥当。

```text
Request DTO record  … Bean Validation（例外は自分で投げない）
AccessCode 値オブジェクト … 不正な値の誕生を阻止（IllegalArgumentException 等）
```

投げてよいのは **`IllegalArgumentException`（または専用のドメイン例外）**。`BusinessException` は HTTP 寄りなので値オブジェクトには載せない。

API へ出すときは Service で変換する例:

```text
try { AccessCode.from(raw); }
catch (IllegalArgumentException ex) {
  throw new BusinessException(BAD_REQUEST, "Bad Request", ex.getMessage());
}
```

または Handler で `IllegalArgumentException` → 400（使いすぎに注意。フレームワーク由来の IAE も拾う）。

### 提示コードへのコメント

| 点 | 評価 |
|----|------|
| `generate` + `SecureRandom` + `StringBuilder` | よい（EJ とも整合） |
| compact ctor で長さ・形式を保証 | よい（不正オブジェクトを作らない） |
| `IllegalArgumentException` | 値オブジェクトとして適切 |
| `CHAR_POOL` が I/O/l を除外 | UX よいが、regex はそれらを許可 → **生成と from のルールを揃える** |
| プロジェクト制約 | openapi/constraints は `[A-Za-z0-9]{8}`。除外文字にするなら **docs も合わせる** |

### `generate` で try-catch して BusinessException に変換する？

**基本不要。** `generate()` は自前の文字集合から 8 桁を組み立てるので、検証と生成が一致していれば compact ctor は落ちない。落ちるなら実装バグ（catch して業務 400 にする対象ではない）。

| 呼び出し | try-catch → BusinessException |
|----------|-------------------------------|
| `AccessCode.generate()` | **しない**（成功前提。失敗はバグ） |
| `AccessCode.from(ユーザー入力)` | **する**（または Controller 手前で `@Pattern`） |
| DB から読んだ文字列を包む | 壊れているならデータ不整合。400 より 500 / ログ向き |

```text
発行: AccessCode.generate().value()  … try なしで Event.create へ
入力: AccessCode.from(code)          … IAE を BusinessException(400) へ
```
