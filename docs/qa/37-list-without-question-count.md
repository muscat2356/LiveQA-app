# 一覧に questionCount は必要か

## 決定

**不要（画面・一覧 API とも）。** 件数は **詳細のみ**、かつ DB スナップショット列 `events.question_count`。

詳細は [36-question-count-jpa.md](36-question-count-jpa.md)。
