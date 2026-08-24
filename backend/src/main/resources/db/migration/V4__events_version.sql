-- events.version: 楽観ロック（名称更新と論理削除の衝突用）
ALTER TABLE events
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
