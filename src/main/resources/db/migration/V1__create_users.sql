-- Initial schema for user-service.

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         CITEXT      UNIQUE NOT NULL,
    password_hash TEXT,                                       -- nullable for OAuth-only users
    google_sub    TEXT        UNIQUE,                         -- Google subject ID
    plan          TEXT        NOT NULL DEFAULT 'free',        -- 'free' | 'premium'
    status        TEXT        NOT NULL DEFAULT 'active',      -- 'active' | 'suspended' | 'deleted'
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE usernames (
    username   CITEXT      PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
