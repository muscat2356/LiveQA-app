# バックエンド — AI Agent 入口

Spring Boot API。**規定の正はルートのルール6本 + `docs/`**。

**手動実装厳守:** 本番／テスト Java は本人が手で書く。Agent は写経提示のみ（解除は「Agentがファイルを編集してよい」明示時）。

| 読む順 | 何か |
|--------|------|
| [../docs/codex.md](../docs/codex.md) | Codex 利用時（Cursor と同じ 6 本。`.mdc` は自動注入されない） |
| [../AGENTS.md](../AGENTS.md) | 索引 |
| [../.cursor/rules/live-qa-keywords.mdc](../.cursor/rules/live-qa-keywords.mdc) | 実装 / 解説 / 確認 / 修復 / PR案作成（**手書き厳守**） |
| [../.cursor/rules/live-qa-backend.mdc](../.cursor/rules/live-qa-backend.mdc) | **Java 形の正**（この領域） |
| [../.cursor/rules/live-qa-learning-explain.mdc](../.cursor/rules/live-qa-learning-explain.mdc) | 写経・【EJ】 |
| openapi / er-diagram / constraints | 契約 |

移植メモ: [../docs/qa/08-backend-project-migrate.md](../docs/qa/08-backend-project-migrate.md)
