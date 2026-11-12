ALTER TABLE request
    ADD COLUMN browser      TEXT,
    ADD COLUMN user_agent   TEXT,
    ADD COLUMN endpoint     TEXT,
    ADD COLUMN filter_field TEXT,
    ADD COLUMN filter_op    TEXT,
    ADD COLUMN filter_value INTEGER,
    ADD COLUMN sort_field   TEXT,
    ADD COLUMN sort_desc    BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN preset       TEXT,
    ADD COLUMN result_count INTEGER,
    ADD COLUMN duration_ms  BIGINT;
