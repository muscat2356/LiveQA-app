# ChatGPT Codex — Cursor と同じルール

Cursor の規範（`.cursor/rules/` **6本** + `docs/`）を、ChatGPT Codex でも同じように守るための読み口。

**規範の正は `.cursor/rules/` の 6 本。** 本ファイルと矛盾したら `.mdc` を採る。ルールファイルは **増やさない**。

関連: [../AGENTS.md](../AGENTS.md)（索引） / [Gemini レビュー](../.gemini/README.md)（GitHub 上の別系統）

---

## なぜこのファイルがあるか

| ツール | セッション開始時に自動で入るもの |
|--------|----------------------------------|
| **Cursor** | `AGENTS.md` **と** `.cursor/rules/*.mdc`（`alwaysApply` / `globs`） |
| **Codex** | **`AGENTS.md` の連鎖だけ**（`.mdc` は自動では入らない） |

Codex に「Cursor と同じ」とだけ言っても、キーワード・手書き厳守・【EJ】は届かない。**開始時に 6 本を Read する**か、本ファイルの「6本の本文」に従う。

---

## Codex が実際に読むもの

公式: [Custom instructions with AGENTS.md](https://developers.openai.com/codex/guides/agents-md)

1. グローバル（任意）: `~/.codex/AGENTS.md`（チーム規範はリポに置く。個人好みだけグローバル）
2. リポルート: [`AGENTS.md`](../AGENTS.md)
3. 今の作業ディレクトリまで降りながら、各階層で **1 ファイルだけ**（`AGENTS.override.md` があればそちら。無ければ `AGENTS.md`）

このリポでは:

| 作業ディレクトリ | つながるファイル |
|------------------|------------------|
| リポルート | ルート `AGENTS.md` |
| `backend/` | ルート + [`backend/AGENTS.md`](../backend/AGENTS.md) |
| `frontend/` | ルート + [`frontend/AGENTS.md`](../frontend/AGENTS.md) |

結合サイズの上限は既定 **32 KiB**。超えると後ろが落ちる。ルート `AGENTS.md` を肥やして 6 本を二重管理しない。詳細は本ファイルと `.mdc`。

`AGENTS.override.md` をリポに置かない（チームの `AGENTS.md` が隠れる）。

---

## 作業開始チェック（Codex）

毎回、コードを触る前に次を **Read** する。

1. 共通（always 相当）
   - [`.cursor/rules/live-qa-project.mdc`](../.cursor/rules/live-qa-project.mdc)
   - [`.cursor/rules/live-qa-keywords.mdc`](../.cursor/rules/live-qa-keywords.mdc)
   - [`.cursor/rules/live-qa-learning-explain.mdc`](../.cursor/rules/live-qa-learning-explain.mdc)
   - [`.cursor/rules/live-qa-learning-memo.mdc`](../.cursor/rules/live-qa-learning-memo.mdc)
2. 領域（Cursor の glob 相当）
   - Java / API / Flyway → [`.cursor/rules/live-qa-backend.mdc`](../.cursor/rules/live-qa-backend.mdc)
   - `frontend/` の TS/TSX → [`.cursor/rules/live-qa-frontend.mdc`](../.cursor/rules/live-qa-frontend.mdc)
3. 契約は `docs/`（openapi / ER / constraints / scope）。学習メモは [`qa/README.md`](qa/README.md)

ChatGPT のカスタム指示に貼る場合は、下の「6本の本文」を使う。正の更新は **`.mdc` 側**。

---

## 6本の本文（Codex 用・正は .mdc）

YAML フロントマターは Cursor 専用。ここは本文だけ。

### 1. 共通 OS（`live-qa-project`）

日本語。コメントも日本語。明るくポジティブ。

**正の優先（上を採る）**

1. `docs/`（openapi / ER / constraints / websocket-sequence / scope）
2. 本ルール群 / `README.md`
3. **Java**: 可読性・EJ（明示型・`final`・不変DTO・分かる名前）≫ `var`・行数削減
4. その他一般論

矛盾時は「docs 更新か実装合わせか」を明示。母体ハブ構想は MVP の正ではない。

**スコープ（要約）**  
MVP-A 死守 F-01〜F-09（[`scope.md`](scope.md)）。ルーム `DRAFT→OPEN⇔CLOSED`。エラー `{ status, error, message }`。権限の正は API。

| Member | 領域 |
|--------|------|
| A | Next.js / STOMP 受信 |
| B | Session+API / QueryDSL |
| C | STOMP 配信 / Compose / CI |

**手動実装（厳守）**

- **実装・修復・レビュー反映は本人が手で書く**（学習目的。Agent が代行しない）
- Agent＝現状確認 → **小単位の写経（チャット提示）** → テスト写経セット → 解説【EJ】
- **禁止:** 本番／テストソースへの自動編集（方針決定・「直して」だけでは解除しない）
- **解除条件:** ユーザーが **「Agentがファイルを編集してよい」** と明示した依頼に限る
- 機能 ID / `docs/` 参照があればそれをスコープに

### 2. 依頼キーワード（`live-qa-keywords`）

語が**単独／文頭**なら実行。曖昧なら **確認**（先に docs とコード）。

**本番・テストのソースは本人が手で書く。** Agent は写経（チャット提示）と解説に徹する。

| する | しない |
|------|--------|
| 現状確認・差分説明・写経案・テスト写経・【EJ】 | `src/main` / `src/test` / frontend 本番への **Write・編集・一括適用** |
| `docs/qa/` 追記（memo）・契約 docs の**追記案の提示** | 「方針決まった」「2にする」「直して」だけでコードを勝手に書き換える |
| ユーザーが貼った断片のレビュー | レビュー指摘の**自動反映** |

- 「書いて」「実装して」「直して」「適用して」でも **手書き厳守は解除しない**
- 解除はユーザーが明示したときだけ: **「Agentがファイルを編集してよい」**（その一文がある依頼に限る）
- 違反したら即座に止めて謝罪し、入れた本番差分は戻す／戻し方を示す

| 語 | 手順 | チェック |
|----|------|----------|
| **実装** | 現状→次タスク→**本番写経**→**テスト写経（セット）**→解説 | 手書き厳守／単位小／テストなしで閉じない／Java: 明示型・`final`・DTOはService・Controllerは`ResponseEntity`／【EJ】／「できたら次」で区切る |
| **解説** | 説明優先（生成最小） | なぜ・流れ・型の正体／Javaなら【EJ】／必要なら `docs/qa/` |
| **確認** | 到達点・docs照合。**大規模編集しない** | 済み・暫定・未／openapi・ER差分／Cookie・CSRF・401等／Issue案はラボのみ（本番禁止） |
| **修復** | エラー→仮説→**最小写経** | 仮説→最小差分の**提示のみ**／無関係リファクタなし／手書き厳守 |
| **PR案作成** | 確認要約→メモ整理→**テンプレ準拠タイトル／本文をコピペ表示**（commit/push/ghは明示時のみ） | `.github/pull_request_template.md`／テスト有無が分かる／秘密なし |

### 3. 写経・解説（`live-qa-learning-explain`）

コードだけ置かない。Java は【EJ】必須。層・RE・DTO は backend ルール。

必ず書く:

1. なぜ作るか
2. 役割（受け取り→処理→返し）
3. データの流れ（Cookie / Redis / DB / SecurityContext を省略しない）
4. 操作語は意味つき／型は正体つき
5. 【EJ】沿っている／あえて外している

| 操作語 | 意味 |
|--------|------|
| 変換 | A→B の形へ |
| 格納 | ○○を△△へ書く |
| 交換 | コード→トークン |
| 復元 | 鍵で読み戻す |
| 発行 | 新規に渡す／作る |
| 検証 | 期待と一致するか |

**型の正体:** interface=契約／具象=実装／アノテーション=メタデータ／`@Bean`=DI定義／依存=何のための starter

```text
【EJ】不変性 … MeResponse は record
【EJ】依存性注入 … コンストラクタ
【EJ】あえて外す … Entity は JPA で可変
```

よく使う: 不変性／静的ファクトリ／enum／equals禁止(Entity)／例外／ジェネリクス／公開面最小化／コンストラクタ注入

**優先:** 明示型・`final`・分かる名前。**劣後:** `var`・無意味な短縮。

写経は **チャット提示**。本人が手でファイルへ書く。

**禁止:** コードだけ／意味なし用語羅列／【EJ】省略／可読性を落とす `var` 推奨／**Agent による本番ソース代筆**

### 4. 学習メモ（`live-qa-learning-memo`）

説明したらチャットだけで終わらせず **`docs/qa/` の機能メモへ追記**。索引 [`qa/README.md`](qa/README.md)。

**1 機能 = 1 md**（`f01-auth.md` …）。番号連番（`51-….md`）は増やさない。横断は `java.md` / `ops.md`。

| いつ | どこ |
|------|------|
| 概念・トラブル・解説/確認/修復の説明 | 該当 `f0x-*.md` に見出し追記（無ければ機能ファイルを新規＋README） |
| 写経だけの実装 | 必須ではない（補足したら機能メモへ） |

書き方: 日本語・質問見出し・秘密禁止。

**PR案作成時:** 今回スコープの `docs/qa/*` を列挙 → 重複は主ファイルへ → 設計 docs と矛盾したらメモ側を合わせる → README 索引 → 秘密なし。旧ラボの大規模改変・設計の無断変更はしない。

### 5. バックエンド（`live-qa-backend`）— Java / yml / sql

```
controller → service → repository → domain
dto/ … request|response（record）
db/migration/ … Flyway 追加のみ（既存 V* 不改変）
```

| 層 | する | しない |
|----|------|--------|
| Controller | principal、`@PreAuthorize`、`ResponseEntity` | 業務分岐、DTO組み立て本体 |
| Service | ユースケース、DB復元、**DTO組み立て** | `ResponseEntity` |
| Repository | DB | DTO / HTTP |
| Entity | 永続化 | API直出し |

- 戻り値 **`ResponseEntity` 統一**（200=`ok`、201=created/status、204=`noContent`）
- DTOは **`record`**。Service組み立て、Controllerが箱へ
- エラー: `{ status, error, message }`（400は任意 `fields`）
- oauth2Login → Session(Redis) → `SESSION` Cookie（JWT都度検証に戻さない）
- CSRF + `X-XSRF-TOKEN`。参加者は匿名。`voter_key`=`g:{id}`/`u:{id}`
- 認可用 ROLE → `Authentication` / `@PreAuthorize`。応答の role → ローカル `users.role`
- ルーム: `DRAFT→OPEN⇔CLOSED`。投稿・いいねは OPEN のみ。`upvote_count` は原子的更新
- `final User x = ...`。単純型を `var` にしない
- テスト: Service = JUnit + Mockito／Controller = `@WebMvcTest` + security-test。正常1 + 分岐0〜1

### 6. フロントエンド（`live-qa-frontend`）— `frontend/` の TS/TSX

- App Router + TS。ログインは Keycloak リダイレクト（自前フォームなし）
- REST: Cookie `SESSION`。変更系: `X-XSRF-TOKEN`
- STOMP: `/topic/events/{eventId}/questions`（MVP-B: `/polls`）。`votedByMe` 等は配信に載せない
- `DRAFT`/`CLOSED` では投稿・いいね UI 無効。権限の正は API
- TanStack Query / Zod / Tailwind + shadcn
- 依頼・写経は keywords / explain

---

## やってはいけないこと

- `.cursor/rules/` に **7 本目**を足す（役割固定の 6 本）
- Codex 用に別ルールを発明して `.mdc` と食い違わせる
- 「実装して」「直して」だけで `backend/src` や `frontend` の本番ソースを書き換える
- リポに `AGENTS.override.md` を置いてチームの `AGENTS.md` を隠す

---

## 任意: 手元の Codex CLI

チーム規範はリポの `AGENTS.md` + 本ファイル。個人の口調だけ `~/.codex/AGENTS.md` へ。

結合が 32 KiB を超えて切れるときだけ、手元の `~/.codex/config.toml` で上限を上げる（リポには書かない）。ファイル名の正は公式の [AGENTS.md ガイド](https://developers.openai.com/codex/guides/agents-md) を見る。

効いている指示の確認例: セッションで「今有効なプロジェクト指示を要約して」と聞く。6本・手書き厳守・キーワードが出なければ、本ファイルと `.mdc` を Read し直す。
