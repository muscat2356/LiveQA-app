# GitHub Actions 解説

## 質問

GitHub Actions とは何か。本リポの `ci.yml` では何が起きているか。

---

## 一言で

**GitHub Actions** は、GitHub 上で「決まったタイミングに、決まった手順を自動実行する」仕組み（CI/CD の実行基盤）。  
人が手元で `mvn test` する代わりに、**PR のたびに GitHub が用意したマシンで同じテストを回す**、というのがいまの使い方。

| 用語 | 正体 |
|------|------|
| **CI（Continuous Integration）** | 変更をこまめに統合し、自動でビルド・テストして壊れていないか確かめる考え方 |
| **CD** | 配布・デプロイの自動化（本プロジェクトの MVP では必須ではない） |
| **Workflow** | 自動化のレシピ一式（YAML ファイル 1 本） |
| **Event（`on:`）** | いつ走るか（PR 作成、push など） |
| **Job** | 1 台のランナー上でまとめて走る作業単位 |
| **Step** | Job の中の 1 手順（checkout、Java セットアップ、`mvn test`…） |
| **Runner** | 実際にコマンドを実行する仮想マシン（例: `ubuntu-latest`） |
| **Action** | 再利用可能な部品（`actions/checkout@v4` など）。自前スクリプトの代わりに使う |

---

## なぜ使うか（本プロジェクト）

- 自分の PC だけで「テスト通った」だと、環境差で他人のマシン／本番相当で落ちることがある  
- PR に ✓/✗ が付くと、レビュー前に「最低限の自動チェック」が分かる（F-09）  
- Member C 主担当だが、backend の `mvn test` ジョブは B が先に載せてよい  

設計の正: [architecture.md](../architecture.md) の CI/CD 節。

---

## データの流れ（いまの `ci.yml`）

```text
① トリガ
   pull_request または push（対象ブランチ: main / develop）

② GitHub が Runner（ubuntu）を用意

③ Step: actions/checkout
   → 発行: そのコミットのソースを Runner 上にクローン

④ Step: actions/setup-java
   → Java 21（Temurin）を入れ、Maven 依存をキャッシュ

⑤ Step: mvn -B test（working-directory: backend）
   → 検証: JUnit テストを実行（-B はバッチ＝ログを CI 向きに）

⑥ 結果
   → 全部成功 → ジョブ緑（PR にチェック）
   → どれか失敗 → ジョブ赤（マージ判断の材料）
```

`concurrency` … 同じブランチで古い CI が走っているとき、新しい方を優先して古いのをキャンセル（待ち行列の無駄を減らす）。

---

## 本リポの YAML を行ごとに

パス: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)

| キー | 意味 |
|------|------|
| `name: CI` | Actions タブに出るワークフロー名 |
| `on.pull_request` | `main` / `develop` 向け PR で起動 |
| `on.push` | それらブランチへの push でも起動 |
| `jobs.backend-test` | ジョブ名（今は backend のみ） |
| `runs-on: ubuntu-latest` | Linux Runner |
| `defaults.run.working-directory: backend` | 以降の `run` は `backend/` がカレント |
| `uses: actions/checkout@v4` | 公式 Action でリポジトリを取得 |
| `uses: actions/setup-java@v4` | JDK 21 + Maven キャッシュ |
| `run: mvn -B test` | 実際のテストコマンド |

**まだ載せていないもの（docs の完成形）:** frontend の lint/build、Testcontainers 付き結合テスト。単体（Mockito）が通る段階からで十分。

---

## ローカル `mvn test` との関係

| | ローカル | GitHub Actions |
|--|----------|----------------|
| 誰が実行 | 自分 | GitHub の Runner |
| いつ | 任意 | PR / push |
| 同じ？ | 同じ `mvn test` を目指す | 依存キャッシュや OS は Runner 側 |

手元で落ちるテストは CI でも落ちる。逆に「手元だけ通る」は環境依存のサイン。

---

## よくある誤解

| 誤解 | 実際 |
|------|------|
| Actions = デプロイ必須 | CI（テスト）だけでも使える |
| YAML を置いただけで常に緑 | **テストが無い／空だと意味が薄い**。中身のテストが本体 |
| `ci.yml` がアプリを起動し続ける | ジョブ終了で Runner は捨てられる（毎回クリーン） |
| Member C 以外触れない | 主担当は C。backend test ジョブは B が先行してよい |

---

## 自分で確かめる手順

1. テストクラスと `ci.yml` をブランチに含めて push / PR  
2. リポジトリの **Actions** タブ、または PR の Checks を開く  
3. `backend (Maven test)` のログで `mvn test` の出力を読む  
4. 一度わざと失敗させて赤になることを確認すると理解が定着する  

---

## 関連

- [13-unique-name-and-test-deps.md](13-unique-name-and-test-deps.md)（テスト依存）
- F-09 / [architecture.md](../architecture.md)
