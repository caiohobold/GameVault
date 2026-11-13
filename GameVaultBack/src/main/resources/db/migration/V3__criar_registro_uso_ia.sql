-- =====================================================================
-- V3 - Infraestrutura dos modulos de IA
-- ia_uso: cada chamada ao Claude, para acompanhar tokens, custo e falhas.
-- metrica_modelo_ia: metricas de avaliacao dos modelos treinados no
-- GameVaultML (hit rate da recomendacao, MAE da demanda etc.).
-- =====================================================================

CREATE TABLE ia_uso (
    id                   BIGSERIAL,
    modulo               VARCHAR(40)  NOT NULL,
    modelo               VARCHAR(60)  NOT NULL,
    usuario_id           BIGINT,
    tokens_entrada       BIGINT       NOT NULL DEFAULT 0,
    tokens_saida         BIGINT       NOT NULL DEFAULT 0,
    tokens_cache_leitura BIGINT       NOT NULL DEFAULT 0,
    duracao_ms           BIGINT       NOT NULL DEFAULT 0,
    sucesso              BOOLEAN      NOT NULL,
    erro                 VARCHAR(500),
    criado_em            TIMESTAMP    NOT NULL,
    CONSTRAINT pk_ia_uso         PRIMARY KEY (id),
    CONSTRAINT fk_ia_uso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL
);

COMMENT ON TABLE ia_uso IS 'Registro de cada chamada a API do Claude (tokens, duracao e falhas)';

CREATE INDEX ix_ia_uso_modulo_data ON ia_uso (modulo, criado_em);

CREATE TABLE metrica_modelo_ia (
    id           BIGSERIAL,
    modelo       VARCHAR(40)      NOT NULL,
    metrica      VARCHAR(40)      NOT NULL,
    valor        DOUBLE PRECISION NOT NULL,
    amostras     INTEGER          NOT NULL DEFAULT 0,
    calculada_em TIMESTAMP        NOT NULL,
    CONSTRAINT pk_metrica_modelo_ia PRIMARY KEY (id)
);

COMMENT ON TABLE metrica_modelo_ia IS 'Metricas de avaliacao dos modelos de aprendizado de maquina a cada treino';

CREATE INDEX ix_metrica_modelo_ia ON metrica_modelo_ia (modelo, metrica, calculada_em);
