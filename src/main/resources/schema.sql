CREATE TABLE IF NOT EXISTS clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name  TEXT NOT NULL,
    last_name   TEXT NOT NULL,
    email       TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS client_social_links (
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    position  INT  NOT NULL,
    value     TEXT NOT NULL,
    PRIMARY KEY (client_id, position)
);

CREATE TABLE IF NOT EXISTS documents (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID        NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    title      TEXT        NOT NULL,
    content    TEXT        NOT NULL,
    summary    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
