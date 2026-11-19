-- The common module's own schema (ENG-19, ENG-60), for what no business module owns.
CREATE SCHEMA common;

-- The Idempotency-Key of each POST (ENG-85, ENG-86), per user, with what the first request
-- answered. A key is claimed by inserting it, so the primary key decides which request runs.
CREATE TABLE common.idempotency_keys (
    -- The login name of the user who sent it, so one user's key never answers another's request.
    owner text NOT NULL,
    key uuid NOT NULL,
    -- SHA-256 of the method, the path with its query and the body.
    request_hash bytea NOT NULL,
    claimed_at timestamptz NOT NULL,
    -- All three are null while the first request is still running.
    response_status integer,
    -- The headers a repeat gets again, such as Content-Type and Location, by name.
    response_headers jsonb,
    response_body bytea,
    PRIMARY KEY (owner, key)
);

-- Keys are kept for 24 hours, and the expired ones are found by when they were claimed.
CREATE INDEX idempotency_keys_claimed_at ON common.idempotency_keys (claimed_at);
