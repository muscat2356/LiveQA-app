# CSRF ヘッダ検証はメソッドにどう定義するか

## 質問

CSRF ヘッダの検証は、どのようにメソッドに定義すればよいか。

## 結論

**Controller の各メソッドに CSRF 用アノテーションは付けない。**  
検証は `SecurityConfig` の Filter（`CsrfFilter`）が **HTTP メソッド単位で自動**に行う。

| 層 | やること |
|----|----------|
| `SecurityConfig` | `.csrf(...)` で Cookie リポジトリ＋ヘッダ読み取りを有効化（定義はここだけ） |
| Controller | 普通に `@PostMapping` 等。CSRF 専用の宣言は不要 |
| クライアント / テスト | Cookie `XSRF-TOKEN` の値をヘッダ `X-XSRF-TOKEN` に載せる |

## 本番の定義場所（すでにこのプロジェクト）

```91:94:backend/src/main/java/com/liveqa/config/SecurityConfig.java
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(csrfHandler)
            )
```

| 設定 | 意味 |
|------|------|
| `CookieCsrfTokenRepository.withHttpOnlyFalse()` | Cookie `XSRF-TOKEN` を JS から読めるように発行 |
| `setCsrfRequestAttributeName(null)` | SPA 向け。ヘッダ `X-XSRF-TOKEN` でトークンを渡す前提 |

## どのメソッドが検証対象か

Spring Security 既定（変更していなければ）:

| HTTP | CSRF |
|------|------|
| GET / HEAD / TRACE / OPTIONS | **検証しない** |
| POST / PUT / PATCH / DELETE | **検証する** |

だから:

```java
@GetMapping("/me")          // CSRF 不要
@PostMapping("/events")     // CSRF 必須（Filter が自動検証）
@PostMapping("/auth/logout") // 同上
```

「この POST だけ CSRF」とメソッドに書くのではなく、**POST なら Filter が効く**。

特定 URL だけ除外したいときだけ Config 側:

```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/webhooks/**")  // 例。本プロジェクトでは原則使わない
    ...
)
```

## クライアント側（フロント / Bruno）

```text
1. 事前に何らかの応答で Set-Cookie: XSRF-TOKEN=abc を受ける
2. 状態変更リクエスト:
   Cookie: SESSION=...; XSRF-TOKEN=abc
   X-XSRF-TOKEN: abc
```

合い言葉 Cookie とヘッダが一致するか Filter が検証する。

## テスト（MockMvc）での「定義」

Controller に書くのではなく、リクエストに CSRF を載せる:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

mockMvc.perform(post("/api/events")
        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_HOST")))
        .with(csrf())   // テスト用に正しい CSRF を付与
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"...\"}"))
    .andExpect(status().isCreated());
```

| ケース | 期待 |
|--------|------|
| `.with(csrf())` あり | CSRF は通る（認可・業務の検証に進む） |
| `.with(csrf())` なしの POST | 403（Invalid CSRF）になりやすい |

GET のテストに `csrf()` は不要。

## データの流れ

```text
POST /api/... + Cookie XSRF-TOKEN + Header X-XSRF-TOKEN
  → CsrfFilter が 検証（一致？）
  → NG なら 403（Controller 未到達）
  → OK なら Controller メソッドへ
```

## 一文

> CSRF はメソッドアノテーションではなく SecurityConfig の Filter。Controller は POST を普通に書き、クライアント／MockMvc が `X-XSRF-TOKEN`（または `.with(csrf())`）を付ける。
