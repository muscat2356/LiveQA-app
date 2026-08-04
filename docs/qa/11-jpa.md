# JPA とは何か（live-qa-app 向け）

> 依頼キーワード **解説**（2026-07-31）。設計の正は `docs/design/er-diagram.md`。実装手順は [10-get-api-events.md](10-get-api-events.md)。

## 一言で

**JPA（Jakarta Persistence API）は「Java のオブジェクト ↔ リレーショナル DB の行」を対応づけるための仕様（インタフェースの集まり）**です。  
このプロジェクトでは実装として **Hibernate**（Spring Boot の `spring-boot-starter-data-jpa` 経由）を使います。

| 名前 | 正体 |
|------|------|
| JPA | **仕様**（契約）。`@Entity` や `EntityManager` などの約束ごと |
| Hibernate | **実装**。実際に SQL を発行して読み書きするエンジン |
| Spring Data JPA | Repository インタフェースから実装を**自動生成**する Spring の便利層 |

「JPA を使う」≒ Entity を書いて、Repository 経由で DB に**格納**／**復元**する、というイメージでよいです。

## なぜ live-qa-app で必要か

`GET /api/events` は「自分のイベント一覧」を返す。一覧の正データは PostgreSQL の `events` テーブル。  
Session（Redis）にはログイン状態しか無く、イベント行は持てない。だから:

```text
Controller → Service → Repository（Spring Data JPA）→ Hibernate → PostgreSQL
```

## データの流れ（操作の意味つき）

```text
1. 起動
   Flyway が V1__init.sql を適用 → テーブルを作る（スキーマの正）
   Hibernate が Entity と表構造を照合（ddl-auto: validate = 検証）

2. リクエスト（例: 一覧）
   Service が repository.findByHostId...(hostId) を呼ぶ
   → Hibernate が SELECT を発行
   → 行を Event オブジェクトへ復元（マッピング）
   → Service が EventSummaryResponse（record）へ変換
   → JSON で返す

3. 保存（例: ユーザー JIT）
   users.save(User.hostOf(sub))
   → 永続化コンテキストに格納（まだコミット前のこともある）
   → トランザクション終了で INSERT が確定
```

| 操作語 | この場所での意味 |
|--------|------------------|
| マッピング | Java フィールドと DB カラムを対応づける |
| 復元 | SELECT 結果の行から Entity インスタンスを組み立てる |
| 格納 / 永続化 | Entity を「DB に書く対象」として管理する（最終的に INSERT/UPDATE） |
| 変換 | Entity → API 用 DTO。Entity をそのまま外に出さない |

## `@Entity` まわり（写経で出る注釈）

いまの `User.java` は空クラス。ここから JPA 用のメタデータを足していく。

| アノテーション | 正体 / 効く範囲 |
|----------------|-----------------|
| `@Entity` | 「このクラスはテーブル1行に対応する」と JPA に教える |
| `@Table(name = "users")` | 表名。省略時はクラス名が使われがちなので明示が安全 |
| `@Id` | 主キー |
| `@GeneratedValue(IDENTITY)` | id を DB の `BIGSERIAL` に任せる |
| `@Column` | カラム名や NOT NULL / unique など |
| `@ManyToOne` | 多対一関連（多数の Event → 一人の User） |
| `@Enumerated(STRING)` | enum を DB に文字列で格納（`DRAFT` など） |
| `@Transactional` | そのメソッドの DB 操作を1トランザクションにまとめる |

**protected 無引数コンストラクタ**が必要な理由: Hibernate が行を**復元**するとき、まず空のインスタンスを作り、あとからフィールドを埋めるため。

## Flyway と JPA の役割分担（本プロジェクト）

| 担当 | 何をするか |
|------|------------|
| Flyway | テーブル作成・変更の正。`V1__init.sql` など |
| JPA / Hibernate | 実行時の読み書き。`ddl-auto: validate` で「Entity と表が一致するか**検証**」のみ |
| やってはいけない | `ddl-auto: update` で本番相当のスキーマを勝手に変える（規約: Flyway 追加のみ） |

### 注意: マイグレーションの置き場

Flyway の既定パスは:

```text
src/main/resources/db/migration/V1__init.sql
```

`db/migration/migration/V1__init.sql` のように **migration が二重**だと、既定では見つからずテーブルが作られない。  
ファイルを書いたらパスを必ず確認する。

## Entity と DTO（Effective Java との接点）

| 種類 | 役割 | EJ の意識 |
|------|------|-----------|
| Entity（`User` / `Event`） | DB と向き合う可変オブジェクト | equals を安易に作らない |
| DTO（`EventSummaryResponse` record） | API 応答の形。不変 | record で不変性 |

