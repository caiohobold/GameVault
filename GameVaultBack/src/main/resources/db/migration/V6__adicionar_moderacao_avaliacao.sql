-- =====================================================================
-- V6 - Moderacao das avaliacoes pelo Claude
-- As avaliacoes existentes ficam APROVADA. moderada_por_id vazio indica
-- que a decisao foi da IA; preenchido, foi de um administrador.
-- =====================================================================

ALTER TABLE avaliacao
    ADD COLUMN status_moderacao VARCHAR(20)  NOT NULL DEFAULT 'APROVADA',
    ADD COLUMN motivo_moderacao VARCHAR(500),
    ADD COLUMN contem_spoiler   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN moderada_em      TIMESTAMP,
    ADD COLUMN moderada_por_id  BIGINT,
    ADD CONSTRAINT fk_avaliacao_moderada_por FOREIGN KEY (moderada_por_id) REFERENCES usuario (id),
    ADD CONSTRAINT ck_avaliacao_status_moderacao
        CHECK (status_moderacao IN ('APROVADA', 'EM_REVISAO', 'BLOQUEADA'));

COMMENT ON COLUMN avaliacao.status_moderacao IS 'Somente APROVADA aparece na listagem publica e entra na nota media (RN08)';

CREATE INDEX ix_avaliacao_status_moderacao ON avaliacao (status_moderacao);
