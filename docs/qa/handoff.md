# バックエンド総評と次回への持ち込み（2026-08-26）

Member B の REST（MVP-A F-01〜F-06/F-08 ＋ MVP-B F-10〜F-13）完了時点の確認要約。正は openapi / ER / constraints。ここは JIT。

## 総評（一言）

**契約どおりの三層 Spring API として、B の範囲は完成している。** 認証の二系統（HOST=OIDC Session / 参加者=匿名 Session）を崩さず、状態遷移・所有者 404・1人1票・DTO `form` まで揃えた。残作業は配信（C）・画面（A）・載せ（#44）であり、REST を足すフェーズではない。

学習リポとしての成果は、コード量より **「なぜその層か」を言語化して手で書いたこと**。次回も同じ型で回せる。

## 全体確認（済み / 暫定 / 未）

| ID | 内容 | 状態 |
|----|------|------|
| F-01 | oauth2Login → Redis `SESSION`、JIT、`/api/me`、logout、CSRF | **済み** |
| F-02 | イベント CRUD の作成・一覧・Join | **済み** |
| F-03 | 開閉 `DRAFT→OPEN⇔CLOSED`、所有者 404、物理 DELETE、楽観ロック | **済み** |
| F-04 | `POST /api/auth/guest`、`voter_key` `g:`/`u:` | **済み**（ゲスト CSRF は ignore。#104） |
| F-05 | 質問投稿・いいね。OPEN のみ。UNIQUE 409 | **済み** |
| F-06 | QueryDSL 人気順/新着順 | **済み** |
| F-07 | STOMP 質問ライブ | **未（C 主）**。B は口なし |
| F-08 | 質問 `ANSWERED` / `ARCHIVED` | **済み** |
| F-09 | Compose / CI | **C 主**。B は `mvn test` 対象 |
| F-10 | ポール作成・一覧・開閉。終端 CLOSED。DELETE API なし | **済み** |
| F-11 | 回答・結果。SINGLE 409＋悲観ロック。MULTI は option UNIQUE | **済み** |
| F-12 | `?status=`。未指定は ARCHIVED 以外 | **済み** |
| F-13 | ARCHIVED 一覧は所有者のみ 403 | **済み** |
| F-14 / F-15 | ストレッチ | **対象外** |

テストは Service Mockito ＋ Controller `@WebMvcTest` が機能単位である。E2E は持たない（方針どおり）。

### 契約との差分（欠けではない／暫定）

| 項目 | 状態 | メモ |
|------|------|------|
| ポール単独 DELETE / CLOSED 再開 / SINGLE 置換 | 無し | constraints どおり |
| `POLL_UPDATED` | 口だけ | `PollService` コメント。C |
| Cookie `Secure=false` | **暫定** | ローカル HTTP。openapi の Secure は本番 HTTPS の日 |
| Swagger UI `permitAll` | **暫定** | ローカル便利。公開 IP では注意 |
| `anyRequest().permitAll()` | **暫定** | ゲスト票・Join を後段で拾う。新しいパスを足すときは matcher を先に書く |
| V1 の CHECK（role / event status / question status） | **暫定** | constraints は SQL 案あり。Flyway 現行は `ck_events_question_count` と V5 のポール系 |
| 500 の `error` 文字列 | 小差 | Handler は `INTERNAL_SERVER_ERROR`。openapi の英語表現は `Internal Server Error` 寄り |
| 母体ハブ構想 | MVP の正ではない | 将来話は docs に残しても実装スコープに入れない |

## コーディングの癖（本人）

次回 Agent が先回りして合わせる用。

| 癖 | どう出たか | Agent の合わせ方 |
|----|------------|------------------|
| **層の正しさを先に疑う** | Service が Session に触るのはおかしい、DTO 化は Service、Optional 引数は EJ 55 | 写経の前に層の表を出す。方針が決まるまでコードを増やさない |
| **将来拡張を先に考える** | role 複数、論理削除、母体アプリ | MVP の正と将来を分ける。採らないなら「今は外す」と【EJ】あえて |
| **認識確認が短い** | 「〜という認識でよい？」が連発 | 先に Yes/No。ずれならデータの流れで訂正。否定から始めない |
| **レビュー指摘をすぐ方針にする** | UNIQUE 名、SQL で所有者、匿名は `"匿名"`、`form`、IdToken 一本化 | 指摘の意図を契約に落とす。自動反映はしない（手書き） |
| **Builder より意図の名前** | `hostOf` / `create` / `form` | 静的ファクトリ。Builder は「問題ない」でも採らない理由を言う |
| **可読性 ≫ 行数** | `var` 劣後、明示型 + `final` | 短縮案を出さない。神メソッド指摘にはオーケストレーション分割 |
| **テストはセット** | 「確認、問題なければテスト」 | 本番写経とテスト写経を同じ単位で閉じる |
| **手で書くことにこだわる** | 「実装は手でやるルールでは？」 | `編集可` / `Agentがファイルを編集してよい` が無い限り src を触らない |
| **エラーとログをそのまま貼る** | Docker / Maven / 403 / 空の users | 仮説→最小。関係ないリファクタ禁止 |
| **コピペできる形を要求** | Issue コメント、PR 本文、返信案 | チャットにそのまま貼れるブロック |

