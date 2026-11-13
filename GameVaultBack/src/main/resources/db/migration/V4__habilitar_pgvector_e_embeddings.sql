-- =====================================================================
-- V4 - Embeddings dos jogos (busca em linguagem natural)
-- Requer a extensao pgvector. No docker-compose a imagem
-- pgvector/pgvector:pg16 ja traz a extensao e o usuario do banco e
-- superusuario. Em instalacoes locais, um superusuario precisa rodar
-- "CREATE EXTENSION vector;" no banco antes desta migration.
--
-- A tabela nao e mapeada como entidade JPA: o Hibernate nao conhece o
-- tipo vector. O acesso e feito por JdbcTemplate (JogoEmbeddingRepository).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE jogo_embedding (
    jogo_id    BIGINT       NOT NULL,
    embedding  vector(384)  NOT NULL,
    texto_hash VARCHAR(64)  NOT NULL,
    gerado_em  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_jogo_embedding      PRIMARY KEY (jogo_id),
    CONSTRAINT fk_jogo_embedding_jogo FOREIGN KEY (jogo_id) REFERENCES jogo (id) ON DELETE CASCADE
);

COMMENT ON TABLE  jogo_embedding            IS 'Vetor de significado (384 dimensoes) do titulo, descricao e categorias do jogo';
COMMENT ON COLUMN jogo_embedding.texto_hash IS 'SHA-256 do texto usado; evita recalcular quando nada mudou';

CREATE INDEX ix_jogo_embedding_hnsw ON jogo_embedding USING hnsw (embedding vector_cosine_ops);
