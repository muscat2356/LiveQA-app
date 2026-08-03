# UserService 単体テスト解説（いま書いている骨組み向け）

## 質問

テスト解説（`UserServiceTest` を書き始めている）。

---

## 単体テストで何をしているか

**本物の DB / Redis / Keycloak を使わず**、`UserService` の分岐だけを確かめる。

```text
テストコード
  → 偽の UserRepository（@Mock）を用意
  → 「find したら empty」「save したらこの User」など振る舞いを決める（when）
  → 本物の UserService に偽 Repository を渡して呼ぶ
  → 戻り値や「save が呼ばれたか」を検証（assert / verify）
```

| 用語 | 正体 | 意味 |
|------|------|------|
| `@Test` | JUnit 5 のアノテーション | 「これが1本のテスト」 |
| `@ExtendWith(MockitoExtension.class)` | JUnit に Mockito を載せる | `@Mock` が動くようにする |
| `@Mock` | 偽オブジェクト | 中身は空。`when` で返事を決める |
| `@InjectMocks` | 本物の Service | `@Mock` をコンストラクタ等に**注入**して作る |
| `when(...).thenReturn(...)` | 振る舞いの指定 | 「こう聞かれたらこう返す」 |
| `verify(...)` | 呼び出し検証 | 「save が呼ばれた／呼ばれていない」 |
| `assertThat(...)` | AssertJ | 戻り値が期待どおりか |

---

## いまの草稿で直した方がよい点

```java
@Mock
UserService userService;  // ← ここが逆
```

**Service はモックしない。Repository をモックして、本物の Service を動かす。**

| NG | OK |
|----|-----|
| `@Mock UserService` | `@InjectMocks UserService userService` |
| `import org.springframework.boot...security...User` | `import com.liveqa.domain.User` |

Security の `User` は別物。ドメインは `com.liveqa.domain.User`。

正しい骨格:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    @InjectMocks
    UserService userService;  // 本物。中に偽 repository が入る

    @Test
    void provisionHost_新規ならsaveする() {
        when(repository.findByOidcSubject("sub-1"))
                .thenReturn(Optional.empty());
        when(repository.save(any(com.liveqa.domain.User.class)))
                .thenAnswer(inv -> {
                    com.liveqa.domain.User u = inv.getArgument(0);
                    // id は JPA が付ける想定 → テストでは ReflectionTestUtils でセット可
                    return u;
                });

        final com.liveqa.domain.User result = userService.provisionHost("sub-1");

        // assert / verify ...
    }
}
```

---

## `provisionHost` で確かめること（例）

本番コード:

```text
findByOidcSubject
  → 空なら save(User.hostOf(sub))
  → あればそのまま返す（save しない）
```

| テスト名のイメージ | 仕掛け（when） | 期待 |
|--------------------|----------------|------|
| 新規なら save | find → empty / save → User | save が **1回**呼ばれる |
| 既存なら save しない | find → Optional.of(既存) | save は **never** |
| require で無し | find → empty | `IllegalStateException` |
| toMeResponse | User + mock OidcUser | id/role/displayName が載る |

---

## データの流れ（1テストの中）

```text
① when: findByOidcSubject("sub-1") → Optional.empty()
② when: save(any) → 渡された User を返す（必要なら id をセット）
③ 実行: userService.provisionHost("sub-1")
     → Service が find → empty → hostOf → save
④ 検証: result の oidcSubject / save が呼ばれたこと
```

DB には一度も繋がらない。**Repository のインタフェース契約だけ**を偽っている。

---

## Controller テストとの違い（次）

| | `UserServiceTest` | `MeControllerTest` |
|--|-------------------|---------------------|
| 起動 | Mockito のみ（軽い） | `@WebMvcTest`（Spring MVC の一部） |
| 偽にするもの | `UserRepository` | `UserService` |
| 本物 | `UserService` | `MeController` |
| 見ること | JIT / DTO 組み立て | HTTP 200/401、`ResponseEntity` |

三層と同じで、**一段下をモックし、一段を本物で試す**。

---

## 【EJ】テスト視点

- **公開 API 最小化** … テストのために Entity に setter を増やさない（`ReflectionTestUtils` で id）
- **依存性注入** … コンストラクタ注入だから `@InjectMocks` が効く
- Service 全体を `@Mock` すると **何も検証していない**のに等しいので避ける

---

## 実装済みテスト（2026-08）

| クラス | 内容 |
|--------|------|
| `UserServiceTest` | 新規/既存 `provisionHost`、`requireByOidcSubject`、`toMeResponse`（fullName / preferredUsername）、`currentHost` |
| `MeControllerTest` | `@WebMvcTest` + `oidcLogin` で 200、未認証 401。`OAuth2ClientAutoConfiguration` は **exclude**（issuer 解決で Keycloak に繋がらないようにする） |

既存分岐の罠: `when(findByOidcSubject("sub-1"))` と `provisionHost("sub-1")` の **subject を揃える**（`"missing"` のまま呼ぶと stub が効かない）。

---

## 関連

- 写経全文は会話の「実装」単位 / [24-test-deps-jacoco.md](24-test-deps-jacoco.md)
- Controller 側の注意は下節と [06-me-controller.md](06-me-controller.md)
