# ドキュメント索引

auction-app の方向性を定めたドキュメント一覧。設計の詳細（ER図・OpenAPI・画面遷移・シーケンス図など）は設計フェーズ（Phase3）で追記していく。

## 決定済みの方向性

- [scope.md](scope.md) — MVP-A / MVP-B のスコープ、対象外、削る順番
- [roles.md](roles.md) — 3名の役割分担（できるだけ均等に）
- [schedule.md](schedule.md) — 30日スケジュール（学習 → 共有 → 設計 → 実装）
- [architecture.md](architecture.md) — アーキテクチャ図・技術要件のマッピング・セキュリティ/整合性方針

## 今後追加予定（設計フェーズ）

- `design/er-diagram.md` — ER図（users / items / bids / categories 等）
- `design/screen-transition.md` — 画面一覧・遷移図
- `design/websocket-batch-sequence.md` — 入札WebSocket / 締切バッチのシーケンス図
- `api/openapi-notes.md` — OpenAPI・動的検索クエリパラメータのメモ
