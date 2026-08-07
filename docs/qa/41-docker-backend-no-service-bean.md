# Docker backend が起動しない（EventService Bean 無し）

## 症状

`Live-qa-backend` が `Exited (1)`。ログ例:

```text
Parameter 0 of constructor in EventController required a bean of type
'com.liveqa.service.EventService' that could not be found.
```

Postgres / Redis / Keycloak は healthy。Flyway も成功している場合が多い。

## 原因

Docker や DB ではなく **Spring DI**。

- `EventController` はコンストラクタで `EventService` を要求する
- `EventService` に `@Service` が無いとコンポーネントスキャン対象外 → **Bean が作られない**
- 起動時に DI 失敗 → プロセス終了

【EJ】依存性注入 … コンストラクタ注入は「型の Bean がコンテナにある」前提。`@Service` / `@Repository` / `@Component` が公開面（DI 登録）になる。

## 直し方

1. `EventService` に `@Service` を付ける  
2. イメージを作り直す（jar に古いクラスが入っていると再発）:

```bash
docker compose up -d --build backend
```

## 確認

- `docker compose ps backend` → `Up`
- ログに `Started LiveQaApplication`
