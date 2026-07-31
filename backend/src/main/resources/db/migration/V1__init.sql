CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    oidc_subject VARCHAR(255) NOT NULL UNIQUE,
    role         VARCHAR(20)  NOT NULL DEFAULT 'HOST',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id         BIGSERIAL PRIMARY KEY,
    host_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    join_code  VARCHAR(12)  NOT NULL UNIQUE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_host_id ON events(host_id);

CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    body         TEXT        NOT NULL,
    display_name VARCHAR(100),
    upvote_count INT         NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_questions_event_status ON questions(event_id, status);
CREATE INDEX idx_questions_event_popular ON questions(event_id, upvote_count DESC, created_at DESC);

CREATE TABLE votes (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT      NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    voter_key   VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_votes_question_voter UNIQUE (question_id, voter_key)
);
CREATE INDEX idx_votes_question_id ON votes(question_id);
