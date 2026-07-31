# ドキュメント索引

live-qa-app（リアルタイム ライブQ&A + 投票 / sli.do風）の方向性・設計ドキュメント一覧。設計の正は本ディレクトリと [../README.md](../README.md)。AI Agent 向けは [../AGENTS.md](../AGENTS.md)。

## 決定済みの方向性

- [scope.md](scope.md) — MVP-A（F-01〜F-09）/ MVP-B（F-10〜F-13）/ ストレッチ（F-14〜F-15）、対象外、削る順番
- [roles.md](roles.md) — 3名の役割分担と機能 ID 対応（A=Next.js / B=Cookieセッション+Redis / C=WebSocket+Docker）
- [schedule.md](schedule.md) — 14日アジャイル（学習フェーズ → MVP-A → MVP-B → 仕上げ）
- [architecture.md](architecture.md) — アーキテクチャ図・技術要件のマッピング・STOMP設計・セキュリティ/整合性方針

## 設計（サービス具体化）

- [design/service-spec.md](design/service-spec.md) — サービス仕様（登場人物 / 想定フロー / 参加者識別 / 画面・エンドポイント一覧 / 状態遷移）
- [design/screen-transition.md](design/screen-transition.md) — 画面一覧・遷移図（主催者フロー / 参加者フロー）
- [design/websocket-sequence.md](design/websocket-sequence.md) — シーケンス図（E2E全体 / 質問投稿 / いいね / ハンドシェイク認証 / Poll）
- [design/er-diagram.md](design/er-diagram.md) — ER図・テーブル定義・制約/インデックス・Flyway初期DDL案
- [design/constraints.md](design/constraints.md) — 制約仕様（入力バリデーション・ビジネスルール・DB CHECK・HTTPステータス）
- [api/openapi-notes.md](api/openapi-notes.md) — REST API / STOMP仕様メモ（Req/Res・DTO・CSRF・配信ペイロード）

## 学習メモ（JIT）

- [qa/README.md](qa/README.md) — 質問と回答の蓄積（Agent が説明のたびに追記。ルール: `.cursor/rules/live-qa-learning-memo.mdc`）
- [qa/00-transplant-from-lab.md](qa/00-transplant-from-lab.md) — session-lab からの **移植判断**（F-01 素振りの持ち込み／除外）
- [qa/08-backend-project-migrate.md](qa/08-backend-project-migrate.md) — **backend プロジェクト移行手順**（Maven 骨組み）
- [backend/README.md](../backend/README.md) — 起動・疎通の入口

> Member B の GitHub Issue 案（B-01〜）は本番リポに置かず、ラボ  
> [`../../live-qa-session-lab/docs/issues/member-b/`](../../live-qa-session-lab/docs/issues/member-b/) に避難。**確認** 時に参照（`.cursor/rules/live-qa-keywords.mdc`）。

## AI レビュー（Gemini on GitHub）

- [../.gemini/README.md](../.gemini/README.md) — Gemini Code Assist（`styleguide.md` / `config.yaml`）
