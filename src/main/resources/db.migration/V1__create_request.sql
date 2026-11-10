CREATE TABLE request (
                               id           BIGSERIAL PRIMARY KEY,
                               requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               filter        TEXT
);