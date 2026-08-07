# @PreAuthorize 403 が GlobalExceptionHandler で 500 になる

## 症状

`MeControllerTest.me_HOST以外なら403` が `expected:<403> but was:<500>`。

## 原因

`@PreAuthorize` 失敗の `AuthorizationDeniedException` が `@ExceptionHandler(Exception.class)` に飲まれ 500 になる。

## 修復

認可例外を専用ハンドラで 403 化。

## Q: catch-all 内の if より Handler を分けるべきか

**分けた方がよい。** `ErrorHandler` 内の `instanceof AuthenticationException` は、例外の種類ごとの入り口がぼやける。

| 案 | 可読性 |
|----|--------|
| catch-all で if 分岐して再送出 | 低い（500 ハンドラに別関心が混ざる） |
| `@ExceptionHandler(AuthenticationException.class)` を別メソッド | 高い（型＝入り口が明示） |

403 用の `AccessDenied` / `AuthorizationDenied` は **返す JSON が同じ**なので、1メソッドに `@ExceptionHandler({ A.class, B.class })` で十分（ここは「二つに分け必須」ではない）。

401 は Filter の EntryPoint が本筋だが、Advice に届いたとき用に専用ハンドラを置くと catch-all から if を消せる。

```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ErrorResponse> authenticationHandler(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(401, "Unauthorized", "ログインが必要です"));
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> errorHandler(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "サーバーエラーが発生しました。"));
}
```

【EJ】公開面最小化／関心の分離 … 1ハンドラ1責務。catch-all に分岐を足さない。
