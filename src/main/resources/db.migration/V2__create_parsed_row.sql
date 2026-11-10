CREATE TABLE story (
                         id            BIGINT PRIMARY KEY,
                         hn_rank       INTEGER,
                         title         TEXT NOT NULL,
                         points        INTEGER,
                         commentCount  INTEGER,
                         first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);