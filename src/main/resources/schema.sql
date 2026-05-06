CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE clients (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         first_name TEXT NOT NULL,
                         last_name TEXT NOT NULL,
                         email TEXT NOT NULL UNIQUE,
                         description TEXT,
                         created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE client_social_links (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                                     url TEXT NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE documents (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
                           title TEXT NOT NULL,
                           content TEXT NOT NULL,
                           summary TEXT,
                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE document_chunks (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                 chunk_index INT NOT NULL,
                                 content TEXT NOT NULL,
                                 embedding VECTOR(1563) NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL,

                                 CONSTRAINT uk_document_chunks_document_index
                                     UNIQUE (document_id, chunk_index)
);

CREATE TABLE document_enrichment_jobs (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                          type TEXT NOT NULL,
                                          status TEXT NOT NULL DEFAULT 'PENDING',
                                          attempts INT NOT NULL DEFAULT 0,
                                          max_attempts INT NOT NULL DEFAULT 3,
                                          last_error TEXT,
                                          available_at TIMESTAMPTZ NOT NULL,
                                          locked_at TIMESTAMPTZ,
                                          completed_at TIMESTAMPTZ,
                                          created_at TIMESTAMPTZ NOT NULL,
                                          updated_at TIMESTAMPTZ NOT NULL,

                                          CONSTRAINT chk_document_enrichment_job_type
                                              CHECK (type IN ('CHUNKING', 'SUMMARY')),

                                          CONSTRAINT chk_document_enrichment_job_status
                                              CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

                                          CONSTRAINT uk_document_enrichment_job_type
                                              UNIQUE (document_id, type)
);

CREATE INDEX idx_clients_email_trgm
    ON clients USING gin (email gin_trgm_ops);

CREATE INDEX idx_clients_first_name_trgm
    ON clients USING gin (first_name gin_trgm_ops);

CREATE INDEX idx_clients_last_name_trgm
    ON clients USING gin (last_name gin_trgm_ops);

CREATE INDEX idx_clients_description_trgm
    ON clients USING gin (description gin_trgm_ops);

CREATE INDEX idx_client_social_links_client_id
    ON client_social_links (client_id);

CREATE INDEX idx_documents_client_id
    ON documents (client_id);

CREATE INDEX idx_document_chunks_document_id
    ON document_chunks (document_id);

CREATE INDEX idx_document_chunks_embedding_hnsw
    ON document_chunks
        USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_document_enrichment_jobs_pending
    ON document_enrichment_jobs (status, available_at, created_at);

CREATE INDEX idx_document_enrichment_jobs_document_id
    ON document_enrichment_jobs (document_id);
