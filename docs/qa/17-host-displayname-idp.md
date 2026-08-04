# HOST 表示名はローカルに持つべきか（IdP と日本語）

## 質問

IdP への登録で日本語ができなかった記憶がある。HOST の表示名は別（ローカル）で用意すべきでは？

## 結論（MVP）

**F-01 仕上げでは、まずローカル `users.display_name` を足さない。**  
いまの docs の正は「表示名はクレームから都度。ローカルは `id` / `oidc_subject` / `role` の橋」。

日本語表示が本当に必要で、かつ IdP に載せられないと分かったら、**docs（er-diagram / openapi）を先に更新してから** Flyway で列追加する。F-01 の JIT 配線と同時にやるとスコープが膨らむ。

## 記憶とのすり合わせ

| よくある制限 | 意味 |
|--------------|------|
| **username** | メール形式や ASCII のみ、など realm / ポリシーで厳しいことが多い |
| **firstName / lastName** | Keycloak 本体は UTF-8（日本語）を入れられることが多い |
| テストユーザー | LibraShare realm JSON の例は `admin` / `test` など英字のみ（「日本語不可」の証明ではない） |

「登録できなかった」が **username** の話か、**氏名フィールド**の話か、**別システムのバリデーション**かを切り分けるとよい。  
Keycloak 管理コンソールで firstName に日本語を入れてトークンの `name` / `given_name` に出るか、一度確認するのが早い。

## 選択肢

| 案 | 向くとき | 欠点 |
|----|----------|------|
| **A. クレームのまま**（現行 docs） | IdP に名前を載せられる／英語 username でデモ十分 | 日本語を IdP に置けないと `/api/me` が寂しい |
| **B. フォールバック** | クレーム空なら `preferred_username` や固定「主催者」 | 日本語の「表示名」にはならない |
| **C. ローカル `users.display_name`** | live-qa 専用の日本語表示が必須 | プロフィールの正がアプリに寄る。母体ハブ構想と二重管理になりやすい |
| **D. 母体アプリのプロフィール**（将来） | LibraShare / live-qa 横断で日本語名 | 今は未実装。[platform-hub-vision.md](../design/platform-hub-vision.md) |

「別で用意する必要がある」は **プロダクト要件としてはあり得る**。  
ただし置き場所は C より、長期的には **D（母体）** か **A（IdP の氏名を UTF-8 で使う）** の方が構想と合う。

## F-01 でやること

1. `provisionHost` / `/api/me` は現行どおり（id/role=DB、displayName=クレームまたは B のフォールバック）
2. IdP で日本語氏名が無理と確定したら Issue 化 → er-diagram + openapi 更新 → `V3__…` で `display_name`（任意）
3. 参加者の `questions.display_name` は別物（投稿ニックネーム）のまま

## 関連

- [er-diagram.md](../design/er-diagram.md) users 節（ローカルに表示名を持たない）
- [openapi-notes.md](../api/openapi-notes.md) GET /api/me
- [16-userservice-when-oidcsubject.md](16-userservice-when-oidcsubject.md)
