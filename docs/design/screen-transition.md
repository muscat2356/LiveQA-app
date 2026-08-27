# 画面遷移図

ライブQ&Aアプリの画面一覧と遷移。主催者フローと参加者フローに分かれる。詳細な仕様は [service-spec.md](service-spec.md) を参照。

## 全体像

```mermaid
flowchart TD
    subgraph host [主催者フロー]
        Login["ログイン導線"] -->|Keycloakへリダイレクト| KC[Keycloak SSO]
        Register["HOST メール登録"] -->|POST /api/auth/register| API[Spring]
        API -->|Admin API + 設定メール| KC
        KC -->|コールバック→セッション発行| Dash[主催者ダッシュボード]
        Dash --> Create[イベント作成画面]
        Create --> Manage["イベント運営画面 (質問+参加URL+開閉)"]
        Dash --> Manage
    end
    subgraph guest [参加者フロー]
        CodeEntry["参加コード入力 /join"] -->|コード検証OK| Join
        Join["参加ページ /join/{joinCode}"] --> QA["Q&A画面 (投稿/いいね/人気順)"]
    end
    Manage -.参加URLを共有.-> Join
    Manage -.参加コードを共有.-> CodeEntry
```

## 主催者フロー

```mermaid
flowchart TD
    Start([アクセス]) --> Auth{ログイン済み?}
    Auth -->|No| Home["トップ（ログイン / 登録）"]
    Home -->|GET /oauth2/authorization/keycloak| KC[Keycloak SSO ログイン]
    Home -->|メール送信 POST /api/auth/register| Mail[パスワード設定メール]
    Mail --> KC
    KC -->|"コールバック /login/oauth2/code/keycloak → セッション発行"| Dash[主催者ダッシュボード]
    Auth -->|Yes| Dash
    Dash -->|新規作成| Create[イベント作成画面]
    Create -->|作成完了| Manage[イベント運営画面]
    Dash -->|イベント選択| Manage
    Manage -->|URLコピー| Share([参加URLを共有])
    Manage -->|名称変更| Manage
    Manage -->|イベント削除| Dash
    Manage -->|ステータス変更| Manage
    Manage -->|"ルーム開始/締切/再開 (DRAFT→OPEN⇔CLOSED)"| Manage
```

- ログイン導線: 未ログイン時のみ表示。「主催者としてログイン」でKeycloak(SSO)へリダイレクト。**パスワード入力フォームは持たない**（メールだけの HOST 登録は `POST /api/auth/register`）
- コールバック後にCookieセッションが発行され、以降はセッションで認証
- 主催者ダッシュボード: 自分のイベント一覧（`GET /api/events`）。名称変更（`PATCH /api/events/{id}`）と削除（`DELETE /api/events/{id}`）もここまたは運営画面から行う
- イベント作成画面: タイトル入力 → `POST /api/events` → joinCode発行（`DRAFT`）→ 運営画面へ
- イベント運営画面: 質問リスト（ライブ）+ ステータス操作 + 参加URL表示 + ルーム開始/締切/再開 + 名称変更・削除。作成直後は `DRAFT` なので「開始」で `OPEN` にして受付を始める。削除は確認ダイアログ必須。成功後はダッシュボードへ戻る
- **MVP-B**: 同じ運営画面にポール作成 / 開始 / 締切。参加者 Q&A に OPEN ポールの回答と棒グラフ。購読は `/topic/events/{eventId}/polls`

## 参加者フロー（ログインなし）

```mermaid
flowchart TD
    Entry{参加方法を選択}
    Entry -->|A. URL直リンク| Open([参加URLを開く])
    Entry -->|B. コード入力| Code["参加コード入力 /join"]
    Code -->|GET /api/join/joinCode で検証| Valid{有効なコード?}
    Valid -->|No| CodeErr[エラー表示: 該当ルームなし]
    CodeErr --> Code
    Valid -->|Yes| Open
    Open --> JoinPage["参加ページ /join/{joinCode}"]
    JoinPage --> Session[匿名セッション発行]
    Session --> Nick{ニックネーム入力?}
    Nick -->|任意入力| QA[Q&A画面]
    Nick -->|スキップ| QA
    QA -->|質問投稿| QA
    QA -->|いいね/取り消し| QA
    QA -->|WebSocket受信| QA
```

- 参加方法は2通りから選択可能: **A. 参加URL直リンク** / **B. 参加コード入力**（`/join` のフォームに手入力）
- 参加コード入力: `GET /api/join/{joinCode}` でコードの有効性を検証し、有効なら `/join/{joinCode}` へ遷移。無効ならエラー表示
- 参加ページ: `GET /api/join/{joinCode}` でイベント情報を取得（SSR）。任意ニックネーム入力
- Q&A画面: 質問投稿・いいね・人気順ライブ表示。STOMPで `/topic/events/{eventId}/questions` を購読
- ルームが `DRAFT`（受付前）/ `CLOSED`（締切）の場合は閲覧のみ（投稿・いいね不可）
- **MVP-B**: OPEN なポールがあれば回答 UI と得票分布。`/topic/events/{eventId}/polls` も購読
