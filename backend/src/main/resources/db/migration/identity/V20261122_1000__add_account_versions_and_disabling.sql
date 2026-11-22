-- Each account has a version, which a change must name (ENG-87), and who made its latest change
-- and when, which a refused change names (FE-81). An account is disabled, never deleted (PLT-45).
--
-- Every new column keeps a default, so the previous release, which does not know them, can still
-- add a user after this migration (ENG-62). The defaults go in a later release.
ALTER TABLE identity.users
    ADD COLUMN version bigint NOT NULL DEFAULT 0,
    ADD COLUMN is_disabled boolean NOT NULL DEFAULT false,
    ADD COLUMN changed_at timestamptz NOT NULL DEFAULT now(),
    -- Null until a user changes the account: the first one is created by a command (PLT-23).
    ADD COLUMN changed_by uuid REFERENCES identity.users (id);

-- An account nobody has changed was last changed when it was created.
UPDATE identity.users SET changed_at = created_at;
