# Gemini Code Assist（GitHub）設定

本ディレクトリは [Gemini Code Assist on GitHub](https://developers.google.com/gemini-code-assist/docs/review-github-code) 用です。

| ファイル | 役割 |
|----------|------|
| [styleguide.md](styleguide.md) | レビュー観点（live-qa-app 向け）。PR 差分に対する指摘基準 |
| [config.yaml](config.yaml) | 自動レビューの閾値・無視パス・PR オープン時の動作 |

## 使い方（概要）

1. GitHub リポジトリに Gemini Code Assist をインストール／連携する（Google Cloud / Developer Connect の手順に従う）
2. 本ディレクトリを **デフォルトブランチ（main 等）にマージ**する（エージェントがリポジトリ設定を読む）
3. PR を開くと、設定に応じてサマリ・コードレビューコメントが付く
4. PR 上で `/gemini` 付きコメントにより追加質問できる場合がある（公式ドキュメント参照）

## 本プロジェクトでの方針

- 設計の正は `docs/`。一般論より openapi / ER / constraints を優先
- MVP-A 死守・学習リポのため、過剰なエンタープライズ要求は styleguide で抑制
- PR テンプレート（`.github/pull_request_template.md`）の担当・F-xx・確認項目と矛盾しないか見る

Cursor 用ルール（`.cursor/rules/`）とは別系統。人間／Cursor 向けは従来どおり `AGENTS.md` と `.cursor/rules/`。
