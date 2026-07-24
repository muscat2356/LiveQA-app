# シーケンス図（WebSocket / REST）

ライブQ&Aの主要フローをシーケンス図で示す。サービス仕様は [service-spec.md](service-spec.md)、画面遷移は [screen-transition.md](screen-transition.md) を参照。

## 1. E2E全体フロー

主催者ログイン → ルーム作成（`DRAFT`）→ 開始（`OPEN`）→ 参加者参加 → 質問 → いいね → ステータス変更まで。

```mermaid
sequenceDiagram
    participant H as 主催者(ブラウザ)
    participant P as 参加者(ブラウザ)
    participant Next as Next.js(SSR)
    participant API as Spring Boot REST
    participant WS as WebSocket/STOMP
    participant Redis as Redis(Session)
    participant DB as PostgreSQL

    H->>API: GET /oauth2/authorization/keycloak
    API-->>H: Keycloakへリダイレクト
    Note over H: Keycloak(SSO)でログイン → 認可コードでコールバック
    H->>API: GET /login/oauth2/code/keycloak (code)
    API->>Redis: トークン交換→JIT作成→セッション作成(SESSION Cookie, role=HOST)
    API-->>H: 主催者ダッシュボードへリダイレクト(Set-Cookie SESSION)

    H->>API: POST /api/events (title)
    API->>DB: event保存(joinCode発行, status=DRAFT)
    API-->>H: eventId + 参加URL(/join/{joinCode})

    H->>API: PATCH /api/events/{id}/status (OPEN)
    API->>DB: status更新(DRAFT→OPEN, 受付開始)

    Note over H,P: 主催者が参加URL または 参加コードを共有

    alt A. 参加URL直リンク
        P->>Next: GET /join/{joinCode}
    else B. 参加コード入力
        P->>Next: GET /join (コード入力ページ)
        P->>API: GET /api/join/{joinCode} (コード検証)
        API-->>P: 有効 → /join/{joinCode} へ遷移
        P->>Next: GET /join/{joinCode}
    end
    Next->>API: GET /api/join/{joinCode} (SSR)
    API->>Redis: ゲストセッション発行(guestId, 任意nickname)
    API-->>Next: イベント情報
    Next-->>P: 参加ページ(SSR) + Set-Cookie SESSION
    P->>WS: CONNECT + SUBSCRIBE /topic/events/{id}/questions (Cookie)

    P->>API: POST /api/events/{id}/questions (body, nickname?)
    API->>DB: question保存(status=OPEN)
    API-->>WS: QUESTION_CREATED を publish
    WS-->>P: /topic/events/{id}/questions に配信
    WS-->>H: 同上(運営画面にも反映)

    P->>API: POST /api/questions/{qid}/vote
    API->>DB: votes一意制約チェック + upvote_count更新
    API-->>WS: QUESTION_UPVOTED を publish
    WS-->>P: 人気順ライブ再ソート

    H->>API: PATCH /api/questions/{qid}/status (ANSWERED)
    API->>DB: status更新
    API-->>WS: QUESTION_ANSWERED を publish
    WS-->>P: ステータスをライブ反映
```

## 2. 質問投稿シーケンス

REST保存 → STOMP publish → 全参加者へ配信。投稿者自身も配信経由でUIを更新する（楽観的更新も可）。

```mermaid
sequenceDiagram
    participant P as 参加者(ブラウザ)
    participant API as Spring Boot REST
    participant DB as PostgreSQL
    participant WS as WebSocket/STOMP
    participant Others as 他の参加者/主催者

    P->>API: POST /api/events/{id}/questions (body, nickname?)
    API->>API: ルームがOPENか確認 + バリデーション
    API->>DB: question保存(display_name=nickname or 匿名)
    DB-->>API: 保存済みquestion
    API-->>P: 201 Created (question)
    API-->>WS: QUESTION_CREATED を publish
    WS-->>P: /topic/events/{id}/questions
    WS-->>Others: /topic/events/{id}/questions
    Note over P,Others: 全クライアントの質問リストにライブ追加
```

## 3. いいねシーケンス（二重投票防止）

`votes` の一意制約 `(question_id, voter_key)` で1人1票を担保し、カウント更新後に人気順をライブ再ソート。

```mermaid
sequenceDiagram
    participant P as 参加者(ブラウザ)
    participant API as Spring Boot REST
    participant DB as PostgreSQL
    participant WS as WebSocket/STOMP

    P->>API: POST /api/questions/{qid}/vote (Cookie)
    API->>API: voter_key算出(u:{userId} or g:{guestId})
    API->>DB: INSERT votes(question_id, voter_key)
    alt 一意制約違反(既に投票済み)
        DB-->>API: 制約エラー
        API-->>P: 409 Conflict (投票済み)
    else 新規投票
        DB-->>API: OK
        API->>DB: upvote_count を原子的インクリメント(UPDATE ... +1)
        API-->>P: 200 OK (最新カウント)
        API-->>WS: QUESTION_UPVOTED を publish
        WS-->>P: 人気順ライブ再ソート
    end
```

## 4. WebSocketハンドシェイクのCookie認証

WebSocket接続時に、RESTと同じ `SESSION` Cookieでセッションを検証する。

```mermaid
sequenceDiagram
    participant B as ブラウザ
    participant WS as WebSocket/STOMP
    participant Redis as Redis(Session)

    B->>WS: WebSocket handshake (Cookie: SESSION)
    WS->>Redis: SESSION IDでセッション取得
    alt セッション有効
        Redis-->>WS: セッション情報(role, guestId/userId)
        WS-->>B: 接続確立
        B->>WS: SUBSCRIBE /topic/events/{id}/questions
        Note over WS: ステータス変更などHOST操作はrole=HOSTのみ許可
    else セッション無効/なし
        WS-->>B: 接続拒否
    end
```

## 5. ポールのライブ集計（MVP-B・アンケート型）

主催者が設問を開始し、参加者が選択肢へ回答 → 得票分布をライブ配信する。購読先は `/topic/events/{eventId}/polls`。

```mermaid
sequenceDiagram
    participant H as 主催者(ブラウザ)
    participant P as 参加者(ブラウザ)
    participant API as Spring Boot REST
    participant DB as PostgreSQL
    participant WS as WebSocket/STOMP

    H->>API: POST /api/events/{id}/polls (設問+選択肢)
    API->>DB: polls(status=DRAFT) + poll_options を保存
    H->>API: PATCH /api/polls/{pollId}/status (OPEN)
    API-->>WS: POLL_OPENED を publish
    WS-->>P: 回答UIをライブ表示

    P->>API: POST /api/polls/{pollId}/responses (optionIds)
    API->>API: ポールがOPENか + 1票判定(SINGLE)
    API->>DB: poll_responses を保存(UNIQUE(option,voter)で重複防止)
    alt 既に回答済み(SINGLE)
        DB-->>API: 制約/既存あり
        API-->>P: 409 Conflict
    else 新規回答
        DB-->>API: OK
        API->>DB: 選択肢ごとに GROUP BY COUNT で集計
        API-->>P: 200 OK (PollResultDTO)
        API-->>WS: POLL_UPDATED を publish
        WS-->>P: 得票分布をライブ更新(棒グラフ)
    end

    H->>API: PATCH /api/polls/{pollId}/status (CLOSED)
    API-->>WS: POLL_CLOSED を publish
    WS-->>P: 回答受付を締切、最終結果を表示
```
