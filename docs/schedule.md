# 14日スケジュール（アジャイル）

学習フェーズ → MVP-A → MVP-B → 仕上げのイテレーション型。3名は A=Next.js、B=Cookieセッション+Redis、C=WebSocket+Docker を主役として担当する。機能 ID は [../README.md](../README.md) / [scope.md](scope.md) が正。

## 進め方の原則

- 各イテレーション末に「動くインクリメント」をデモ + ふりかえり
- 設計は `docs/` を正とし、実装と並走して更新する
- Day4「基盤疎通ゲート」と MVP-A 末「Go/No-Go」を必ず通過確認する
- **MVP-A 未達なら MVP-B に進まず、仕上げを優先**

| フェーズ | 期間 | 内容 |
|---------|------|------|
| 学習フェーズ | Day1-4 | 各自が主役技術をJIT学習しつつ基盤構築（**基盤疎通ゲート**） |
| MVP-A（Sprint 1） | Day5-9 | **F-01〜F-09** ライブQ&Aコア。末尾 Go/No-Go |
| MVP-B（Sprint 2） | Day10-13 | **F-10〜F-13** Poll + 運用強化 |
| 仕上げ | Day14 | E2E / README・スクショ / 相互デモ / バグ修正・UX |

## Day1: キックオフ（学習フェーズ開始）

- テーマ・MVP-A/B・ロール分担・リポジトリ / ブランチ運用を合意
- C=Docker Compose骨組み（`nextjs` / `api` / `postgres` / `redis` / `keycloak`）に着手

## Day2-4: 学習フェーズ（JIT学習 + 基盤）

| Day | Member A | Member B | Member C |
|-----|----------|----------|----------|
| 2-3 | Next.js（App Router / SSR）土台 + ログイン導線 + イベント一覧骨組み | Keycloak OIDC（`oauth2Login`）→ Session(Redis) 橋渡し素振り（Cookie/CSRF/RBAC） | STOMPエコー疎通 + Compose起動（keycloak含む） |
| 4 | フロントから認証付きREST呼び出し確認 | SSO→セッション→認証付きREST通し | CI緑・Compose一括・エコーがブラウザまで |

**基盤疎通ゲート（Day4）**

- Keycloakログイン→セッション発行→認証付きRESTが叩ける
- `docker compose up` で全サービス一括起動
- GitHub Actions CIが緑（backend test / frontend lint・build）
- STOMPエコーがブラウザまで届く

## Day5-9: MVP-A（Sprint 1・F-01〜F-09）

- B=イベント作成 / ルーム状態 / 質問投稿API / いいねAPI（1人1票）/ ステータス変更API / QueryDSL
- A=イベント一覧・詳細（SSR）/ 参加ページ / 質問投稿・いいねUI / 人気順ライブUI / 運営画面
- C=STOMP配信（`QUESTION_*`）+ ハンドシェイクCookie認証 + Compose/CI仕上げ

**Go/No-Go（Day9）**

| 判定 | 条件 | 次のアクション |
|------|------|----------------|
| Go | 主催者ログイン → ルーム作成 → 開始 → 参加者が質問 → いいね → 人気順ライブ → 回答済みマークが E2E で動く | MVP-B（F-10〜）へ |
| No-Go | 上記のいずれか未完了 | MVP-B 捨て、Day14 仕上げへ |

## Day10-13: MVP-B（Sprint 2・F-10〜F-13）

- B=Poll API（作成・開閉・回答・集計）/ フィルタAPI / モデレーション
- A=Poll作成・回答・棒グラフUI / フィルタUI
- C=`POLL_OPENED` / `POLL_UPDATED` / `POLL_CLOSED` 配信
- 余力があれば F-14 / F-15

## Day14: 仕上げ

- E2Eテスト、README/スクリーンショット、相互デモ、バグ修正・UX仕上げ

## 情報交換セッション

- 毎朝15分の進捗共有
- EX（ペア実装）: 学習フェーズで認証 × 環境の通し、MVP-A中盤で質問・いいねの WebSocket × UI 連携をペアで