Entity を Controller から直接返すと、遅延ロードや内部構造が漏れやすい。**変換 →** Service で DTO にする。

## Repository とは

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOidcSubject(String oidcSubject);
}
```

- `JpaRepository` は **インタフェース**（契約）。実装クラスは自分で書かない
- Spring Data が起動時に実装を用意する
- メソッド名 `findByOidcSubject` から「WHERE oidc_subject = ?」相当のクエリを組み立てる

## いまの自分の位置

1. pom / application.yml（JPA・Flyway 有効）… 済み  
2. enums … 済み  
3. `V1__init.sql` … 作成済み（**パスを `db/migration/` 直下に直す**）  
4. `User` Entity を空から `@Entity` 付きへ写経 … **次**  
5. `Event` / Repository / `GET /api/events`

## Q: `User.hostOf` は Builder？ SecurityContext から取る？

**半分合っていて、半分違う。**

### 1. `hostOf` ≠ Builder（合っている）

| | Builder | `User.hostOf(sub)` |
|--|---------|---------------------|
| 正体 | 多数の任意パラメータを段階的に組み立てるパターン | **静的ファクトリメソッド**（EJ Item 1） |
| 使い方 | `User.builder().oidc(...).role(...).build()` | `User.hostOf(sub)` 一発 |
| 目的 | 複雑な生成 | 「OIDC の sub から HOST 行を作る」という**意図に名前を付ける** |

`hostOf` は Security から情報を取るメソッドではない。引数に渡された `oidcSubject` 文字列で、**ローカル DB 用の `User` Entity を new するだけ**。

### 2. SecurityContext が持つもの（ここがポイント）

SecurityContext（セッション経由で**復元**される）が持つのは主に:

- `OidcUser`（Keycloak ログインの principal）… `sub` / 名前 / email など
- `Authentication` の authorities … `ROLE_HOST` など

**持たないもの:** JPA の `User` Entity（`users.id` の行）。DB のユーザーとは別物。

```text
SecurityContext
  └─ OidcUser.getSubject()  →  "abc-uuid-from-keycloak"   ← 認証の世界

PostgreSQL users
  └─ id=10, oidc_subject="abc-uuid-..."                  ← 永続化の世界
```

橋渡しは例えば `CurrentUserService.requireHost(oidcUser)`:

1. Context から来た `OidcUser` の `sub` を取る（認証情報の**復元**結果を読む）
2. `users.findByOidcSubject(sub)` で DB 行を探す
3. 無ければ `User.hostOf(sub)` で Entity を作り、`save` で**格納**（簡易 JIT）

### 3. では Builder にしてよいか？

**動くし、意図も書けば伝わる。ただしこの `User` では Builder は厚すぎる。**

ER 上の `users` はだいたい次だけ:

- `oidc_subject`（必須・生成の核）
- `role`（JIT ではほぼ常に `HOST`）
- `created_at`（now でよい）

| 方針 | 向くとき | この User だと |
|------|----------|----------------|
| 静的ファクトリ `hostOf(sub)` | 必須引数が少なく、生成パターンに名前を付けたい | **ちょうどよい**。「OIDC sub から HOST 行」が一語 |
| Builder | 任意項目が多い・組み合わせが何通りもある | `builder().oidcSubject(sub).role("HOST").build()` は冗長 |

Builder 例:

```java
User.builder().oidcSubject(sub).role("HOST").build();
```

同じことを `hostOf` はこう表す:

```java
User.hostOf(sub);  // role=HOST・createdAt=now は中で決める
```

**EJ の目安:** 必須パラメータが少ない生成は静的ファクトリ。Builder は「望遠鏡コンストラクタ」や多数の optional 向け。

チームで「Entity 生成は全部 Builder」と決めるなら統一しても問題はない。その場合も **SecurityContext を読むのは Builder ではなく Service** という分担は変えない。

## Q: `hostOf` で role=HOST 固定は拡張性が低い？

**MVP の今は妥当。将来はファクトリを増やす／引数化するので十分追いつく。**

### 設計上の前提（docs）

- `users` は **会員＝主に主催者**。参加者は login せず `users` に入らない（匿名セッション）
- er-diagram: `role` は当面 `HOST`、コメントで「将来 `PARTICIPANT` 拡張用に保持」
- **認可の正**はリクエスト時の Security（Keycloak → `ROLE_HOST`）。DB の `role` はローカル行のラベル寄り

だから「Keycloak でログインして JIT された行」＝今はほぼ HOST。`hostOf` 固定はスコープに合っている。

### 拡張するときの選択肢（壊さずに足せる）

| やり方 | 例 | 向き |
|--------|-----|------|
| ファクトリを増やす | `hostOf(sub)` / 将来 `participantOf(sub)` | 生成パターンが少数で名前が重要 |
| 引数化 | `of(sub, Role.HOST)` | ロール種が増えたとき |
| OIDC から写す | `fromOidc(OidcUser)` で realm roles → ローカル role | Keycloak がロールの正のとき |
| DB に持たない | 認可は常に `Authentication`、users は id+sub のみ | 単一 role カラムが辛い多ロール向け |

`hostOf` → 後から `of(sub, role)` にリネーム／委譲するのは破壊が小さい。最初から Builder で `role` を毎回渡す必要はない。

### 注意: 「複数ロール」の意味

- **ユーザー種別が増える**（HOST / 将来ログイン参加者）→ 上記のファクトリ拡張で足りることが多い
- **一人が同時に複数ロール**（HOST+ADMIN）→ `varchar role` 1本では足りない。別テーブルや Keycloak 側のみ、などモデル変更が要る

後者まで見据えるなら、今の固定 HOST より **「ローカル role をどこまで正にするか」** を先に決めた方が効く。MVP-A では Security の `hasRole('HOST')` を正にし、`users.role` は HOST 既定のままでよい。

## Q: なぜコンストラクタをカプセル化（protected / private）する？

**「勝手な `new User(...)` を防ぎ、生成経路を `hostOf` などに一本化するため」＋「JPA が復元用に無引数コンストラクタを必要とするため」。**

### 二つのコンストラクタの役割

| コンストラクタ | 可視性 | 誰が使うか | 目的 |
|----------------|--------|------------|------|
| 無引数 | `protected`（または package） | Hibernate | 行から Entity を**復元**するとき、空インスタンスを作る |
| （実質）生成 | 外から呼べない | なし。代わりに `hostOf` | 正しい初期値で作る入口を制限する |

```java
protected User() {}  // JPA 用。アプリコードからは new User() したくない

