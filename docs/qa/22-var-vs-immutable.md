# `var` とイミュータブル（EJ）は別軸

## 質問

`var local = ...` と書く理由は？ EJ ならイミュータブルを意識すべきでは？

## 結論

**その通り、イミュータブルは意識すべき。ただし `var` の話とは別軸。**

| 軸 | 何を決めるか | 例 |
|----|--------------|-----|
| **型の書き方** | 左辺に型名を書くか推論するか | `User local` vs `var local` |
| **参照の再代入** | 変数に別オブジェクトを入れ直せるか | `final User local` |
| **オブジェクトの不変** | 中身のフィールドを変えられるか | `record` / 不変クラス vs JPA Entity |

`var` は **型名の省略（推論）** だけで、可変にも不変にもしない。

## `var` を使う理由（ある場合）

- 右辺で型が明らかなとき、左辺の重複を減らす（可読性の好み）
- 例: `var local = userService.requireByOidcSubject(...)` → 戻りが `User` と分かる

**必須ではない。** 学習・レビューでは明示型の方が追いやすいことも多い。

```java
// 同じ意味（どちらも「参照は再代入できる」）
User local = userService.requireByOidcSubject(sub);
var local = userService.requireByOidcSubject(sub);
```

## EJ のイミュータブルとどう合わせるか

1. **API の入出力** → `record`（不変オブジェクト）… 本プロジェクト方針  
2. **ローカル変数の再代入を防ぐ** → **`final`**（`var` ではなくこちら）

```java
final User local = userService.requireByOidcSubject(oidcUser.getSubject());
// または
final var local = userService.requireByOidcSubject(oidcUser.getSubject());
```

3. **JPA Entity（`User`）** → 永続化都合で**可変**。【EJ】あえて外す。だから `final User local` でも「参照は固定、中身のフィールドは変わりうる」

## `/api/me` でのおすすめ

```java
final User local = userService.requireByOidcSubject(oidcUser.getSubject());
return ResponseEntity.ok(new MeResponse(
        local.getId(),
        displayName,
        oidcUser.getEmail(),
        local.getRole()
));
```

- 型を明示（学習・レビュー向き）  
- `final` で参照の再代入を防ぐ  
- 応答は不変の `MeResponse` record  

## Q: わざわざ `var` にする必要は？推論のメリットは？

**この `requireByOidcSubject` の結果では、`User` と明示した方がよい。`var` 必須ではない。**

### `var` のメリットが出る例

- 右辺が長く、左辺の型が**繰り返しで読みにくい**とき  
  `var map = new HashMap<String, List<OrderLine>>();`  
  （菱形演算子や長いジェネリクス）
- 匿名や中間型で、型名が本質でないとき（스트림の一時変数など、チーム次第）

### メリットが薄い／逆効果の例（今回）

```java
var local = userService.requireByOidcSubject(...);  // 型が一目で弱い
User local = userService.requireByOidcSubject(...); // 学習・レビュー向き
```

- 戻り型は `User` ひとつで、左辺に書いても冗長にならない  
- 学習リポでは **「何が返ってきたか」を型で見せた方が理解が速い**  
- IDE 無しの diff レビューでも追いやすい  

## 本プロジェクトでの優先（確定）

**冗長性の回避より、可読性と堅牢な EJ を優先する。**

写経・実装では:

1. 型を明示する（`User` / `ResponseEntity<MeResponse>`）
2. 再代入しないなら `final`
3. API 入出力は不変（`record`）
4. 短くするための `var` は使わない（長いジェネリクスで可読性が上がるときだけ例外）

Agent もこの順で例を出す。
