-- events.question_count: 質問数スナップショット（詳細 API 用。一覧には出さない）
ALTER TABLE events
    ADD COLUMN question_count INT NOT NULL DEFAULT 0;

ALTER TABLE events
    ADD CONSTRAINT ck_events_question_count CHECK (question_count >= 0);
