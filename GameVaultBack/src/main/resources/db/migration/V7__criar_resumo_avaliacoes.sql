-- =====================================================================
-- V7 - Resumo das avaliacoes gerado pelo Claude
-- Um resumo por jogo, refeito quando o jogo acumula novas avaliacoes.
-- =====================================================================

CREATE TABLE resumo_avaliacao (
    id                     BIGSERIAL,
    jogo_id                BIGINT    NOT NULL,
    resumo                 TEXT      NOT NULL,
    pontos_positivos       JSONB     NOT NULL,
    pontos_negativos       JSONB     NOT NULL,
    aspectos               JSONB     NOT NULL,
    avaliacoes_divergentes JSONB     NOT NULL,
    total_avaliacoes_base  INTEGER   NOT NULL,
    gerado_em              TIMESTAMP NOT NULL,
    CONSTRAINT pk_resumo_avaliacao      PRIMARY KEY (id),
    CONSTRAINT fk_resumo_avaliacao_jogo FOREIGN KEY (jogo_id) REFERENCES jogo (id) ON DELETE CASCADE,
    CONSTRAINT uk_resumo_avaliacao_jogo UNIQUE (jogo_id)
);

COMMENT ON COLUMN resumo_avaliacao.total_avaliacoes_base IS 'Quantas avaliacoes aprovadas o jogo tinha quando o resumo foi gerado';
