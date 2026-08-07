# JPA `save` 失敗時のエラー処理

## 質問

JPA の `save` が失敗したときのエラー処理の正解は？

## 結論（このプロジェクト）

**「全部 catch して同じ JSON」にしない。** 失敗の種類ごとに分ける。

| 失敗の種類 | 典型例外 | HTTP | 扱い |
|------------|----------|------|------|
| 想定する競合（UNIQUE 等） | `DataIntegrityViolationException`（中に制約名） | **409** | Service で catch → `BusinessException`、または Handler で制約名マップ |
| 業務上ありえない／原因不明 | その他の `DataAccessException` / 実行時例外 | **500** | catch-all（いまの `ExceptionHandler`） |
| 先に防げるもの | （例外にしない） | — | `existsByJoinCode` で再発行、状態チェックで 409 など |

`save` 自体は成功時に Entity を返すだけ。失敗は **例外で上がる**（戻り値 null ではない）。

## データの流れ

```text
repository.save(entity)
  → OK: 永続化して返す
  → NG (UNIQUE 衝突など):
       SQLException
         → Hibernate / Spring が DataIntegrityViolationException に変換
           → （推奨）Service or Advice で制約名を見て BusinessException(409, "Conflict", ...)
           → 未処理なら catch-all で 500（よくない）
```

## 正解に近いパターン

### 1. 予防できるなら例外に頼らない（joinCode）

```text
existsByJoinCode → 衝突なら再発行 → save
```

レースで極稀に UNIQUE 違反が残る → そのときだけ 409 or 再試行。

### 2. UNIQUE が業務の正（いいね 1 人 1 票）

docs: 制約違反 → **409**。

```text
try {
  voteRepository.save(vote);
} catch (DataIntegrityViolationException ex) {
  if (制約名が uq_votes_question_voter) {
    throw new BusinessException(CONFLICT, "Conflict", "既に投票済みです");
  }
  throw ex; // 想定外は 500 へ
}
```

制約名を揃えてあるのはこのため（[13](13-unique-name-and-test-deps.md)）。

### 3. Handler 一括でも可

`@ExceptionHandler(DataIntegrityViolationException.class)` で制約名 → 409 メッセージをマップ。  
Service が薄くなる。どちらでも「409 にする競合」と「500 の想定外」を分けることが正。

## やらない方がよい

| やり方 | 理由 |
|--------|------|
| `save` の戻りで成否判定 | 失敗は例外。成功時しか返らない |
| あらゆる DB 例外を 400 | クライアント入力ではないことが多い。競合は 409、不明は 500 |
| メッセージ全文の文字列マッチだけ | 制約名の方が安定（名前付き UNIQUE） |
| 握りつぶして null 返却 | API 契約と三層が崩れる |

## 型の正体

| 型 | 意味 |
|----|------|
| `DataIntegrityViolationException` | Spring の永続化例外。FK/UNIQUE/NOT NULL など |
| `BusinessException` | アプリが決めた HTTP 付き業務失敗 |
| `save` | 成功時の格納。失敗は例外で伝播 |

## 一文

> `save` 失敗は例外。UNIQUE など想定競合は **409（`BusinessException`）**、想定外は **500**。できる競合は先にチェック／再試行し、DB は最後の砦。
