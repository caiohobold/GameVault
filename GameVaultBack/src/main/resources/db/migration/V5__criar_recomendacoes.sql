-- =====================================================================
-- V5 - Recomendacao de jogos
-- recomendacao_usuario e jogo_similar sao reescritas a cada treino do
-- modelo (filtragem colaborativa no GameVaultML).
-- visualizacao_jogo alimenta a recomendacao de quem ainda nao comprou nada.
-- =====================================================================

CREATE TABLE recomendacao_usuario (
    id             BIGSERIAL,
    usuario_id     BIGINT           NOT NULL,
    jogo_id        BIGINT           NOT NULL,
    pontuacao      DOUBLE PRECISION NOT NULL,
    motivo_jogo_id BIGINT,
    gerada_em      TIMESTAMP        NOT NULL,
    CONSTRAINT pk_recomendacao_usuario        PRIMARY KEY (id),
    CONSTRAINT fk_recomendacao_usuario_usuario FOREIGN KEY (usuario_id)     REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_recomendacao_usuario_jogo    FOREIGN KEY (jogo_id)        REFERENCES jogo (id)    ON DELETE CASCADE,
    CONSTRAINT fk_recomendacao_usuario_motivo  FOREIGN KEY (motivo_jogo_id) REFERENCES jogo (id)    ON DELETE SET NULL,
    CONSTRAINT uk_recomendacao_usuario_jogo    UNIQUE (usuario_id, jogo_id)
);

COMMENT ON COLUMN recomendacao_usuario.motivo_jogo_id IS 'Jogo da biblioteca do usuario mais parecido com o recomendado ("porque voce jogou X")';

CREATE TABLE jogo_similar (
    id         BIGSERIAL,
    jogo_id    BIGINT           NOT NULL,
    similar_id BIGINT           NOT NULL,
    pontuacao  DOUBLE PRECISION NOT NULL,
    CONSTRAINT pk_jogo_similar         PRIMARY KEY (id),
    CONSTRAINT fk_jogo_similar_jogo    FOREIGN KEY (jogo_id)    REFERENCES jogo (id) ON DELETE CASCADE,
    CONSTRAINT fk_jogo_similar_similar FOREIGN KEY (similar_id) REFERENCES jogo (id) ON DELETE CASCADE,
    CONSTRAINT uk_jogo_similar         UNIQUE (jogo_id, similar_id)
);

CREATE TABLE visualizacao_jogo (
    id             BIGSERIAL,
    usuario_id     BIGINT    NOT NULL,
    jogo_id        BIGINT    NOT NULL,
    visualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT pk_visualizacao_jogo              PRIMARY KEY (id),
    CONSTRAINT fk_visualizacao_jogo_usuario      FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_visualizacao_jogo_jogo         FOREIGN KEY (jogo_id)    REFERENCES jogo (id)    ON DELETE CASCADE,
    CONSTRAINT uk_visualizacao_jogo_usuario_jogo UNIQUE (usuario_id, jogo_id)
);

COMMENT ON TABLE visualizacao_jogo IS 'Ultima vez que um usuario logado abriu a pagina de um jogo';
