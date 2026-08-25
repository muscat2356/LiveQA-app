# Java 形（JPA / DTO / 三層 / テスト）

機能をまたぐ方針。個別 API は `f01`〜`f05`。正: [.cursor/rules/live-qa-backend.mdc](../../.cursor/rules/live-qa-backend.mdc)。

## 三層

```text
controller → service → repository → domain
dto/ … request|response（record）
```

| 層 | する | しない |
|----|------|--------|
| Controller | principal、`@PreAuthorize`、`ResponseEntity` | 業務分岐、DTO 組み立て本体 |
| Service | ユースケース、DB 復元、**DTO 組み立て** | `ResponseEntity` / `HttpSession` |
| Repository | DB | DTO / HTTP |
| Entity | 永続化 | API 直出し |

## DTO は record

Request/Response は `record`（不変）。Entity は class（可変＋静的ファクトリ）。

**Controller の戻りは `ResponseEntity` 統一**（200=`ok`、201=created、204=`noContent`）。包むのは HTTP の箱、中身は record。

DTO 化は Service（推奨）。Service が `ResponseEntity` まで返すのはやりすぎ。

## Q. 変換メソッドを Service から Entity（domain）へ移すか？

**Entity には移さない。Response の静的ファクトリ（`form`）へ寄せるのは採る。**

| 案 | EJ の気持ち | このリポ |
|----|-------------|----------|
| `event.toDetailResponse()` | Entity に寄せる | **採らない。** Entity は永続化。API 直出し禁止 |
| `PollResponse.form(poll)` | 項目1 静的ファクトリ。作られる型のクラス | **採る。** 形の写像（A→B）は DTO。stream の `map` が自然 |
| Service が材料を集めて `form` を呼ぶ | オーケストレーション | **正。** 票・得票・Oidc クレームは Service |

変換は行の事実だけでは足りないことが多い。足りない分は **引数で渡す**。DTO が Repository を見ない。

| DTO | Entity に無いもの | 誰が渡す |
|-----|-------------------|----------|
| `QuestionResponse.votedByMe` | 今の SESSION の票 | Service |
| `PollResponse.totalResponses` / `myOptionIds` | `poll_responses` | Service（F-11） |
| `MeResponse.displayName` / `email` | `OidcUser` クレーム | Service |

```text
// F-10（得票なし）
.polls.stream().map(PollResponse::form).toList()

// F-11（材料は Service）
PollResponse.form(poll, totalResponses, myOptionIds)
```

`dto` が `domain` を import するのは Response だけ許容（出力の写像）。Request → Entity はこれまでどおり Service / `Poll.create`。

項目1 の静的ファクトリは **new する型のクラス** に置く。名前はこのリポの `form`（`JoinCode.form` / `VoterKeySources.form`）に揃える。

| new するもの | 置き場 | 例 |
|--------------|--------|-----|
| Entity / 値オブジェクト | **domain** | `Event.create` / `User.hostOf` / `JoinCode.form` |
| Response record | **dto の `form`** | `PollResponse.form` / `PollOptionResponse.form` |

Service の `toXxxResponse` は、`form` へ移したら消す。一気にやらなくてよい。ポールで始めて、質問・イベントは触った単位で寄せる。

【EJ】静的ファクトリ … `form` は Response を new する入口  
【EJ】公開面最小化 … Entity は API を知らない  
【EJ】あえて外す … F-10 の `form` は count=0 固定。F-11 で引数を足す

## `var` と不変性は別軸

| 軸 | 決めること |
|----|------------|
| 型の書き方 | `User local` vs `var local` |
| 再代入 | `final` |
| オブジェクトの不変 | `record` vs JPA Entity |

このリポは **明示型 + `final`**。`var` は長いジェネリクス向きで、学習・レビューでは劣後。

## JPA

オブジェクト ↔ 行の仕様。実装は Hibernate。DDL の正は **Flyway**（`ddl-auto: validate`）。既存 `V*` は不改変、追加のみ。

- 生成は静的ファクトリ（`hostOf` / `create` / `post` / `cast`）。`protected` コンストラクタは JPA 用＋勝手な `new` 防止
- `hostOf` は Builder ではない。SecurityContext は `OidcUser`（sub）を持ち、User 行は Service が復元する
- Entity の全フィールド equals は安易に付けない
- UNIQUE 名は Flyway と `@UniqueConstraint(name=...)` を揃える（`@Column(unique=true)` だけでは名前が付かない）

### JPA 管理下の save

同じ TX で managed な Entity を書き換えたら、コミット時に dirty checking で UPDATE される。`save` は省略可。新規（transient）だけ `save` が必要。

`save` 失敗は戻り値 null ではない（例外）。予防できる衝突は exists。業務 UNIQUE（いいね）は制約名で 409。想定外は 500。

## Optional（項目55）

