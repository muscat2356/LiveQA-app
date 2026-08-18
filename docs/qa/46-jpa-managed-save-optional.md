# JPA 管理下なら save を省略できる理由

## 質問

`event.changeStatus(next)` のあと、`eventRepository.save(event)` はなぜ省略できるか。

## 結論

**同じトランザクション内で、Persistence Context に載っている Entity（managed）を書き換えた場合、コミット時に JPA が差分を UPDATE する。**  
そのため明示的な `save` は必須ではない。付けても害はほぼない（再 attach / 明示の意図）。

## 用語

| 語 | 意味 |
|----|------|
| Persistence Context | 「この TX で追跡している Entity の箱」（EntityManager） |
| managed（管理下） | 箱に載っている。フィールド変更が検知される |
| transient | `new` しただけ。まだ箱に無い → **save/persist が必要** |
| detached | かつて管理下だったが TX 終了などで箱から外れた |

## データの流れ（状態更新）

```text
@Transactional の updateStatus 開始
  → findById … DB から読み、Event が managed になる
  → event.changeStatus(OPEN) … メモリ上のフィールド変更
  → （メソッド終了）
  → コミット直前: dirty checking が「status / updatedAt が変わった」と検知
  → UPDATE events SET status=..., updated_at=... WHERE id=...
  → コミット
```

`save` を書かなくても、**変更の検知 → SQL** はコンテキスト任せで走る。

## いつ save が必要か

| 状況 | save |
|------|------|
| `Event.create(...)` の新規（まだ managed でない） | **必要**（今の `save` 作成） |
| `findById` などで取得済みを同じ TX 内で変更 | **省略可** |
| TX の外・別リクエストで受け取った detached | merge/save が必要になりやすい |

## `save` を明示する意味（任意）

- 読み手に「永続化する」意図が伝わる  
- 実装が `save`＝persist/merge と決まっているチーム規約  
- 害: 管理下なら多くの場合ノーオペに近く、余分な混乱は少ない  

## 【EJ】

```text
【EJ】あえて外す … Entity は JPA で可変（managed の書き換え）
【EJ】例外 … 新規は transient → 静的ファクトリ後に save で格納
```

## 一文

> managed な Entity の変更は TX コミット時に自動 flush される。新規だけ `save` で箱に入れる。
