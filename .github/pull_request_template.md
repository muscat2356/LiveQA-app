## 概要
<!-- 1〜2行で何を変えたかだけ書く -->

## 関連 Issue（1つ選ぶ）
- [ ] Closes #
- [ ] Refs #
- [ ] なし

## 担当（該当だけ）
- [ ] front（Member A / Next.js）
- [ ] back（Member B / Session+API）
- [ ] infra（Member C / WS+Docker）

## スコープ（1つ選ぶ）
- [ ] MVP-A（F-01〜F-09）
- [ ] MVP-B（F-10〜F-13）
- [ ] ストレッチ（F-14〜F-15）
- [ ] 対象外ではない（docs / リファクタ等）

## 機能 ID（該当だけ）
- [ ] F-01 認証・セッション（Keycloak SSO → Cookie）
- [ ] F-02 イベント作成・参加コード/URL
- [ ] F-03 ルーム状態（DRAFT → OPEN ⇔ CLOSED）
- [ ] F-04 匿名参加
- [ ] F-05 質問投稿・いいね
- [ ] F-06 人気順 / 新着順
- [ ] F-07 WebSocket(STOMP)ライブ反映
- [ ] F-08 質問ステータス変更
- [ ] F-09 Docker / CI
- [ ] F-10 / F-11 Poll（MVP-B）
- [ ] F-12 質問フィルタ（MVP-B）
- [ ] F-13 簡易モデレーション（MVP-B）
- [ ] F-14 / F-15 ストレッチ（プレゼンス / QR）
- [ ] なし

## 確認（該当だけ）
- [ ] Issue の完了条件を満たす
- [ ] 動作確認した（手動で十分）
- [ ] HOST（主催者）で確認した
- [ ] 参加者（匿名）で確認した
- [ ] DRAFT / OPEN / CLOSED の状態差を確認した（ルーム操作があるとき）
- [ ] 401 / 403 / 409 / エラーJSON（status/error/message）を確認した（該当時）
- [ ] STOMP配信・受信を確認した（ライブ更新があるとき）
- [ ] CSRF（X-XSRF-TOKEN）を確認した（状態変更があるとき）
- [ ] screen-transition と矛盾なし（画面・ルート変更時）
- [ ] openapi-notes と矛盾なし（API 変更時）
- [ ] er-diagram / constraints と矛盾なし（DB・制約変更時）
- [ ] README の機能 ID / スコープと矛盾なし
