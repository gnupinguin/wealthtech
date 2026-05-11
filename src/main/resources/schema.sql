CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS clients (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         email VARCHAR(320) NOT NULL UNIQUE,
                         description TEXT,
                         created_at TIMESTAMPTZ NOT NULL,

                         CONSTRAINT chk_clients_description_length
                             CHECK (description IS NULL OR LENGTH(description) <= 4096)
);

CREATE TABLE IF NOT EXISTS client_social_links (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                                     url VARCHAR(2048) NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS documents (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                           title VARCHAR(255) NOT NULL,
                           content TEXT NOT NULL,
                           summary TEXT,
                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL,

                           CONSTRAINT chk_documents_content_length
                               CHECK (LENGTH(content) <= 1000000),

                           CONSTRAINT chk_documents_summary_length
                               CHECK (summary IS NULL OR LENGTH(summary) <= 4096)
);

CREATE TABLE IF NOT EXISTS document_chunks (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                 chunk_index INT NOT NULL,
                                 content TEXT NOT NULL,
                                 embedding VECTOR(1536) NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL,

                                 CONSTRAINT chk_document_chunks_content_length
                                     CHECK (LENGTH(content) <= 100000),

                                 CONSTRAINT uk_document_chunks_document_index
                                     UNIQUE (document_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS document_enrichment_outbox_events (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                                    type TEXT NOT NULL,
                                                    status TEXT NOT NULL DEFAULT 'PENDING',
                                                    attempts INT NOT NULL DEFAULT 0,
                                                    last_error TEXT,
                                                    available_at TIMESTAMPTZ NOT NULL,
                                                    locked_at TIMESTAMPTZ,
                                                    published_at TIMESTAMPTZ,
                                                    created_at TIMESTAMPTZ NOT NULL,
                                                    updated_at TIMESTAMPTZ NOT NULL,

                                                    CONSTRAINT chk_document_enrichment_outbox_event_type
                                                        CHECK (type IN ('CHUNKING', 'SUMMARY')),

                                                    CONSTRAINT chk_document_enrichment_outbox_event_status
                                                        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED')),

                                                    CONSTRAINT uk_document_enrichment_outbox_event_type
                                                        UNIQUE (document_id, type)
);

CREATE TABLE IF NOT EXISTS document_enrichment_processed_events (
                                                      event_id UUID PRIMARY KEY,
                                                      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                                      type TEXT NOT NULL,
                                                      processed_at TIMESTAMPTZ NOT NULL,

                                                      CONSTRAINT chk_document_enrichment_processed_event_type
                                                          CHECK (type IN ('CHUNKING', 'SUMMARY'))
);

CREATE INDEX IF NOT EXISTS idx_clients_email_trgm
    ON clients USING gin (email gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_clients_first_name_trgm
    ON clients USING gin (first_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_clients_last_name_trgm
    ON clients USING gin (last_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_clients_description_trgm
    ON clients USING gin (description gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_client_social_links_client_id
    ON client_social_links (client_id);

CREATE INDEX IF NOT EXISTS idx_documents_client_id
    ON documents (client_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
    ON document_chunks (document_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding_hnsw
    ON document_chunks
        USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_document_enrichment_outbox_events_publishable
    ON document_enrichment_outbox_events (status, available_at, created_at);

CREATE INDEX IF NOT EXISTS idx_document_enrichment_outbox_events_document_id
    ON document_enrichment_outbox_events (document_id);

CREATE INDEX IF NOT EXISTS idx_document_enrichment_processed_events_document_id
    ON document_enrichment_processed_events (document_id, type);
