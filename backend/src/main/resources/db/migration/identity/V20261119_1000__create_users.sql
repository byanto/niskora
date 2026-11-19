-- The identity module's own schema (ENG-19, ENG-60) and its users (PLT-17, PLT-19).
CREATE SCHEMA identity;

CREATE TABLE identity.users (
    id uuid PRIMARY KEY,
    login_name text NOT NULL,
    full_name text NOT NULL,
    -- The Argon2 hash, prefixed with the encoder's id, such as {argon2}$argon2id$...
    password_hash text NOT NULL,
    created_at timestamptz NOT NULL
);

-- A login name is unique whatever its case, and is looked up the same way.
CREATE UNIQUE INDEX users_login_name_key ON identity.users (lower(login_name));
