# JIT（Just-In-Time）プロビジョニングとは

## 質問

F-01 仕上げの **JIT** とは何か。なぜ必要か。

---

## 一言で

**JIT** = Just-In-Time（その場・ちょうどそのとき）。  
ここでは「主催者が Keycloak でログインした**その瞬間**に、ローカル DB の `users` 行を**初めて必要なら作る**」こと。

事前に管理画面でユーザーを手登録しない。ログイン成功の流れの中で**発行（INSERT）**する。

---

## なぜ必要か（本プロジェクト）

| 世界 | 何を持っているか |
|------|------------------|
| **Keycloak（IdP）** | 人の正体。`sub`（OIDC subject）、名前、email、realm ロール |
| **ローカル DB `users`** | アプリ内の主キー `id`。イベントの `host_id` など **FK の受け皿** |

Keycloak の `sub` は長い文字列で、DB の `events.host_id` に直接載せる設計ではない（er-diagram は `host_id → users.id`）。

だからログイン時に:

1. `sub` で `users` を**検索**
2. 無ければ `User.hostOf(sub)` で行を**生成**して**格納**（これが JIT）
3. 以降はローカル `id` を HOST の識別子として使う（`/api/me` の `id`、将来の `host_id`）

openapi の言い方: 「IDトークンの `sub` で `users` を JIT プロビジョニング」。

---

## データの流れ（仕上げ後のイメージ）

```text
ブラウザ → Keycloak ログイン
  → コールバック /login/oauth2/code/keycloak
  → 認可コードをトークンに交換
  → OidcUser をロード（クレーム取得: sub, name, email, realm_access）
  → ★ JIT: UserService.provisionHost(sub)
        → findByOidcSubject
        → 無ければ save(User.hostOf(sub))   ← ここで users 行が生まれる
  → SESSION Cookie 発行（以降は Cookie で復元）
  → GET /api/me
        → 同じ sub で users を復元
        → 応答の id / role はローカル行、displayName / email はクレーム
```

★ が無いと: ログインは成功しても `users` が空のまま → `/api/me` の数値 id も、イベント作成の `host_id` も作れない。

---

## 「プロビジョニング」の意味

**プロビジョニング** = 使うために必要な記録・権限・資源をあらかじめ（または必要時に）用意すること。

- **事前プロビジョニング**: 管理者が先に `users` を INSERT しておく  
- **JIT プロビジョニング**: 初回ログイン時に自動 INSERT（本アプリの方針）

会員登録フォームを自前で持たない（Keycloak に委譲）ので、ローカル行は JIT が自然。

---

## コード上の役割分担

| 部品 | 役割 |
|------|------|
| `User.hostOf(sub)` | HOST 行の**生成**（静的ファクトリ） |
| `UserRepository.findByOidcSubject` | **検索** |
| `UserService.provisionHost` | 検索 → 無ければ生成＋**格納**（JIT 本体） |
| `OidcUserService` 差し込み | ログイン成功の流れで `provisionHost` を呼ぶ（配線） |
| `MeController` | JIT 済み行を**復元**して JSON にする（通常は新規作成しない） |

いま `UserRepository` まで写経済みなら、次は `UserService.provisionHost` が JIT の中心処理。

---

## JIT しない場合との違い

| | JIT あり | JIT なし（いまの骨組み） |
|--|----------|---------------------------|
| `/api/me` の `id` | ローカル PK（数値） | OIDC `sub`（文字列）が多い |
| `events.host_id` | `users.id` を入れられる | FK の受け皿が無い |
| Keycloak だけ消えた人 | ローカル行は残る（履歴の持ち主） | アプリ内に HOST 記録が無い |

---

## 注意（よくある誤解）

- JIT は **Keycloak にユーザーを作る処理ではない**（Keycloak 側のユーザーは既にある）  
- JIT は **毎回新しい行を作るわけではない**（2回目以降は find だけ）  
- `displayName` / `email` は JIT で DB に保存しない（openapi: クレームから都度返す）  
- 権限 `ROLE_HOST` のマッピング（`GrantedAuthoritiesMapper`）と JIT は別物。前者は Security、後者は DB 行

---

## 関連

- [14-f01-finish.md](14-f01-finish.md) — 仕上げの写経順  
- [openapi-notes.md](../api/openapi-notes.md) — 認証節の JIT 記述  
- [er-diagram.md](../design/er-diagram.md) — `users.oidc_subject` UNIQUE
