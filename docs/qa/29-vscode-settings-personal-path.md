# backend/.vscode/settings.json の個人パス指摘

## 質問

レビューで「`backend/.vscode/settings.json` の個人環境依存のパス変更を取り除き、目的に関係する変更だけを残してください」と指摘された。解決策は？

## 指摘の意味

| 種類 | 例 | リポジトリに載せる？ |
|------|-----|----------------------|
| **目的に関係** | Maven 自動取込・ソース DL など、チーム共通のエディタ挙動 | ○ 残してよい |
| **個人環境依存** | `/home/自分のユーザー/.sdkman/...` のような **絶対パス** | ✕ 取り除く |

今のファイルだと:

- 残してよい: `updateBuildConfiguration` / `java.import.maven.enabled` / `java.maven.downloadSources`
- 取り除く: `java.jdt.ls.java.home` と `java.configuration.runtimes` の **自分の SDKMAN パス**

他人の PC・CI・別 OS ではそのパスが存在せず、共有設定として破綻する。

## Q: 1 以外（個人パス）を削除してよいか

**リポジトリの `settings.json` からは削除してよい（推奨）**。指摘どおり「目的の3キーだけ残す」。

- 自分の Cursor で Java が壊れるときだけ、同じキーを **User 設定**へ移す（手順2）。拡張が `JAVA_HOME` / SDKMAN を拾えるなら User にも不要
- ファイルごと消すのは、チームで Maven 自動取込を共有したいなら非推奨（目的の3行まで残す）

## 解決策（手順）

1. **コミットする `backend/.vscode/settings.json` を目的だけにする**

```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.import.maven.enabled": true,
    "java.maven.downloadSources": true
}
```

2. **自分用の Java パスは User 設定へ移す**（リポジトリ外）  
   Cursor/VS Code → Settings (JSON) ユーザー側、例:

```json
{
    "java.jdt.ls.java.home": "/home/spiritualmasa/.sdkman/candidates/java/21.0.10-tem",
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-21",
            "path": "/home/spiritualmasa/.sdkman/candidates/java/21.0.10-tem",
            "default": true
        }
    ]
}
```

3. （任意）チームは `JAVA_HOME` / SDKMAN / 拡張の自動検出に任せ、ワークスペースにパスを書かない方針でもよい。

## データの流れ（設定の優先）

```text
User settings（個人・絶対パス可）
  ⊃ Workspace .vscode/settings.json（リポジトリ共有・環境非依存のみ）
    → Java 拡張が言語サーバ起動時に参照
```

ワークスペースに個人パスを書くと、clone した全員に自分のホームが強制される。

## 型の正体（設定キー）

| キー | 正体 |
|------|------|
| `java.jdt.ls.java.home` | 言語サーバが使う JDK の **マシン上の場所**（個人） |
| `java.configuration.runtimes` | プロジェクトが想定する Java 世代と path（path が個人） |
| `java.import.maven.enabled` 等 | 拡張の挙動フラグ（環境非依存＝目的） |