## 質問でよく上がった内容

頻度の高い塊。次回も同じ順で説明する。

1. **Session / Cookie / Redis**  
   箱の鍵は Cookie、中身は Redis。`@AuthenticationPrincipal` は都度 JWT 検証ではない。
2. **OIDC / mapper / ROLE**  
   認可用は `Authentication` の `ROLE_*`（IdToken `realm_access`）。応答の `role` は `users`。混ぜない。Bean だけでは mapper は呼ばれない。
3. **JIT / `provisionHost` / `sub`**  
   ログイン成功時に User 行。表示名は IdToken クレームで、DB に持たない。
4. **CSRF**  
   Filter の仕事。Controller に書かない。`@WebMvcTest` の `addFilters=false` では CSRF を検証できない。ゲスト発行だけ ignore。
5. **401 vs 403**  
   Filter 未到達は Config。Controller に届いた認可失敗は Handler。UTF-8 は `getWriter()` 前。
6. **JPA**  
   静的ファクトリ、`protected` コンストラクタ、managed なら `save` 省略、UNIQUE 名を Flyway と揃える、Entity equals 禁止。
7. **所有者 404**  
   メモリで比較しない。SQL `id` + `host_id`。他人も無しも 404。
8. **Optional**  
   戻りには使う。引数・フィールドには使わない。無さは sealed `VoterKeySource`。
9. **DTO `form`**  
   Entity に `toXxx` しない。材料（票・得票・Oidc）は Service が渡す。
10. **状態とロック**  
    ルーム往復可、ポール CLOSED 終端。いいね UNIQUE。SINGLE は UNIQUE 不足 → `FOR UPDATE`。楽観ロックはイベント行の同時更新。
11. **匿名ニックネーム**  
    JSON も DB も `"匿名"`。`null` を API に出さない。
12. **環境**  
    issuer 無しでもアプリは上がる（初回リクエストまで）。ブラウザは `localhost:8080`、コンテナ間は `keycloak:8080`。IdToken に role が無いと 403。

## 今回使ったルール（6本＋入口）

増やさない。次回リポでも役割だけコピーする。

| # | ファイル | 役割 |
|---|----------|------|
| 1 | `live-qa-project.mdc` | 日本語・docs 優先・MVP スコープ・手書き |
| 2 | `live-qa-keywords.mdc` | **実装 / 解説 / 確認 / 修復 / PR案作成**。手書き解除は明示文だけ |
| 3 | `live-qa-learning-explain.mdc` | なぜ・役割・流れ・型・**【EJ】**。写経はチャット |
| 4 | `live-qa-learning-memo.mdc` | 説明したら `docs/qa/`。1機能1md |
| 5 | `live-qa-backend.mdc` | 三層・record・`ResponseEntity`・Cookie Session・テスト層 |
| 6 | `live-qa-frontend.mdc` | A 領域（B は参照程度） |

入口: ルート `AGENTS.md`、backend `AGENTS.md`。Codex は `.mdc` が自動注入されないので `docs/codex.md`。

キーワードの型は次回もそのまま使える。Java 形（明示型・`final`・DTO は Service）は backend ルール。

## Agent への申し送り（次回プロジェクト）

- 依頼語が文頭なら即実行。曖昧なら先に確認。
- 実装・修復は写経提示。`src/main` / `src/test` を編集するのは解除文があるときだけ。
- 解説はコードだけ置かない。操作語（変換・格納・交換・復元・発行・検証）を意味つきで。
- 契約（openapi / ER / 制約）とコードが矛盾したら「docs を直すか実装を合わせるか」を先に聞く。
- 将来の母体ハブは MVP に混ぜない。
- PR はテンプレ準拠のコピペ。commit/push は明示時のみ。
