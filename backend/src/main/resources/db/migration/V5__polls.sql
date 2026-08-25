-- F-10: ポール（アンケート）本体・選択肢・回答
-- 回答 API は F-11。テーブルだけ先に作る
-- UNIQUE 名は Entity の @UniqueConstraint と揃える

CREATE TABLE polls (
    id            BIGSERIAL PRIMARY KEY,
    event_id      BIGINT      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    question_text TEXT        NOT NULL,
    type          VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_polls_type CHECK (type IN ('SINGLE', 'MULTI')),
    CONSTRAINT ck_polls_status CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED'))
);
CREATE INDEX idx_polls_event_id ON polls(event_id);

CREATE TABLE poll_options (
    id            BIGSERIAL PRIMARY KEY,
    poll_id       BIGINT       NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    label         VARCHAR(200) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT ck_poll_options_order CHECK (display_order >= 0),
    CONSTRAINT uq_poll_options_id_poll UNIQUE (id, poll_id)
);
CREATE INDEX idx_poll_options_poll_id ON poll_options(poll_id);

CREATE TABLE poll_responses (
    id             BIGSERIAL PRIMARY KEY,
    poll_id        BIGINT      NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    poll_option_id BIGINT      NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    voter_key      VARCHAR(64) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_poll_responses_option_belongs
        FOREIGN KEY (poll_option_id, poll_id)
        REFERENCES poll_options(id, poll_id) ON DELETE CASCADE,
    CONSTRAINT uq_poll_responses_option_voter UNIQUE (poll_option_id, voter_key)
);
CREATE INDEX idx_poll_responses_poll_id ON poll_responses(poll_id);
CREATE INDEX idx_poll_responses_option_id ON poll_responses(poll_option_id);
