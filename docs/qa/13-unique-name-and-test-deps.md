# UNIQUE 制約に名前を付ける理由 / テスト依存の指摘

## 質問

AI レビューで「unique 制約に名称を付けたほうがいい」「テスト関係の依存が入っていない」と指摘された。どういう意味か。

---

## 1. UNIQUE 制約に名称を付ける

### いまの `V1__init.sql` の状態

| 場所 | 書き方 | 名前 |
|------|--------|------|
| `users.oidc_subject` | 列定義の末尾 `UNIQUE` | **無名**（DB が自動命名） |
| `events.join_code` | 同上 | **無名**（自動命名） |
| `votes (question_id, voter_key)` | `CONSTRAINT uq_votes_question_voter UNIQUE (...)` | **あり**（良い例） |

`votes` だけ docs（er-diagram）どおり名前付き。単列の 2 つが「列レベル UNIQUE」で名前が無い、というのが指摘の対象になりやすい。

### なぜ名前が欲しいか

**UNIQUE 制約**は「この列（や列の組）に同じ値が二重に入らない」という DB 側のルール。  
違反すると PostgreSQL はエラーを返す。アプリはそれを捕まえて **409 Conflict** などに変換する想定（constraints / openapi）。

名前があると次が楽になる。

1. **エラーの識別**  
   例外メッセージや `SQLException` / Hibernate の制約違反に **制約名**が載る。  
   「join_code の重複」と「oidc_subject の重複」を、名前で分岐しやすい。  
   自動名だと `users_oidc_subject_key` のように DB 製品・版で決まり、読みにくく移植時に変わりうる。

2. **後からの変更**  
   `ALTER TABLE ... DROP CONSTRAINT uq_events_join_code` のように、**意図した制約だけ**を落とせる。  
   自動名を推測して DROP するのは脆い。

3. **ドキュメントとの対応**  
   er-diagram / constraints の「1人1票」「参加コード一意」と、DB オブジェクト名を揃えられる（`uq_votes_...` が既にその形）。

### 列末尾 `UNIQUE` と `CONSTRAINT 名前 UNIQUE` の違い

- **列末尾 `UNIQUE`**  
  書きやすいが、制約オブジェクトの名前は DB まかせ。  
  PostgreSQL ではだいたい `{table}_{column}_key`。

- **`CONSTRAINT uq_xxx UNIQUE (列…)`**  
  **発行（DDL で制約オブジェクトを明示作成）**するときに、人間が読める名前を付ける。  
  複合 UNIQUE（いいねの 1 人 1 票）はもともとこの形が自然。

推奨の形（イメージ）:

```sql
-- 列定義の中で名前付き
oidc_subject VARCHAR(255) NOT NULL,
CONSTRAINT uq_users_oidc_subject UNIQUE (oidc_subject)

-- またはテーブル後段
CONSTRAINT uq_events_join_code UNIQUE (join_code)
```

### Flyway 注意（本プロジェクト）

既存 `V*` は**書き換えない**方針。すでに V1 が適用済みなら、名前付けは例えば次のマイグレーションで行う。

```sql
-- 例: V2__name_unique_constraints.sql（イメージ）
ALTER TABLE users RENAME CONSTRAINT users_oidc_subject_key TO uq_users_oidc_subject;
ALTER TABLE events RENAME CONSTRAINT events_join_code_key TO uq_events_join_code;
```

（実際の自動名は `\d users` や `information_schema` で確認してから。）  
ローカルだけ DB を捨て直す運用ならチーム合意のうえで V1 を直すこともあるが、共有 DB / CI があるなら **追加マイグレーション**が正。

### 重要度の感覚

セキュリティ退行ではない。**運用・デバッグ・409 マッピングのしやすさ**の改善。  
学習リポでも「votes だけ名前付きで他は無名」の不揃いは直す価値あり。

---

## 2. 「テスト関係の依存が入っていない」

### 依存とは何か

Maven の **依存（dependency）**は「このプロジェクトが使うライブラリを宣言し、ビルド時にクラスパスへ載せる」仕組み。  
`pom.xml` の `<dependency>` がその宣言。

**テスト用依存**は `<scope>test</scope>` を付ける。意味は「本番 JAR には入れず、`mvn test` / IDE のテスト実行のときだけ使う」。

### いまの `pom.xml`（現状確認）

すでに次が入っている。

| artifact | 正体 | 役割 |
|----------|------|------|
| `spring-boot-starter-test` | テスト用のまとめ依存（JUnit 5 / AssertJ / Mockito / MockMvc 等） | 単体・スライステストの土台 |
| `spring-security-test` | Security 向けテスト支援 | `@WithMockUser` や CSRF 付き MockMvc など |

一方、`backend/src/test/` 配下のテストクラスは**まだ無い**（2026-07 時点）。

### 指摘が「あり」になりやすい理由

レビュアが見た差分によっては次のどれか。

1. **当時の pom に test 依存が無かった**（今は入っているなら「対応済み」）
2. **依存はあるがテストコードが無い** → CI で `mvn test` しても実質何も検証しない、という意味での指摘
3. **足りない種類の話**  
   - いまの starter-test だけで「Security 付き Controller」はかなり書ける  
   - Postgres + Flyway の統合テストまでやるなら、後から **Testcontainers** 等が候補（MVP-A 必須ではない）

