CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE public.documents (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    title VARCHAR(200) NOT NULL,
    author_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,

    type VARCHAR(15) NOT NULL,
    format VARCHAR(15) NOT NULL,
    status VARCHAR(15) NOT NULL,
    processing_generation INTEGER NOT NULL DEFAULT 1,

    original_file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    file_checksum VARCHAR(255) NOT NULL,

  CONSTRAINT uk_document_checksum UNIQUE (file_checksum),
  CONSTRAINT uk_document_object_key UNIQUE (object_key)
);

CREATE INDEX idx_documents_topic ON public.documents (topic_id);
CREATE INDEX idx_documents_author ON public.documents (author_id);
CREATE INDEX idx_documents_status ON public.documents (status);


CREATE TABLE public.document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(1024),

    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id)
            REFERENCES public.documents (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_chunks_document ON public.document_chunks (document_id);
CREATE INDEX idx_chunks_doc_chunk ON public.document_chunks (document_id, chunk_index);


ALTER TABLE public.document_chunks
    ADD COLUMN content_tsv tsvector
        GENERATED ALWAYS AS (
            to_tsvector('spanish', coalesce(content, ''))
            ) STORED;

CREATE INDEX idx_chunks_content_tsv
    ON public.document_chunks
    USING GIN (content_tsv);


CREATE TABLE public.document_targets (
    document_id BIGINT NOT NULL,
    education_level VARCHAR(50) NOT NULL,
    grade_level VARCHAR(50) NOT NULL,
    course_id BIGINT NOT NULL,

    CONSTRAINT pk_document_targets
     PRIMARY KEY (document_id, education_level, grade_level, course_id),

    CONSTRAINT fk_document_targets_document
     FOREIGN KEY (document_id)
         REFERENCES public.documents (id)
         ON DELETE CASCADE
);

CREATE INDEX idx_targets_lookup
    ON public.document_targets (education_level, grade_level, course_id);
