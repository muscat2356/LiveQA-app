# Service で Response DTO に変換すべきか（三層）

## 質問

Service 層で Response に変換した方が、三層構造として正しいのでは？

## 結論

**「業務の組み立て＋DTO 化」を Service（または専用の応用サービス）に置くのは正しい選択。**  
ただし **Service が `ResponseEntity`（HTTP）を返すのはやりすぎ。** 返すのは `MeResponse` などの DTO まで。

| 層 | 持つもの | 持たないもの |
|----|----------|--------------|
| Controller | HTTP・認証 principal の受け取り、`ResponseEntity.ok(...)` | 業務分岐・Repository 直叩き |
| Service | ユースケース、Entity 取得、**DTO への組み立て** | `HttpServletResponse` / `ResponseEntity` |
| Repository | DB アクセス | DTO・HTTP |

## 二つの置き場（どちらも三層としてあり）

### A. Controller で DTO 化（薄い変換）

```text
Controller: OidcUser + UserService.require... → new MeResponse(...)
Service: User を返すだけ
```

- 変換が単純なときに短く済む  
- Controller に「クレームと DB の突合」が集まりやすい  

### B. Service で DTO 化（推奨しやすい）

```text
Controller: oidcUser を渡す → ResponseEntity.ok(meService.currentHost(oidcUser))
Service: users 復元 + クレームから MeResponse を組み立てて返す
```

- **三層の「業務は Service」** に近い  
- Controller は HTTP と認可注釈に集中  
- 同じ組み立てを別口から再利用しやすい  

本プロジェクトの「Controller に業務分岐を書かない」には **B がよく合う。**

## 組み立てを分割する（推奨形）

`requireByOidcSubject`（User）と、Response 組み立てを分ける。

```java
// 1. DB 復元（既存）
public User requireByOidcSubject(String oidcSubject) { ... }

// 2. 組み立て専用（戻りは MeResponse。ResponseEntity ではない）
public MeResponse toMeResponse(User local, OidcUser oidcUser) {
    final String displayName = oidcUser.getFullName() != null
            ? oidcUser.getFullName()
            : oidcUser.getPreferredUsername();
    return new MeResponse(
            local.getId(),
            displayName,
            oidcUser.getEmail(),
            local.getRole()
    );
}

// 3. ユースケース（薄いオーケストレーション）
public MeResponse currentHost(OidcUser oidcUser) {
    final User local = requireByOidcSubject(oidcUser.getSubject());
    return toMeResponse(local, oidcUser);
}
```

| メソッド | 戻り値 | 役割 |
|----------|--------|------|
| `requireByOidcSubject` | `User` | DB 復元 |
| `toMeResponse` | **`MeResponse`** | 組み立てだけ（分割の本体） |
| `currentHost` | `MeResponse` | 上二つを順に呼ぶ |
| Controller | **`ResponseEntity<MeResponse>`** | HTTP の箱 |

「Response にした」＝ **API 用 DTO**。HTTP の `ResponseEntity` まで Service に載せない。

## 【EJ】

- **公開 API の最小化** … Controller の公開面は薄く、組み立ては Service  
- **不変性** … 返すのは `MeResponse` record  
- **あえて外す** … Service が OIDC 型（`OidcUser`）を知るのは応用サービスとしては実務でよくある。厳密にドメイン純度を出すなら `record MeCommand(String sub, String name, String email)` に変換してから Service へ、でも MVP では過剰になりやすい  

## やらないこと

- Service が `ResponseEntity` を返す  
- Repository が `MeResponse` を返す  
- Entity を Controller から直接 JSON にする  

## Q: Service の Javadoc はコンストラクタまで付ける？

**付けない。** `EventService` 型。クラス（どの集約の業務か）＋公開メソッド（分岐・HTTP相当の結果）＋ヘルパー／DTO変換。

コンストラクタとフィールドはコメントしない。`toXxxResponse` は「レスポンスDTOへ変換するメソッド」。エラーは本文に 404 / 409 / 401 を書く。  
