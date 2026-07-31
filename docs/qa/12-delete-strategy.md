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

## 「論理で一部実装が必要」への答え

その理解でよい。実装するのはだいたい:

1. **状態遷移**（`PATCH` status）— Event / Question / Poll  
2. **一覧・フィルタで状態を除外**（例: 参加者向けは `ARCHIVED` を出さない、など openapi に従う）  
3. **いいね取り消しだけ**行 DELETE（投票の UNDO）

物理 `DELETE FROM events` や soft-delete カラム追加は **今は不要**。

## CASCADE がある理由

DDL に `ON DELETE CASCADE` があるのは、「もしイベント行を消したら関連も消える」という DB 側の安全網。  
アプリが MVP でイベント削除 API を持たないことと矛盾しない（将来・運用・テスト用）。
