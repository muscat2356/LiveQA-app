# ResponseEntity を使う／使わない

## 質問

`MeController` などで `ResponseEntity` を使わない理由は？使った方がよいのでは？

## 結論（チーム方針・更新）

**Controller の戻り値は `ResponseEntity` で統一する。**  
成功 200 も `ResponseEntity.ok(body)` と書き、ステータスが一目で分かるようにする（可読性・統一感）。

| ケース | 書き方の例 |
|--------|------------|
| 200 + JSON | `return ResponseEntity.ok(meResponse);` |
| 201 作成 | `return ResponseEntity.status(HttpStatus.CREATED).body(...);` または `created(uri).body(...)` |
| 204 ボディ無し | `return ResponseEntity.noContent().build();` |

DTO 自体（`MeResponse` record）と `ResponseEntity` は別物。  
**包むのは HTTP の箱、中身は record。**

### `/api/me` の形（方針どおり）

```java
@GetMapping("/me")
@PreAuthorize("hasRole('HOST')")
public ResponseEntity<MeResponse> me(@AuthenticationPrincipal OidcUser oidcUser) {
    // ...
    return ResponseEntity.ok(new MeResponse(...));
}
```

401 は従来どおり Security EntryPoint（Controller では返さない）。

## 以前の「直返しでもよい」との関係

技術的には DTO 直返しでも 200 になる。  
**このリポでは統一のため `ResponseEntity` を選ぶ**（guest 既存の `ResponseEntity.ok` に揃える）。

## 二つの書き方

```java
// A: ボディ型を返す（デフォルト 200 + JSON）
@GetMapping("/me")
public MeResponse me(...) {
    return new MeResponse(...);
}

// B: ステータス／ヘッダも明示
@GetMapping("/me")
public ResponseEntity<MeResponse> me(...) {
    return ResponseEntity.ok(new MeResponse(...));
}
```

| | A（ボディ直返し） | B（`ResponseEntity`） |
|--|-------------------|------------------------|
| 向く | 成功が常に 200、ヘッダ不要 | 201 / 204 / 302、`Location` など |
| `/api/me` | **十分**（openapi も 200 + JSON） | 書いてもよいが冗長になりやすい |
| 401 | Controller まで来ない（Security EntryPoint） | 同じ（ここで 401 を返す必要はない） |

## `/api/me` で使わなくてよい理由

1. openapi の成功系は **200 + ボディ**だけ  
2. 未認証 401 は **Security** が返す（Controller の戻り値では制御しない）  
3. Spring MVC は `@RestController` で戻り値を JSON 化し、ステータス未指定なら **200**  

「使った方が絶対よい」わけではなく、**制御したいものがあるか**で選ぶ。

## 使った方がよい例（このプロジェクトでも）

| API | 理由 |
|-----|------|
| `POST /api/events` → **201** | 作成成功のステータス |
| `POST .../logout` → **204** または **302** | ボディ無し／リダイレクト |
| 作成後 `Location` ヘッダ | `ResponseEntity.created(uri).body(...)` |

guest は既に `ResponseEntity.ok(Map.of(...))` を使用中。

## 方針（学習用）

- **統一:** すべての Controller メソッドは `ResponseEntity` を返す（200 も `.ok`）
- 中身の DTO は `record` のまま
- 401/403 の JSON は Security / 例外ハンドラ側（Controller で無理に包まない）
