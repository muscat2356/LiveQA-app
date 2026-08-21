# F-06 動的クエリ（QueryDSL）

scope: `GET /api/events/{id}/questions` の `sort` / （F-12 で）`status`。正: [architecture](../architecture.md) / [openapi-notes](../api/openapi-notes.md) / [service-spec](../design/service-spec.md)。Issue: [#20](https://github.com/muscat2356/live-qa-app/issues/20)。

## 到達点

| 項目 | 状態 |
|------|------|
| 一覧 | **あり**（`findByEvent_IdOrderByCreatedAtDesc` 固定） |
| QueryDSL 依存 | **未**（`pom` に無し） |
| `sort=popular\|newest` | **未**（フロントはタブあり。API は無視） |
| `status` フィルタ | F-12 |

## なぜ動的か

条件の組み合わせを **メソッド名で全部用意すると爆発する**。

```text
findByEvent_IdOrderByCreatedAtDesc
findByEvent_IdOrderByUpvoteCountDescCreatedAtDesc
findByEvent_IdAndStatusOrderByCreatedAtDesc
findByEvent_IdAndStatusOrderByUpvoteCountDesc
…
```

`sort` と `status` はリクエストごとに有ったり無かったりする。**実行時に WHERE / ORDER BY を組み立てる**のが動的クエリ。

## このリポでの役割

```text
GET /api/events/{id}/questions?sort=popular&status=OPEN
  → Controller はクエリを受け取る（業務分岐しない）
  → Service が「載せる条件」を決める
  → Repository が Predicate + Order を組み立てて DB へ
  → Entity の List を復元
  → Service が QuestionResponse に変換（votedByMe は別クエリ）
```

Cookie / Redis / SecurityContext は一覧の認可には使わない（公開 GET）。`votedByMe` だけ Session の `voter_key` を読む。動的クエリ本体は **questions 行の絞りと並び**。

## 型の正体

| 名前 | 正体 |
|------|------|
| 派生クエリ（今） | メソッド名が SQL の契約。固定 1 本 |
| QueryDSL | コンパイル時に Entity から `QQuestion` を生成するライブラリ |
| `QQuestion` | 生成クラス。列への型つきパス（`question.upvoteCount`） |
| `BooleanBuilder` | 具象。AND で条件を足す箱。null なら足さない |
| `Predicate` | 契約。WHERE 1 かたまり |
| `OrderSpecifier` | ORDER BY 1 本（DESC/ASC + 列） |
| `JPAQueryFactory` | Hibernate 経由で SELECT を発行する入口 |

【EJ】ジェネリクス … `JPAQuery<Question>` で戻りを Entity に固定  
【EJ】公開面最小化 … 組み立ては Repository（または専用 query クラス）。Controller に `BooleanBuilder` を出さない  
【EJ】あえて外す … 今は派生クエリ 1 本。F-06 で QueryDSL に替える  

## 組み立てのイメージ（契約）

```text
必須: event.id = :eventId
任意: status が来たら AND status = :status
      来なければ ARCHIVED 以外（F-12）
並び: popular → upvote_count DESC, created_at DESC
      newest  → created_at DESC
```

`BooleanBuilder` は **WHERE**。並びは `orderBy(...)`。混ぜない。

文字列で SQL を足す（`"ORDER BY " + sort`）は型も SQL 注入も弱い。QueryDSL は列名を Java のフィールドで検証する。

## Q. JPQL の `@Query` では足りないか？

**固定なら足りる。** いいね ±1 は `@Query` でよい（条件が変わらない）。  
`sort` と `status` がリクエストごとに変わるので、F-06/F-12 は組み立てが要る。

## Q. MyBatis の `<if>` は？

architecture の代替案。このリポは JPA が正なので QueryDSL を採る。XML 動的 SQL は今は足さない。

## この単位（`sort` のみ）

`status` は F-12。今回は `?sort=popular|newest`（未指定は popular、不正は 400）。

`BooleanBuilder` にはまず `event.id` だけ載せる。F-12 で `status` を `and` する箱にしておく。

`QQuestion` は手書きしない。`mvn compile` が Entity から生成する。

組み立ては `QuestionQuery`（Repository 断片）。Controller に QueryDSL を出さない。

## Q. 静的クエリと何が違う？

**静的**は、メソッド名（または `@Query` の文字列）が **1 本の SQL の契約**。今の `findByEvent_IdOrderByCreatedAtDesc` がそれ。引数は `eventId` だけ。並びも条件も変わらない。

**動的**は、同じ入口から **実行時に部品を足す**。`sort=newest` なら ORDER BY が変わる。F-12 で `status=OPEN` が来たら WHERE に AND を足す。来なければ足さない（architecture の「null はスキップ」）。

いいね ±1 は条件が変わらないので、動的にしなくてよい。今の `@Query` のまま。

## Q. votedByMe は動的クエリに入れる？

**入れない。** 動的クエリの仕事は `questions` の **どの行を・どの順で** 取るか。

`votedByMe` は「今の Session の `voter_key` が、その質問に投票済みか」。Cookie → Redis の Session → `g:` / `u:` を **復元**し、`votes` を別クエリで読む。一覧の WHERE に混ぜると、公開 GET なのにキー無しと有りで行集合が変わってしまう。

## Q. BooleanBuilder に sort を載せない理由

`BooleanBuilder` は **WHERE の箱**（真偽の AND）。並びは真偽ではないので `orderBy(...)`。

popular を WHERE に書くと「いいねが多い行だけ残す」になってしまい、**並べ替え**にならない。

## Q. Qクラスはいつ・誰が作る？

`mvn compile`（QueryDSL の注釈処理）が `Question` Entity から `QQuestion` を **生成**する。手書きしない。列名の打ち間違いはコンパイルで落ちる（文字列 SQL だと実行時まで分からない）。

## Q. Criteria API / Specification / `"ORDER BY " + sort` は？

| 手段 | このリポ |
|------|----------|
| 文字列連結 | 使わない。型も SQL 注入も弱い |
| JPA Criteria | 標準だが記述が長い |
| Spring Data `Specification` | Predicate 合成は近い。ORDER BY まで含めると QueryDSL の方が読みやすい |
| QueryDSL | architecture の正。`QQuestion` で列が型つき |

MyBatis の `<if>` は代替案。JPA が正なので今は足さない。
