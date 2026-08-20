# F-04 参加（Member B）— 到達点と写経順

## Q. F-04 で B がやることは？

scope: **参加ページは A 主 / B＝匿名セッション（`voter_key`）**。  
Join 照会（`GET /api/join/{code}`）は F-02 で済。F-04 の本丸は **guest + voter_key を F-05 で使える形に整える**。

## 到達点（現状）

| 項目 | 状態 |
|------|------|
| `POST /api/auth/guest` | あり（Service + `GuestResponse`） |
| `GET /api/join/{joinCode}` | あり＋テスト |
| Security `permitAll`（guest / join） | あり |
| Guest テスト | あり |
| openapi の guest 節 | あり |
| `VoterKey` 共通化（`g:` / `u:`） | あり（HOST 利用は F-05） |
| 質問・いいね API | **F-05**（今はやらない） |

## 写経順（小単位）

1. **openapi** に `POST /api/auth/guest` を追記  
2. **`VoterKey`**（record または utility）+ 単体テスト  
3. **`GuestResponse` record** + Controller 差し替え + **GuestSessionControllerTest**  
4.（任意）`GET /api/auth/guest` 復元、または F-05 直前に `VoterKeyResolver`（Session/Oidc → 文字列）

## 写経② — GuestSessionService（**旧案・非推奨**）

> **訂正:** Service に `HttpSession` を渡す案は三層的に不適切。下記の旧コードは使わず、末尾「Service が HttpSession に触ってよいか？」の訂正版写経を正とする。


### なぜ

Controller から Session 操作と DTO 組み立てを剥がす。DB は触らない（F-05 の VoteRepository とは別）。

### 本番

**新規** `service/GuestSessionService.java`

```java
package com.liveqa.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.liveqa.domain.VoterKey;
import com.liveqa.dto.response.GuestResponse;

import jakarta.servlet.http.HttpSession;

@Service
public class GuestSessionService {

    public static final String ATTR_GUEST_ID = "guestId";

    public GuestResponse issueOrReuse(HttpSession session) {
        final Object existing = session.getAttribute(ATTR_GUEST_ID);
        final String guestId = existing instanceof String s ? s : UUID.randomUUID().toString();
        session.setAttribute(ATTR_GUEST_ID, guestId);
        return new GuestResponse(guestId, VoterKey.guest(guestId).value());
    }
}
```

**差し替え** `GuestSessionController`

```java
@RestController
@RequestMapping("/api/auth")
public class GuestSessionController {

    private final GuestSessionService guestSessionService;

    public GuestSessionController(GuestSessionService guestSessionService) {
        this.guestSessionService = guestSessionService;
    }

    @PostMapping("/guest")
    public ResponseEntity<GuestResponse> createGuestSession(HttpSession session) {
        return ResponseEntity.ok(guestSessionService.issueOrReuse(session));
    }
}
```

- `ATTR_GUEST_ID` は Controller から削除（Service 側へ）
- `Map` / `UUID` の import は Controller から不要

### テスト写経（セット）

**`GuestSessionServiceTest`**（JUnit + Mockito の `HttpSession`）

```java
@Test
void issueOrReuse_無ければ新規guestId() {
    final HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(GuestSessionService.ATTR_GUEST_ID)).thenReturn(null);

    final GuestResponse response = guestSessionService.issueOrReuse(session);

    assertThat(response.guestId()).isNotBlank();
    assertThat(response.voterKey()).isEqualTo("g:" + response.guestId());
    verify(session).setAttribute(eq(GuestSessionService.ATTR_GUEST_ID), eq(response.guestId()));
}

@Test
void issueOrReuse_既存があれば再利用() {
    final HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(GuestSessionService.ATTR_GUEST_ID)).thenReturn("fixed-id");

    final GuestResponse response = guestSessionService.issueOrReuse(session);

    assertThat(response.guestId()).isEqualTo("fixed-id");
    assertThat(response.voterKey()).isEqualTo("g:fixed-id");
}
```

**`GuestSessionControllerTest`**（任意・薄い）

- `@WebMvcTest(GuestSessionController.class)` + `@MockitoBean GuestSessionService`
- POST `/api/auth/guest` → Service の戻りを JSON で返すことだけ確認（`.with(csrf())` はフィルタ有効時）

```bash
mvn test -Dtest=GuestSessionServiceTest,GuestSessionControllerTest
```

### 流れ

```
POST /api/auth/guest + CSRF
  → Controller（HttpSession 受け取り → Service 委譲 → ResponseEntity）
  → GuestSessionService
       → Session 属性 guestId 復元／発行（→ Redis）
       → VoterKey.guest → GuestResponse
```

### 【EJ】（訂正後の正）

```text
【EJ】公開面最小化 … Servlet API は Controller まで
【EJ】依存性注入 … GuestSessionController コンストラクタ
【EJ】不変性 … GuestResponse / VoterKey は record
【EJ】あえて外す … Service に HttpSession を渡さない／Repository なし
```

