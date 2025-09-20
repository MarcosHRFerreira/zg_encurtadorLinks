CREATE TABLE short_urls (
    id SERIAL PRIMARY KEY,
    original_url TEXT NOT NULL,
    code VARCHAR(5) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE short_url_accesses (
    id SERIAL PRIMARY KEY,
    short_url_id INTEGER NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
    accessed_at TIMESTAMP NOT NULL,
    user_agent TEXT,
    referer TEXT
);