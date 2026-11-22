-- Each account has a version, which a change must name (ENG-87), and who made its latest change
-- and when, which a refused change names (FE-81). An account is disabled, never deleted (PLT-45).
ALTER TABLE identity.users
    ADD COLUMN version bigint NOT NULL DEFAULT 0,
    ADD COLUMN is_disabled boolean NOT NULL DEFAULT false,
    ADD COLUMN changed_at timestamptz,
    -- Null until a user changes the account: the first one is created by a command (PLT-23).
    ADD COLUMN changed_by uuid REFERENCES identity.users (id);

UPDATE identity.users SET changed_at = created_at;

ALTER TABLE identity.users
    ALTER COLUMN changed_at SET NOT NULL,
    ALTER COLUMN version DROP DEFAULT,
    ALTER COLUMN is_disabled DROP DEFAULT;
