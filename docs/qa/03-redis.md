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

## Q: `session.setAttribute` で Redis に入る理由は？（`addAttribute` ではない）

### 結論

`HttpSession#setAttribute` 自体は Redis API を呼ばない。  
**Spring Session** が「このアプリの HttpSession の実体は Redis」と差し替えているため、属性変更がリクエスト終了時に Redis へ **格納**される。

（Servlet に `addAttribute` は無い。guest では `setAttribute`。）

### なぜ作っているか（このリポ）

`@EnableRedisHttpSession` + Redis 接続があるので、メモリ上のだけのセッションではなく、プロセス再起動や複数インスタンスでも同じ箱を共有できる。

### データの流れ（guest の例）

```text
POST /api/auth/guest
  → SessionRepositoryFilter
       SESSION Cookie があれば Redis から HttpSession を 復元
       無ければ新規セッション（後で Cookie 発行）
  → GuestSessionController
       session.setAttribute("guestId", …)   // メモリ上の Session に書く
  → 応答前〜リクエスト終了
       SessionRepositoryFilter が変更を検知
       → Redis の live-qa:sessions:{id} へ 格納（シリアライズ）
       → 必要なら Set-Cookie: SESSION=…
```

| 操作語 | 誰 | 何 |
|--------|-----|-----|
| `setAttribute` | アプリ（Controller 等） | セッション箱に属性を載せる（API は普通の HttpSession） |
| 格納 | Spring Session Filter | 箱の中身を Redis に書く |
| 復元 | 同上（次リクエスト） | Cookie の鍵で Redis から読み戻す |

### 型の正体

| 名前 | 正体 |
|------|------|
| `HttpSession` | インタフェース（Servlet）。アプリが触る面 |
| `@EnableRedisHttpSession` | アノテーション。実装を Redis 版に切り替えるスイッチ |
| `SessionRepositoryFilter` | 具象 Filter。復元／格納の実行役 |
| `SESSION` Cookie | 箱の**鍵**（中身の guestId そのものではない） |

Security の `Authentication` も同じ仕組みで Session 属性として Redis に載る（ログイン成功時）。guest の `guestId` も同じ経路。

### 【EJ】

```text
【EJ】公開面最小化 … Controller は setAttribute まで。Redis 直書きはしない
【EJ】依存性注入／設定 … SessionConfig で永続先を決める
```

### 一文

> `setAttribute` は「箱に書く」。箱の置き場が Redis なのは Spring Session の Filter がリクエストの前後で復元／格納するから。

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
