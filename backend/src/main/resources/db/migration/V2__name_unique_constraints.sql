-- V1 の列レベル UNIQUE が付けた自動名を、Entity / docs と同じ名前に揃える
-- （PostgreSQL のデフォルト: {table}_{column}_key）
ALTER TABLE users RENAME CONSTRAINT users_oidc_subject_key TO uq_users_oidc_subject;
ALTER TABLE events RENAME CONSTRAINT events_join_code_key TO uq_events_join_code;
