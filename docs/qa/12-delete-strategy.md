# 削除方針（物理 DELETE が少ない理由）

> 解説（2026-07-31）。正は [er-diagram.md](../design/er-diagram.md) / [constraints.md](../design/constraints.md) / [openapi-notes.md](../api/openapi-notes.md)。

## 一言

**MVP-A/B は「行を消す」より「状態で隠す・閉じる」が正。**  
User 行は Keycloak がプロフィールの正でも **ローカルに必要**（`host_id` の FK）。Event 等の「論理削除っぽい」操作は **status 更新**で実装する。

## User は不要？ → 不要ではない

| 置き場 | 正になるもの |
|--------|----------------|
| Keycloak | 認証・表示名・email・realm roles |
| ローカル `users` | `id`（PK）、`oidc_subject`、イベントの `host_id` FK |

JIT で作るのは「OIDC の人を DB の所有者キーにするため」。プロフィール複製が目的ではない。  
**ユーザー削除 API が無い**のは、退会フローがスコープ外だから。Keycloak で消してもローカル行は残る（孤児になりうる）が、MVP では許容。

## ドメインデータの削除方針（docs どおり）

er-diagram:

> **MVP-A/B: 削除しない**。イベント削除時は CASCADE（将来・手動用の FK）。明細のバッチ削除は将来。

| 対象 | API でやること | DB |
|------|----------------|-----|
| Event | `CLOSED`（再開可） | 行は残す。`DELETE /api/events` は無い |
| Question | `ARCHIVED` / `ANSWERED` | 行は残す。物理 DELETE 無し |
| Poll | `CLOSED` | 行は残す |
| Vote | `DELETE .../vote`（取り消し） | **投票行だけ**物理削除（いいね解除） |
| User | 削除 API 無し | 行は残す |

いわゆる「論理削除フラグ `deleted_at`」は採用していない。**既存の status 列がそれ相当**。

## User に論理削除（`deleted_at` 等）を入れるべきか

**結論（MVP-A/B）: 入れない方がよい。** 設計の正は「User 削除 API 無し・行は残す」。Event/Question は **status** が論理相当で、別途 soft-delete 列は採用していない。

### 入れたくなる理由（気持ちは分かる）

- Keycloak でアカウントを消したあと、ローカル `users` が孤児になる  
- 「退会した HOST」を一覧や JIT から除外したい  
- 監査・コンプライアンスで「消したことにしたいが FK は残したい」

### いま入れない理由

| 観点 | 説明 |
|------|------|
| **スコープ** | 退会・アカウント無効化 API が MVP 対象外（docs/qa/12） |
| **設計の一貫性** | 論理相当は既存の **status**（Event/Question/Poll）。`users` にだけ `deleted_at` を足すと方針が割れる |
| **JIT との衝突** | `oidc_subject` が UNIQUE。論理削除行を残したまま同じ人が再ログインすると UNIQUE 違反、または「削除済みを復活」分岐が必要になり F-01 が厚くなる |
| **FK** | `events.host_id` は NOT NULL。User を「消した扱い」にしてもイベント所有者は残る。物理 CASCADE はさらに危険（イベントごと消える） |
| **学習リポ** | F-01 仕上げ（JIT / me / logout）の本丸から外れる |

### 将来入れるなら（ストレッチ以降のイメージ）

1. docs（er-diagram / openapi）を先に更新  
2. Flyway で `disabled_at` や `status`（ACTIVE/DISABLED）を **追加**  
3. JIT: 無効ユーザーは 403、または復活ポリシーを明示  
4. UNIQUE: 無効後の再登録を許すなら部分 UNIQUE（PostgreSQL partial index）等を設計  

「なんとなく `deleted_at`」より、**無効化の業務ルール**を決めてからが安全。

## 将来: サードパーティーユーザー管理アプリを見据える場合

想定: LibraShare / live-qa-app など複数クライアントを束ねる **ユーザー管理（退会・無効化・監査）** を別アプリ／共通基盤で持つ。  
母体ハブ構想の全体図は [design/platform-hub-vision.md](../design/platform-hub-vision.md) を正とする。

### 削除カラムを「今 live-qa の users に足す」は将来設計の本命か？

**必ずしも本命ではない。** live-qa の `users` は今のところ:

- Keycloak `sub` → ローカル PK の **橋**
- `events.host_id` の受け皿  
であり、プロフィールや退会の正ではない（正は IdP / 将来のユーザー管理）。

将来の責務分離のイメージ:

```text
Keycloak（認証の正）
  └── 複数 client: LibraShare / live-qa-app / …

ユーザー管理アプリ（将来・任意）
  └── 無効化・退会・監査・アプリ横断の利用停止
        ↓ 通知 or 共有DB or Admin API
各アプリのローカル users（JIT 橋）
  └── 必要なら disabled_at / status を「反映用」に持つ
```

つまり **削除／無効の業務ルールの正**はユーザー管理側（または Keycloak の disable）に置き、live-qa は「反映用フラグ」を後から Flyway で足す、が拡張しやすい。

### 今カラムを先置きするメリット / デメリット

| | 先に `deleted_at`（や `disabled_at`）を V3 で足す | MVP では足さず、必要時に Flyway 追加 |
|--|--|--|
| メリット | テーブル形が将来に近い、忘れにくい | F-01 が薄い、UNIQUE/JIT を単純に保てる |
| デメリット | 使わない列・クエリ分岐が先行、docs/openapi とズレる、再ログイン時の UNIQUE 方針を今決め打ちする | 後で ALTER が1本増える（この規模では安い） |

**推奨:** ポートフォリオ期間（F-01〜MVP-A）は **列を足さない**。将来設計は docs に「無効化は共通ユーザー管理／IdP が正、各アプリは反映用フラグを後付け」とメモする。実装コストは Flyway 1本で吸収できる。

### 入れるなら名前と意味

「削除」より **`disabled_at`（無効化時刻）** や **`status: ACTIVE|DISABLED`** の方が、サードパーティー横断の「利用停止」に近い。  
物理削除や CASCADE でイベントを消す用途ではない。

再ログインを許すなら:

- 無効行を残したまま同じ `oidc_subject` で UNIQUE 衝突しないよう、**部分 UNIQUE**（`WHERE disabled_at IS NULL`）をセットで設計する

### live-qa F-01 での扱い

`provisionHost` は「無ければ作る・あれば使う」のまま。無効フラグは見ない。  
ユーザー管理アプリ着手時に、反映ルールと一緒に列を追加する。

その理解でよい。実装するのはだいたい:

1. **状態遷移**（`PATCH` status）— Event / Question / Poll  
2. **一覧・フィルタで状態を除外**（例: 参加者向けは `ARCHIVED` を出さない、など openapi に従う）  
3. **いいね取り消しだけ**行 DELETE（投票の UNDO）

物理 `DELETE FROM events` や soft-delete カラム追加は **今は不要**。

## CASCADE がある理由

DDL に `ON DELETE CASCADE` があるのは、「もしイベント行を消したら関連も消える」という DB 側の安全網。  
アプリが MVP でイベント削除 API を持たないことと矛盾しない（将来・運用・テスト用）。
