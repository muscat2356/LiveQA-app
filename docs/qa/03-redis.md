# Redis（使い方・キー・Insight）

## このラボでの Redis の役割

Redis は **「ログイン状態（セッション）の置き場」**。  
ブラウザが持つのは ID だけ（`SESSION` Cookie）。中身は Redis。

```text
ブラウザ          Spring Boot              Redis
  SESSION=abc  →  復元（IDで探す） →  spring:session:sessions:abc
                 ← 属性を読み戻す ←
                 変更があれば格納 →  同じキーを更新
```

DB（PostgreSQL）とは別。ラボでは **一時的なログイン状態** を Redis に載せる。

### なぜ Redis か

| 理由 | 意味 |
|------|------|
| 速い | メモリ上の KV。セッションの読み書き向き |
| プロセスと分離 | アプリ再起動しても Redis が生きていれば残せる |
| 本番と同じ | 本番も Spring Session + Redis |

---

## キーの見え方

実際に見えた例:

```text
spring:session:sessions:208fbae6-83eb-4678-b72c-b59dc0917b21
spring:session:sessions:6bd77c7f-180e-411a-a7fa-45fb7821d0ec
```

| 観察 | 意味 |
|------|------|
| `spring:session:sessions:{UUID}` | **格納**成功。セッション本体が Redis にある |
| UUID 部分 | Cookie `SESSION` の値（セッション ID）と対応 |
| キーが2つ | アクセス等でセッションが2回作られた |

```text
spring:session:sessions:{UUID}
     │         │              └─ セッション ID
     │         └─ 「セッション本体」用
     └─ namespace（接頭辞）
```

| 操作 | Redis 上の意味 |
|------|----------------|
| **格納** | このキーに属性を書き込む |
| **復元** | Cookie の ID でこのキーを読む |
| **削除** | ログアウト等でキーを消す（または TTL 切れ） |

---

## なぜ `session-lab*` が空で `spring:session:*` に出る？

`application.yml` には:

```yaml
spring.session.redis.namespace: session-lab
```

とあるが、`@EnableRedisHttpSession` を付けると、アノテーション側のデフォルト namespace **`spring:session`** が使われ、yml の `session-lab` が効いていないことが多い。

| 名前 | 正体 | いま起きていること |
|------|------|-------------------|
| `@EnableRedisHttpSession` | 設定用アノテーション | namespace 既定が `spring:session` |
| `spring.session.redis.namespace` | yml | アノテーション未使用の自動設定向け。上書きされがち |

動かないバグではなく、**キーの接頭辞が想定と違うだけ**。  
`KEYS 'session-lab*'` が空だった理由はこれ。

揃えたいとき:

```java
@EnableRedisHttpSession(redisNamespace = "session-lab")
```

---

## `(empty array)` の意味

**Redis には繋がっているが、該当パターンのキーがまだ1つもない**（または別 namespaceを見ている）。

### 起動直後に空な理由

Spring Session は **セッションが必要になったリクエストのあと** に Redis へ書く。  
アプリ起動だけ・誰も HTTP を叩いていないとキーは増えない。

### キーを出す手順

1. アプリが動いていること  
2. ブラウザで `http://localhost:8080/` を開く  
3. Cookie に `SESSION` があるか確認  
4. `KEYS spring:session:*` または `KEYS *`

---

## よく使う `redis-cli`

```bash
docker exec -it $(docker compose -f docker/docker-compose.yml ps -q redis) redis-cli
```

| コマンド | 何をするか |
|----------|------------|
| `PING` | 疎通。`PONG` なら OK |
| `KEYS *` | 全キー一覧（学習用） |
| `KEYS spring:session:*` | セッション関連 |
| `TYPE …` | 型（だいたい hash） |
| `HGETALL …` | 中身を全部見る |
| `TTL …` | 残り秒数 |
| `DEL …` | そのセッションを消す |
| `FLUSHDB` | 今の DB 全消し（注意） |

`HGETALL` がバイナリっぽく見えても正常なことが多い。

### Spring から見た流れ

```text
① リクエスト（Cookie: SESSION=abc）
② Redis から復元
③ コントローラ等が使う
④ 変更があれば格納
⑤ 必要なら Set-Cookie で発行
```

`SessionConfig` の仕事は Redis の中身ではなく、**Cookie の発行の作法**。  
接続先は `spring.data.redis.host` / `port`。

---

## Redis に管理画面はない？

Redis 本体にはブラウザ管理画面は無い。**別ツール**で GUI を使う。

| ツール | 特徴 |
|--------|------|
| **Redis Insight**（公式寄り） | キー一覧・中身・TTL。学習向き |
| Another Redis Desktop Manager | 軽めのデスクトップ GUI |
| redis-commander 等 | Docker で上げる Web UI |

| | Keycloak | Redis |
|--|----------|-------|
| 管理画面 | あり（`:8081`） | 本体には無し |
| 中身の見方 | ブラウザ | CLI または Insight 等 |

必須ではない。`redis-cli` だけでラボは進められる。

---

## Redis Insight は Windows に入れる？

**はい。Windows に Redis Insight を入れる**理解でよい。

```text
Windows
  ├ Docker Desktop        … Redis コンテナを動かす
  └ Redis Insight         … その Redis を見る GUI（別アプリ）

WSL（Ubuntu）
  └ Spring Boot / mvn     … localhost:6379 経由で同じ Redis を使う
```

接続: Host `127.0.0.1` / Port `6379` / パスワードなし（ラボ最小 Compose）。  
WSL 側に入れる必要は基本ない。