public static User hostOf(String oidcSubject) {
    User u = new User();  // 同じクラス内なので呼べる
    u.oidcSubject = oidcSubject;
    // ...
    return u;
}
```

### カプセル化する意図（EJ / 設計）

1. **不変条件を守る**  
   外から `new User()` されると `oidcSubject` 未設定の不正行ができうる。生成を `hostOf` に閉じると「必ず sub 付き HOST」に寄せられる。

2. **生成の意図を名前で示す**  
   `new User()` より `User.hostOf(sub)` の方が「何のための User か」が伝わる（静的ファクトリ）。

3. **公開 API の最小化**  
   Entity の作り方を Service 層以外に広げない。Controller が直接 `new` しない。

### JPA だけの都合ではない

Hibernate は「無引数コンストラクタが必要」なので **完全に private で引数付きだけ**、にはしにくい（実装によっては private 無引数も動くが、`protected` が無難）。  
アプリ向けには「無引数を public にしない」＝カプセル化、がポイント。

### やらなくても動く？

## Q: 「HOST 行を作る」とは？

**PostgreSQL の `users` テーブルに、主催者（role=`HOST`）として1行 INSERT すること。**

```text
users
┌────┬─────────────────────────┬──────┬────────────┐
│ id │ oidc_subject            │ role │ created_at │
├────┼─────────────────────────┼──────┼────────────┤
│ 10 │ "keycloak-sub-uuid..."  │ HOST │ 2026-...   │  ← この1行が「HOST 行」
└────┴─────────────────────────┴──────┴────────────┘
```

- **HOST** … アプリ上のロール名。イベントを作れる主催者（Keycloak ログイン勢）
- **行** … DB のレコード1件（ここでは `users` の1行）
- **作る** … まだ無ければ JIT で INSERT（`User.hostOf(sub)` → `repository.save`）

Keycloak にユーザーがいるだけでは、ローカル DB にはまだ行が無い。  
初回ログイン（または初回 `requireHost`）で「この OIDC の人用の HOST 行」をローカルに作る、という意味。

参加者（匿名）は `users` に行を作らない。だから普段言う「HOST 行」＝主催者用の `users` レコード。

## Q: Event の写経例に生成手段が無い？

**その指摘どおり。GET 一覧用に「復元＋getter」だけ示していて、作成（`POST /api/events`）の入口が欠けていた。**

読み取りだけなら JPA が無引数で**復元**し getter で読めば足りる。  
作成時はアプリが Entity を組み立てる必要があるので、`User.hostOf` と同様に **静的ファクトリ `Event.create`** を足す（Builder 必須ではない）。

関連: [10-get-api-events.md](10-get-api-events.md)

