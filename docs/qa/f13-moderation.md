# F-13 簡易モデレーション

scope: 不適切質問を隠す。**新 API は作らない。**  
正: [f08-status.md](f08-status.md) の `ARCHIVED` ＋ [f12-filter.md](f12-filter.md) の既定（ARCHIVED 以外）。 Issue: [#23](https://github.com/muscat2356/live-qa-app/issues/23)。

## 到達点（2026-08-26）

| 項目 | 状態 |
|------|------|
| `PATCH .../questions/{qid}/status` → `ARCHIVED` | **F-08 済**（HOST 所有者。401 / 403 / 404 は F-08 のまま） |
| 参加者一覧から既定で隠す | **F-12 済**（未指定は ARCHIVED 以外） |
| `GET .../questions?status=ARCHIVED` を所有者だけに | **済み**（ゲスト・他 HOST は 403） |

## なぜ新パスを作らないか

隠す操作は「ステータスを `ARCHIVED` にする」こと。確認は「一覧で `status=ARCHIVED` を見る」こと。どちらも既存 API。専用の hide / unhide を足すと公開面が増える。

```text
【EJ】公開面最小化 … パスを増やさない。PATCH は F-08、一覧は F-12
```

## 契約（見え方）

一覧 GET は `permitAll` のまま（未指定 / `OPEN` / `ANSWERED` は誰でも 200）。特権なのは **ARCHIVED 行を見る** ところだけ。

| 見る人 | 未指定 / OPEN / ANSWERED | `status=ARCHIVED` |
|--------|--------------------------|-------------------|
| ゲスト / 他 HOST | 200（ARCHIVED は出ない） | **403** |
| 所有者 HOST | 200 | 200 |

- 403 の本文はゲストも他 HOST も同じ: `"この操作は主催者のみ可能です。"`
- OIDC はあるが `users` 行が無いときも **403**（`requireByOidcSubject` の 404 は使わない。公開 GET の特権クエリをユーザー存在で漏らさない）
- 401 にはしない。一覧は公開 GET。ゲストは Security を通る。Service が拒否する
- イベントが無いときは **404**（ARCHIVED 指定でも先に `requireVisibleEvent`）
- 一覧全体に `@PreAuthorize("hasRole('HOST')")` は付けない（公開 GET が死ぬ）

PATCH の HOST ガード（401 / 非 HOST 403 / CSRF 403 / 非所有者 404）は **F-08 のまま。足さない。**

## なぜ / 役割 / 流れ

**なぜ:** F-12 の `?status=ARCHIVED` は公開 GET なので、知っていればゲストも隠した質問を覗ける。モデレーション確認は所有者だけにする。

**役割:** Controller は `oidc` を渡すだけ。Service が「ARCHIVED を見てよいか」を検証してから QueryDSL に渡す。

```text
GET /api/events/10/questions?status=ARCHIVED
  Cookie SESSION（HOST なら SecurityContext に OidcUser。ゲストなら null）
  → Controller は文字列と oidc を渡す
  → requireVisibleEvent（無ければ 404）
  → QuestionStatus.form → Optional.of(ARCHIVED)
  → requireOwnedEventForArchived
       oidc が null → 403
       oidc.sub → users 行（無ければ 403。require の 404 は使わない）
       findByIdAndHost_Id（0件 → 403）
  → searchByEventId（status = ARCHIVED）
  → votedByMe は今のセッション（HOST なら u:{id}）
```

Cookie / Redis: 一覧 GET 自体は Session が無くても通る。`status=ARCHIVED` のときだけ SecurityContext の OidcUser を読む。DB は `events.id + host_id`。`EventService.requireOwnedEvent`（非所有者 404）は使わない。公開 GET の特権クエリなので **403**。

## 型の正体

| 名前 | 正体 |
|------|------|
| `OidcUser` | SecurityContext に載っているログイン済みの契約。未ログインは `null` |
| `Optional<QuestionStatus>` | 未指定は empty。`orElse(null)` はしない |
| `findByIdAndHost_Id` | 派生クエリ。所有者行だけ復元 |
| `BusinessException` + `FORBIDDEN` | 業務の 403。`GlobalExceptionHandler.businessHandler` が JSON にする |

`questionStatus.isPresent() && questionStatus.get() == ARCHIVED`。empty を `null` の enum にしない。

```text
【EJ】Optional … 未指定は empty。null の enum を Repository に渡さない
【EJ】公開面最小化 … ガードは Service。一覧 GET に @PreAuthorize を付けない
【EJ】例外 … ゲスト / users行なし / 他 HOST は同じ 403。存在漏洩はイベント無しの 404 だけ
【EJ】あえて外す … EventService.requireOwnedEvent は 404。ここは公開 GET なので 403
【EJ】依存性注入 … 既存の UserService / EventRepository。新しい Bean は増やさない
```