## Q. Service が HttpSession に触ってよいか？

**結論: 触らない。** `HttpSession` は Web 関心（Controller）。backend 規約でも Service は `ResponseEntity` を持たないのと同系統。

| 層 | する | しない |
|----|------|--------|
| Controller | `get/setAttribute`、`ResponseEntity` | `VoterKey` 組み立て本体 |
| Service | 既存 id（`String` / null）→ 発行判定、`VoterKey`、`GuestResponse` | `HttpSession` |
| Repository | （guest では無し） | — |

## Q. Guest に Service / Repository は要るか？

### 結論

| 層 | F-04 guest | 理由 |
|----|------------|------|
| **Repository（JPA）** | **作らない** | guestId の正は Session→Redis。ER に guests 表なし |
| **Service** | **あるとよい** | 発行／再利用と DTO 組み立て。**引数は素の String** |
| **VoteRepository** | **F-05** | DB に載るのは `voter_key` 文字列 |

### 正しい形の例

```text
Controller
  → existing = (String) session.getAttribute("guestId")  // Web
  → response = service.issueOrReuse(existing)            // 業務
  → session.setAttribute("guestId", response.guestId()) // Web
  → ResponseEntity.ok(response)
```

### 写経（訂正版）Service

```java
@Service
public class GuestSessionService {

    public GuestResponse issueOrReuse(String existingGuestId) {
        final String guestId = (existingGuestId == null || existingGuestId.isBlank())
                ? UUID.randomUUID().toString()
                : existingGuestId;
        return new GuestResponse(guestId, VoterKey.guest(guestId).value());
    }
}
```

### 写経 Controller（Session はここだけ）

```java
public static final String ATTR_GUEST_ID = "guestId";

@PostMapping("/guest")
public ResponseEntity<GuestResponse> createGuestSession(HttpSession session) {
    final Object existing = session.getAttribute(ATTR_GUEST_ID);
    final String existingId = existing instanceof String s ? s : null;
    final GuestResponse response = guestSessionService.issueOrReuse(existingId);
    session.setAttribute(ATTR_GUEST_ID, response.guestId());
    return ResponseEntity.ok(response);
}
```

## Q. GuestSessionControllerTest は CSRF を見ていない？

**認識どおり。** `addFilters = false` は Security Filter（CSRF 含む）を切った **Controller 配線のスライス**。openapi の「CSRF 必須」と矛盾しているのではなく、**検証レイヤが違う**。

| 層 | 何を保証するか | この PR のテスト |
|----|----------------|------------------|
| Filter（SecurityConfig） | CSRF あり→通過／なし→403 | **対象外**（`addFilters=false`） |
| Controller | Session↔Service↔JSON | **対象**（現状） |

本番では `SecurityConfig` の CSRF が全 POST に効く（Controller に CSRF 宣言は無い）。[`docs/qa/35-csrf-header-how-to-define.md`](35-csrf-header-how-to-define.md)

### 別途確認の想定は？

はい。次のどちらか（または両方）:

1. **手動** … Bruno/curl で `POST /api/auth/guest`  
   - CSRF あり → 200  
   - CSRF なし → 403  
   → PR テンプレの「CSRF を確認した」にチェック  
2. **自動** … `GuestSessionControllerCsrfTest`  
   - `.with(csrf())` → 200  
   - csrf なし → 403  
   （配線スライス `GuestSessionControllerTest` は `addFilters=false` のまま）

JoinControllerTest も同様に `addFilters=false`（GET なので CSRF 論点が薄い）。Event の POST 系は以前 `.with(csrf())` 付きで Filter 有効側を見るパターン。

### 一文

> スライステストは CSRF 対象外で意図的。契約上の CSRF 必須は Filter＋手動（または Filter 有効テスト）で担保する想定でよい。

**結論: F-04 の B 範囲として十分。openapi / `VoterKey` リネーム反映済み。**

| 項目 | 判定 |
|------|------|
| Session は Controller / 業務は Service | OK |
| Repository なし | OK（設計どおり） |
| `VoterKey` + `GuestResponse` | OK |
| テスト（Service / Controller / VoterKey） | OK |
| Security `permitAll` | OK |
| openapi に `POST /api/auth/guest` | 反映済み |
| クラス名 `VoterKey` | 反映済み |
| Service の `final` / 引数名 | 他クラスと揃えるとよい |

HOST の `u:{id}` 利用・投票未確立 401 は **F-05**。

## Q. GuestSessionService の引数は Optional ではなく VoterKeySource？

**Guest / Missing ならそうする。`form` は使わない。**

| 組み立て | 用途 |
|----------|------|
| `VoterKeySources.form(oidc, session)` | 投票者は誰か（Host 優先） |
| `VoterKeySources.fromGuestSession(session)` | 匿名 guestId の有無だけ |

`issueOrReuse` は `switch`。`Guest` 再利用 / `Missing` 新規 UUID / `Host` は 400（form を繋いだときに壊れる）。Session の書き込みは今どおり Controller。
