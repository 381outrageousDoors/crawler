CREATE TABLE story (
                         id            BIGINT PRIMARY KEY,
                         hn_rank       INTEGER,
                         title         TEXT NOT NULL,
                         points        INTEGER,
                         commentCount  INTEGER,
                         first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE scan_state (
                            id               INT PRIMARY KEY CHECK (id = 1),
                            last_scan_at     TIMESTAMPTZ,
                            next_scan_at     TIMESTAMPTZ,
                            interval_seconds INT NOT NULL DEFAULT 30
);
INSERT INTO scan_state (id) VALUES (1);