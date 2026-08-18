---
name: review-frontend
description: live-qa-app の Next.js フロントエンド（frontend/ 配下）をレビューする。Server/Client Component の分割、Cookie セッション連携、CSRF、STOMP のライブ更新まわりを、このプロジェクトの API 仕様との整合を含めて確認する。「フロントをレビューして」「この画面/コンポーネントを見て」と言われたとき、または frontend/ 配下の .tsx/.ts を変更したあとに使う。
allowed-tools: Read, Grep, Glob
---

# フロントエンドレビュー

レビュー専用。**コードは修正せず、指摘のみを出力する**。修正が必要な場合は「こう直す」を提示し、実際の編集はユーザーの指示を待つ。

## 前提となる設計（このプロジェクト固有）

指摘の根拠になるので必ず踏まえること。詳細は `docs/api/openapi-notes.md` と `docs/design/screen-flow.md` を参照。

- 構成は Next.js(App Router/SSR) + Spring Boot API の分離。API のベースパスは `/api`
- 認証は Keycloak(OIDC) でログイン → 以降は `SESSION` Cookie（Spring Session + Redis）。JWT はフロントで扱わない
- 参加者はログイン不要の**匿名セッション**。主催者のみ `HOST` ロール
- stateful なので **CSRF 有効**。`POST`/`PATCH`/`DELETE` は `X-XSRF-TOKEN` ヘッダが必要（`XSRF-TOKEN` Cookie と突合）
- STOMP 購読先は `/topic/events/{eventId}/questions` と `/topic/events/{eventId}/polls`
- **STOMP のペイロードにセッション依存の値は含まれない**（`votedByMe` / `myOptionIds` は各クライアントがローカル保持）
- ルーム状態は `DRAFT → OPEN ⇔ CLOSED`。`OPEN` 以外では投稿・いいね不可（サーバは 409 を返す）
- エラーは `{ status, error, message }`。400 のみ任意で `fields` が付く

## 手順

1. `frontend/` の構成を把握する（`app/` のルーティング、`"use client"` の付いているファイルを Grep で洗い出す）
2. API 呼び出し箇所、STOMP 接続箇所、型定義をそれぞれ特定する
3. 下のチェックリストを上から順に確認する
4. 指摘を **Critical / Warning / Nit** に分類して出力する

推測で断定しない。確認できていない箇所は「未確認」と明示する。

## チェックリスト

### A. Server / Client Component の分割

- [ ] `"use client"` が不必要に上位のコンポーネントに付いていないか（付けた時点で配下すべてがクライアントになる）
- [ ] Server Component 内で `useState` / `useEffect` / イベントハンドラを使っていないか
- [ ] SSR すべき画面（イベント一覧、参加ページ）が Client Component で `useEffect` フェッチになっていないか
- [ ] Server Component から Client Component へ関数やクラスインスタンスを props で渡していないか（シリアライズ不可）

### B. Cookie セッションの受け渡し ★頻出

- [ ] **Server Component からの `fetch` に Cookie を手動転送しているか**。`cookies()` で読んで `headers` に載せない限り `SESSION` は付かない。`GET /api/events` や `GET /api/me` を SSR しているなら必須
- [ ] `credentials: "include"` がクライアント側の fetch に付いているか（別オリジンの場合）
- [ ] **匿名セッションの `Set-Cookie` がブラウザに届く経路になっているか**。Server Component の `fetch` はレスポンスの `Set-Cookie` を素通ししない。参加ページでセッションを発行するなら Route Handler 経由かクライアントからの呼び出しが必要
- [ ] 401 のときにログイン導線へ誘導しているか（主催者画面）

### C. CSRF

- [ ] 状態変更系リクエストに `X-XSRF-TOKEN` を付けているか（質問投稿・いいね・ステータス変更・ポール回答すべて）
- [ ] トークンの取得元は `XSRF-TOKEN` Cookie か。ハードコードや使い回しになっていないか
- [ ] 初回描画時点で Cookie が存在しないケース（参加者の初アクセス）を踏んでいないか
- [ ] fetch ラッパーに集約されているか。各コンポーネントで個別に付けていると付け忘れが必ず出る

### D. STOMP / リアルタイム ★バグの温床

- [ ] **受信データとローカルの `votedByMe` をマージしているか**。配信ペイロードに含まれないので、そのまま state を置き換えると自分の投票状態が消える
- [ ] **楽観的更新と STOMP 受信の二重反映**になっていないか。自分のいいねは REST レスポンスと `QUESTION_UPVOTED` の両方が返る。どちらを正とするか一貫しているか
- [ ] `QUESTION_CREATED` で同じ質問が重複追加されないか（id での dedupe）
- [ ] **再接続時に REST で再同期しているか**。切断中の投稿を取りこぼす
- [ ] 購読解除（unsubscribe / disconnect）がクリーンアップ関数で行われているか。画面遷移のたびに購読が積み上がっていないか
- [ ] `useEffect` の依存配列に接続を張り直す値が入っていないか（毎レンダーで再接続する事故）
- [ ] 人気順の再ソートが読んでいる最中に走って行が飛ばないか（UX。ソートを遅延させる等の配慮があるか）
- [ ] TanStack Query を使っている場合、STOMP 受信を `setQueryData` で反映しているか、`invalidateQueries` で毎回再取得していないか（後者はリアルタイムの意味が薄れる）

### E. API 仕様との整合

- [ ] 型定義が `docs/api/openapi-notes.md` の DTO と一致しているか。特に `displayName` は `null` を取る、`myOptionIds` は配列
- [ ] `sort` / `status` のクエリパラメータの値が仕様通りか（`popular` / `newest`、`OPEN` / `ANSWERED` / `ARCHIVED`）
- [ ] エラーレスポンスを `{ status, error, message }` の形で扱っているか。`fields` は 400 のときのみ
- [ ] **409 のハンドリングがあるか**。「既に投票済み」「ルームが受付状態でない」「不正な状態遷移」はすべて 409 で、UI 側の無効化だけでは防げない
- [ ] `DRAFT` / `CLOSED` のときに投稿・いいね UI を無効化しているか

### F. 一般

- [ ] `any` を使っていないか。API レスポンスに型が付いているか
- [ ] ローディング / エラー / 空状態の3つが表示されるか
- [ ] `key` に index を使っていないか（並び替えがあるので id 必須）
- [ ] ニックネームや質問本文をそのまま `dangerouslySetInnerHTML` に渡していないか
- [ ] 環境依存の値（API のベース URL）がハードコードされていないか

## 出力フォーマット

```
## Critical
- `path/to/file.tsx:42` — 何が問題か。なぜ問題か。どう直すか。

## Warning
- ...

## Nit
- ...

## 未確認
- ...（読めていない、または判断材料が足りない箇所）
```

指摘がない項目は書かない。良い実装があれば1〜2点だけ簡潔に触れる。