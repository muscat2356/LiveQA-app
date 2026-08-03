# テスト周りの依存・プラグイン（JaCoCo 等）

## 質問

テストの際にあった方がよい依存関係は？ JaCoCo など。

## 結論（本プロジェクト・いま）

| 優先 | 何か | いま |
|------|------|------|
| **必須級（済）** | `spring-boot-starter-test` | pom にあり（JUnit5 / AssertJ / Mockito / MockMvc / `@WebMvcTest` 等） |
| **必須級（済）** | `spring-security-test` | pom にあり（`oidcLogin` / CSRF / `@WithMockUser`） |
| **あるとよい（任意）** | **JaCoCo**（プラグイン） | 未。カバレッジ計測用。依存ではなく **build plugin** |
| **後から（結合）** | Testcontainers（PostgreSQL / Redis） | architecture の CI 完成形。単体が先 |
| **不要寄り（MVP）** | 追加の AssertJ / Mockito 単体依存 | starter-test に含まれる |
| **不要寄り（学習）** | 厳密なカバレッジゲート（例: 80% 未満で CI 失敗） | 学習リポでは先に「意味あるテスト」を書く |

**いま足りないのは依存よりテストクラス。** JaCoCo は「書いてから測る」道具。

---

## すでに starter-test に入っているもの

| ライブラリ | 用途 |
|------------|------|
| JUnit 5 | テストの枠 |
| AssertJ | `assertThat(...)` |
| Mockito | `@Mock` / `when` |
| Spring Test / MockMvc | `@WebMvcTest` 等 |
| JSONAssert 等 | JSON 検証（必要時） |

追加で `mockito-core` を書く必要は通常ない。

---

## JaCoCo とは（依存ではない）

**JaCoCo** = テスト実行時に「どの行が通ったか」を測る **カバレッジツール**。  
`pom` の `<dependency>` ではなく、だいたい:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
            <goal>report</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

- `mvn test` 後に `backend/target/site/jacoco/index.html` などで見る  
- Boot parent がバージョン管理していることが多い（version 省略可な場合あり）  
- CI に「レポート生成」まで載せるのはよい。「○% 未満で fail」は方針を決めてから  

学習リポでは **レポートを見て穴を知る**用途が主。ゲートは必須にしない、が無難。

---

## 段階別おすすめ

### 今（F-01・単体 / Slice）

1. 既存の starter-test + security-test で書く  
2. CI は `mvn -B test`（済）  
3. 余力で JaCoCo report を追加  

### 次（Repository / Flyway 結合）

- `org.testcontainers:postgresql` + `testcontainers-junit-jupiter`  
- または `@DataJpaTest` + H2（本番 Postgres との差に注意。docs は Testcontainers 寄り）  

### やらなくてよい（今）

- PowerMock  
- カバレッジ 100% 強制  
- UI E2E を backend pom に載せる  

---

## CI との関係

```text
mvn test           … テスト実行（今の ci.yml）
mvn test jacoco:report  … カバレッジ HTML（任意で step 追加）
```

カバレッジは「テストの質の補助」。**意味あるケース（正常1＋異常0〜1）が先。**

---

---

## Q: `src/test` 以下は自動生成されないの？

**されない（普通は自分で作る／IDE の「Create Test」で作る）。**

| あるもの | 自動で作られる？ |
|----------|------------------|
| `pom` の `spring-boot-starter-test` | **ライブラリがクラスパスに載るだけ**。フォルダもテストクラスも作らない |
| `src/main/java` | プロジェクト作成時や手で置いたもの |
| `src/test/java/...` | **手作成**、または IDE の Generate Test / Maven archetype |
| GitHub Actions の `mvn test` | テストが無ければ「0 tests」で緑になりうる |

Spring Initializr で作ると空の `src/test/java` が付くことが多いが、**途中から依存だけ足した場合はフォルダが無い**こともある。  
`.gitkeep` を test 配下に置くのは「空ディレクトリを Git に残す」用途で、テスト本体ではない。

作る例:

```text
backend/src/test/java/com/liveqa/service/UserServiceTest.java
backend/src/test/java/com/liveqa/controller/MeControllerTest.java
```

パッケージは `main` 側（`com.liveqa...`）に揃えるとわかりやすい。