**戻りには使う。引数・フィールドには使わない。** 「無いかもしれない結果」を呼び出し側に強制する型が Optional。引数にすると呼び出し側が毎回箱を作り、箱そのものが `null` でも通る。

| 使い場 | このリポ |
|--------|----------|
| 戻り `tryResolve` → `Optional<VoterKey>` | **沿う**（`resolve` は直返し、`require` は void） |
| 引数 `VoterKeySource`（sealed） | **沿う**。HOST / Guest / Missing。`Optional<String> guestId` はやめる |
| 引数 `OidcUser oidc` | Event / Me は従来どおり。VoterKey 取得だけ Source に変換 |
| Entity の列 | Optional にしない（JPA） |

Controller で Session / principal を開けて `VoterKeySource` にする。戻りの `tryResolve` は Optional のまま。

### String ラッパーは Optional 引数を消すか？

**単独では消えない。消えるのは「無さ」の表し方を変えたとき。** ラッパー（`GuestId` が String を持つ）は項目62（文字列の取り違え）。Optional 引数は項目55（無いかもしれない値の渡し方）。別軸。

| 引数 | Optional 引数は消える？ | 取り違えは防げる？ |
|------|-------------------------|--------------------|
| `Optional<String>` | 残る（いまここ） | いいえ |
| `Optional<GuestId>` | 残る（箱が二重） | はい |
| `String guestId`（null 可） | **消える** | いいえ |
| `GuestId guestId`（null 可） | **消える** | はい |
| `VoterKeySource`（Missing を含む） | **消える**（null も不要） | はい（HOST と混ざらない） |

`GuestId` の中に空を入れて `isPresent()` すると、Optional を自作しただけになる。VO は「ある値」だけを表し、無いときは引数 null か `Missing`。

`issueOrReuse` は「無ければ発行」なので、引数は `VoterKeySource`（`Guest` 再利用 / `Missing` 新規 UUID）。`form`（Host 優先）は使わない。`fromGuestSession` が Guest / Missing だけを組み立てる。詳細は [f04-guest.md](f04-guest.md)。

## ユーティリティクラス

**インスタンスを持たない、static メソッドの置き場。** 「誰かの状態」ではなく「手順」だけがある。EJ 項目4。

| | ユーティリティ | Service / Entity |
|--|----------------|------------------|
| `new` | しない（private コンストラクタ） | する（またはファクトリ） |
| フィールド | 定数だけ | 依存や行の状態 |
| メソッド | `static` | インスタンス |
| DI | しない | `@Service` で注入 |

このリポの例: 予定の `GuestSession.readGuestId`。`VoterKeyResolver` は `UserService` が要るので **Service**（ユーティリティにしない）。

`Math.max` や `Objects.requireNonNull` が同じ仲間。やりすぎると「何でも static」になる。業務の流れ・DB は Service。

MVP は質問を status で閉じる。イベント削除だけ物理 DELETE。`deleted_at` は今足さない。

| 消す／隠す | やり方 |
|------------|--------|
| いいね取消 | `votes` 1行の物理 DELETE |
| **イベント削除** | `events` の物理 DELETE（配下 CASCADE） |
| 不適切質問 | `ARCHIVED`（F-08 / F-13）。質問行は残す |
| User | 消さない（`host_id` の FK 受け皿） |

## テスト・JaCoCo

| 層 | 手段 |
|----|------|
| Service | JUnit + Mockito |
| Controller | `@WebMvcTest` + security-test。本番 Security を無理に Import しない |

`src/test` は自動生成されない。`starter-test` で JUnit/Mockito/AssertJ/MockMvc は揃う。JaCoCo は後からで可。正常1 + 分岐0〜1。E2E/100% 不要。

```text
【EJ】不変性 … DTO は record
【EJ】静的ファクトリ … Entity の入り口
【EJ】公開面最小化 … Controller は薄く
【EJ】あえて外す … Entity は JPA で可変
```

## Javadoc の粒度

**役割 → クラス名 → `@author`。getter / record アクセサ / `JpaRepository` 由来 / コンストラクタは付けない。**

| 層 | 付ける | 付けない |
|----|--------|----------|
| Entity | ファクトリ、変更メソッド、compact | getter |
| enum | 定数1行、`canTransitionTo` | — |
| DTO | どの API の入出力か。`ErrorResponse` の `of` / `withField` | `body()` 等 |
| Repository | 自前の派生クエリ | `save` / `findById` |
| Service | 公開メソッド（404/409/401）、`toXxxResponse` | コンストラクタ・フィールド |

enum の遷移の正は [constraints](../design/constraints.md)。`EventStatus` は `DRAFT→OPEN`、`OPEN⇔CLOSED`。削除は物理 DELETE（enum に `DELETED` は持たない）。`QuestionStatus` は `OPEN→ANSWERED→ARCHIVED`（`OPEN→ARCHIVED` 可）。`PollStatus` は `DRAFT→OPEN→CLOSED`（終端、MVP-B）。
