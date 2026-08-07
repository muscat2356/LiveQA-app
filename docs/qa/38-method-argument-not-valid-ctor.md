# MethodArgumentNotValidException のコンストラクタ引数

## 質問

`MethodArgumentNotValidException` を投げる（new する）ときの引数は？

## 答え

コンストラクタは **2引数**（Spring Web 6）:

```text
MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult)
```

| 引数 | 型 | 意味 |
|------|-----|------|
| 1 | `MethodParameter` | どのコントローラ引数の検証か（本番では `@RequestBody` のパラメータ） |
| 2 | `BindingResult` | フィールドエラーなどの検証結果 |

本番ではアプリが `throw new` することはほぼ無い。`@Valid` 失敗時に Spring MVC が発行し、`GlobalExceptionHandler#validationHandler` が受ける。

## テストで new する例

```java
final BindingResult bindingResult = mock(BindingResult.class);
when(bindingResult.getFieldErrors()).thenReturn(List.of(
        new FieldError("createEventRequest", "title", "1〜100文字で入力してください")
));

// 第1引数はユニットテストでは null でも動くことが多い
final MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(null, bindingResult);
```

厳密にやるなら `MethodParameter` をモック／実メソッドから取る。

## 名前注意

正しくは **`MethodArgumentNotValidException`**（`Valid` の前に `Not`）。
