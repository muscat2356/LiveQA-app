# live-qa-app — Gemini Code Assist レビュー用スタイルガイド

このファイルは [Gemini Code Assist on GitHub](https://developers.google.com/gemini-code-assist/docs/code-review-style-guide) の `styleguide.md` です。  
PR レビュー時に、本リポジトリの設計・分担・MVP スコープに沿った指摘をしてください。

## 言語・トーン

- レビューコメントは**日本語**で書く
- 指摘は短く具体的に（ファイル・懸念・推奨を1コメントにまとめる）
- 学習用ポートフォリオ（14日・3名・アジャイル）であることを踏まえ、過剰なエンタープライズ要求はしない
- 褒めるべき点（設計 docs との一致、セキュリティ考慮）は短く肯定してよい

## 設計の正（矛盾したら必ず指摘）

優先順位は次のとおり。コードがこれらと矛盾する場合は **HIGH** 以上で指摘する。

1. `docs/`（特に `docs/api/openapi-notes.md` / `docs/design/er-diagram.md` / `docs/design/constraints.md` / `docs/design/websocket-sequence.md`）
2. `README.md` / `docs/scope.md`（MVP-A 死守、対象外機能）
3. `.github/pull_request_template.md` の確認項目（該当するもの）

コードだけ見て「一般的なベストプラクティス」で docs を上書きする提案はしない。差分がある場合は「docs を更新するか、実装を docs に合わせるか」を明示する。

## PR テンプレートとの照合

PR 本文が `.github/pull_request_template.md` 形式なら、次を確認する。

- 担当（front / back / infra）と差分の領域が一致しているか
- スコープ（MVP-A / MVP-B / ストレッチ）と変更内容が一致しているか
- 機能 ID（F-xx）が差分と対応しているか。無関係な F-xx のチェックや、必須なのに未記載なら指摘
- 「確認」チェックが付いているのに、該当するテスト観点（CSRF、401/403、OPEN のみ投稿など）がコード上危うい場合は指摘

## チーム分担（越境の指摘）

| 領域 | 主担当 | レビューで見る |
|------|--------|----------------|
| `frontend/` | Member A | App Router、Cookie 付き REST、STOMP **受信** UI |
| `backend/` Session・CRUD・Security | Member B | oauth2Login→Session、CSRF、API、Flyway、QueryDSL |
| STOMP **配信**・Compose・CI | Member C | WebSocket 設定、ハンドシェイク Cookie、`compose.yml`、workflows |

1つの PR が複数領域にまたがる場合は、境界が曖昧な変更（例: B が STOMP 設定全体を抱え込む、A が API の認可をフロントだけで担保しようとする）を指摘する。

## セキュリティ・認証（必須チェック）

次に反する変更は **CRITICAL または HIGH**。

- 主催者: Keycloak OIDC（`oauth2Login`）→ **Cookie セッション（Spring Session + Redis）**。以降は `SESSION` Cookie。API で JWT を都度検証する stateless に戻さない
- 状態変更（POST/PATCH/DELETE）は **CSRF**（`XSRF-TOKEN` Cookie + `X-XSRF-TOKEN` ヘッダ）
- 参加者は SSO 不要。匿名セッション + `voter_key`（`g:{guestId}` / `u:{userId}`）
- HOST 操作は `@PreAuthorize("hasRole('HOST')")` **かつ** イベント所有者チェック。UI の出し分けだけでは不十分
- エラー JSON は `{ status, error, message }`（400 は任意で `fields`）。HTML エラーページやメッセージだけの 500 を増やさない
- `.env` / client secret / パスワードのコミットを検出したら CRITICAL

## ドメイン・状態遷移

- ルーム: `DRAFT → OPEN ⇔ CLOSED`（`DRAFT` へ戻さない）。質問投稿・いいね・Poll 回答はルーム `OPEN` のみ（違反は 409 系）
- 質問: `OPEN` / `ANSWERED` / `ARCHIVED`
- Poll（MVP-B）: `DRAFT → OPEN → CLOSED`
- MVP-A/B では原則 **行の物理削除を増やさない**（いいね取り消しの vote DELETE は例外）。論理相当は status
- `upvote_count` は原子的更新。楽観ロック `version` を新規導入する提案は docs と矛盾するので慎重に

## バックエンド（`backend/`）

- レイヤ: Controller → Service → Repository。Controller に業務分岐を書かない
- DB 変更は **Flyway 追加のみ**（既存 `V*` の書き換え禁止）
- Flyway パスは `src/main/resources/db/migration/`（二重ディレクトリにしない）
- DTO は不変（`record` 推奨）。Entity を API レスポンスに直接出さない
- 生成は意図の分かる静的ファクトリを推奨（例: `User.hostOf` / `Event.create`）
- `GrantedAuthoritiesMapper` は `@Bean` だけでは不十分。`oauth2Login(...userAuthoritiesMapper(...))` 差し込みを確認
- `/api/me` の role は `OidcUser.getAuthorities()` ではなく **Authentication** 側を見る実装になっているか

## Effective Java（`backend/` の Java 差分）

Java 実装のレビューでは **Effective Java（第3版）の考え方を意識**する。  
ただし **プロジェクト確定方針（レイヤ・セキュリティ・Flyway・docs）が優先**。Spring / JPA の慣習と EJ が衝突する場合は、docs と確定方針に合わせ、EJ の「教科書どおり」を無理に強要しない。

指摘するときはコメントに「EJ: 不変性」など観点名を短く添える。学習リポなので、Item 番号の羅列より「何が危ないか / どう直すか」を優先する。

### 積極的に見る（MEDIUM〜HIGH）

| 観点 | 見るポイント | 反例・注意 |
|------|--------------|------------|
| **不変性** | Req/Res DTO は `record` や不変オブジェクト。Entity と DTO の役割を混ぜない | Entity をそのまま JSON 返却、可変 DTO のフィールド公開 |
| **静的ファクトリ** | 生成意図が名前で伝わる（`hostOf` / `create` / `post` / `cast` 等） | 意味のない public コンストラクタ乱立、遷移を setter 連打だけ |
| **enum** | ルーム/質問/Poll の状態は enum（永続化は `EnumType.STRING` 等） | 状態を裸の String / int 定数の散在 |
| **equals / hashCode** | JPA Entity で安易な全フィールド equals を入れない。方針未定なら未実装でよい場合もある | Set/Map 用にフィールド全部 equals |
| **例外** | 業務エラーは unchecked → 共通エラー JSON。握りつぶし・チェック例外の乱用を避ける | `catch (Exception e) {}`、意味のない checked の伝播 |
| **ジェネリクス** | Repository・コレクションで raw type を使わない | `List` 生型、未チェックキャストの放置 |
| **リソース** | 手動 close より try-with-resources（ストリーム・外部接続） | `close()` 忘れ、finally だけの古い書き方の新規追加 |
| **公開 API の最小化** | パッケージ／メソッドを必要以上に public にしない | Controller 以外から触る必要のない API の public 化 |

### 指摘しすぎない

- Builder / 継承階層 / 並行ユーティリティの過剰導入（14日・MVP に見合わない抽象化）
- 「EJ の Item 全部に合わせよ」という網羅要求
- Spring Data / Security の定石を EJ だけで否定するコメント（例: インターフェース Repository、DI による可変 Bean）

### コメント例

```text
【重要度】MEDIUM
【観点】EJ: 不変性 / DTO
【問題】Entity をレスポンスに直接返しており、永続化モデルが API 契約に漏れる
【推奨】response DTO（record）へ写し、openapi-notes の形に合わせる
```

## フロントエンド（`frontend/`）

- Next.js App Router + TypeScript。セキュリティの正は API
- REST は Cookie 付き。状態変更に CSRF ヘッダ
- STOMP 購読: `/topic/events/{eventId}/questions`（MVP-B は `/polls` も）
- 配信ペイロードに `votedByMe` / `myOptionIds` を載せる変更は契約違反として指摘
- ルーム `DRAFT` / `CLOSED` で投稿・いいね UI を有効にしたままにしない

## WebSocket / Docker（`compose`・STOMP）

- STOMP トピック・ペイロードは `openapi-notes` / `websocket-sequence` と一致させる
- ハンドシェイクは同じ `SESSION` Cookie
- Compose サービス: `nextjs` / `api` / `postgres` / `redis` / `keycloak` の意図を壊すポート・依存の変更は理由を求める

## スコープ制御（学習プロジェクト向け）

次は **指摘しすぎない / 必須にしない**（MEDIUM 未満相当、またはコメントしない）。

- カバレッジ100%、完全な E2E、マイクロサービス分割、過剰な抽象化
- MVP-B / ストレッチ機能を MVP-A の PR に無理に入れる提案
- 決済・メール・ファイル添付など `docs/scope.md` の対象外

次は **積極的に指摘する**。

- docs / openapi / ER との不一致
- セキュリティ退行（CSRF 無効化、JWT 都度検証への逆戻り、所有者チェック漏れ）
- Flyway 既存ファイルの破壊的編集
- secret のコミット
- 1人1票（UNIQUE / voter_key）の破壊
- Java 差分での EJ 観点の明確な退行（Entity の API 直出し、状態の String 散在、例外の握りつぶし等）

## コメントの書き方

```text
【重要度】HIGH
【観点】認可 / openapi / EJ: 不変性 など
【問題】何が起きうるか（1文）
【推奨】具体的な修正方針（docs への参照があればパスを付ける）
```

質問だけの場合は「確認したい点」と明記し、ブロッキングかどうか分ける。