### starter-test が無いと困る操作の意味

| やりたいこと | 無いと |
|--------------|--------|
| `mvn test` で JUnit を回す | テスト API 自体がクラスパスに無い |
| `@SpringBootTest` / `@WebMvcTest` | Spring のテスト注釈が使えない |
| MockMvc で `/api/me` を叩く | HTTP 層の自動テストが書きにくい |
| CSRF / ロールのテスト | `spring-security-test` が無いと手厚い検証がしづらい |

F-09（CI）で backend の `mvn test` / `mvn verify` を回すなら、**test scope の依存は前提**。今の pom はその前提は満たしている。

### 学習リポでの優先度

- **MEDIUM 寄りの妥当指摘**: テスト依存を入れ忘れると、後で CI を足したときに一気に詰まる  
- **今すぐ大量のテスト必須ではない**: MVP-A 死守の本体は API / Session / STOMP。ただし「依存ゼロ・ソースゼロ」より、starter-test を置いておくのは安い保険  
- 次の一歩の例: `@WebMvcTest` で認証まわり、または Repository + Testcontainers（余力）

---

## まとめ

| 指摘 | 意味 | いまどうする |
|------|------|----------------|
| UNIQUE に名称 | DB 制約を人間可読な名前で固定し、409 分岐・ALTER・docs 対応を楽にする | `votes` は済み。`oidc_subject` / `join_code` を名前付きへ（原則 V2 追加） |
| テスト依存 | 本番以外のクラスパスにテスト用ライブラリを載せる宣言 | pom に starter-test / security-test は既にある。無いと言われた差分が古いか、**テストコード未着手**を指している可能性大 |

---

## 3. UNIQUE 名を JPA アノテーションで付けたい場合

### 結論

- **名前付き UNIQUE を Entity に書くなら** `@Column(unique = true)` ではなく  
  **`@Table(uniqueConstraints = @UniqueConstraint(name = "...", columnNames = {...}))`** を使う
- 本プロジェクトは `ddl-auto: validate` なので、**JPA アノテーションは DB に制約を作らない／名前を変えない**
- **実 DB の名前の正は Flyway**。JPA はその名前と**一致する宣言**をする（ドキュメント兼 validate 用）

`Vote` はすでにこの形になっている（良い手本）。

### なぜ `@Column(unique = true)` では足りないか

| 書き方 | 正体 | 制約名 |
|--------|------|--------|
| `@Column(unique = true)` | 列メタデータ。「一意である」ことだけ伝える | **付けられない**（DDL 生成時は DB / Hibernate まかせ） |
| `@UniqueConstraint(name = "...", columnNames = ...)` | `@Table` 上の**制約メタデータ**（インタフェース的な宣言） | **`name` で明示**できる |

単列でも「名前を付けたい」なら、列の `unique = true` を外し、テーブル側の `uniqueConstraints` に寄せる。

### データの流れ（誰が何をするか）

1. **Flyway** … SQL で制約を**発行**（CREATE / RENAME）。PostgreSQL 上の実体が生まれる  
2. **JPA Entity** … 「このテーブルにはこの名前の UNIQUE がある」と**宣言**  
3. **`ddl-auto: validate`** … 起動時に Hibernate が Entity と実 DB を**照合**。食い違うと起動失敗  
4. **アプリ** … INSERT 失敗時、制約名が例外に載り 409 分岐しやすい（名前が揃っているとなお良い）

つまり「JPA だけで名前を付けて終わり」にはならない。**両方同じ名前**がセット。

### 写経イメージ（User / Event）

`Vote` と同じパターン:

```java
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_users_oidc_subject",
        columnNames = "oidc_subject"
    )
)
public class User {
    // unique = true は外す（名前は Table 側に一本化）
    @Column(name = "oidc_subject", nullable = false)
    private String oidcSubject;
    // ...
}
```

```java
@Entity
@Table(
    name = "events",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_events_join_code",
        columnNames = "join_code"
    )
)
public class Event {
    @Column(name = "join_code", nullable = false, length = 12) // unique = true を外す
    private String joinCode;
    // ...
}
```

### Flyway 側（必須のペア）

V1 適用済みなら、例えば追加マイグレーションで自動名をリネーム（実名は DB で確認）:

```sql
-- 例: V2__name_unique_constraints.sql
ALTER TABLE users RENAME CONSTRAINT users_oidc_subject_key TO uq_users_oidc_subject;
ALTER TABLE events RENAME CONSTRAINT events_join_code_key TO uq_events_join_code;
```

（`votes` の `uq_votes_question_voter` は V1 ですでに一致している想定。）

### よくある誤解

| 誤解 | 実際 |
|------|------|
| `@UniqueConstraint` を書けば DB の名前が変わる | `validate` では変わらない。Flyway が正 |
| `unique = true` のまま `name` も付けられる | 標準 JPA の `@Column` に制約名属性は無い |
| Entity だけ直せばレビュー完了 | DB 名とズレると validate や運用がつらい。SQL も揃える |

### 優先順位（本プロジェクト）

1. Flyway で実制約名を確定  
2. Entity の `@UniqueConstraint(name = ...)` を同じ文字列に揃える  
3. `unique = true` との二重宣言は避け、Table 側に一本化
