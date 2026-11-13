-- =====================================================================
-- V9 - Alertas de fraude gerados pela deteccao de anomalias
-- A IA so cria o alerta; quem confirma ou descarta e o administrador.
-- As decisoes viram rotulos para o classificador supervisionado.
-- =====================================================================

CREATE TABLE alerta_fraude (
    id              BIGSERIAL,
    usuario_id      BIGINT           NOT NULL,
    pontuacao       DOUBLE PRECISION NOT NULL,
    motivos         JSONB            NOT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'ABERTO',
    criado_em       TIMESTAMP        NOT NULL,
    revisado_por_id BIGINT,
    revisado_em     TIMESTAMP,
    CONSTRAINT pk_alerta_fraude              PRIMARY KEY (id),
    CONSTRAINT fk_alerta_fraude_usuario      FOREIGN KEY (usuario_id)      REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerta_fraude_revisado_por FOREIGN KEY (revisado_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_alerta_fraude_status       CHECK (status IN ('ABERTO', 'CONFIRMADO', 'DESCARTADO')),
    CONSTRAINT ck_alerta_fraude_pontuacao    CHECK (pontuacao >= 0 AND pontuacao <= 1)
);

COMMENT ON COLUMN alerta_fraude.pontuacao IS 'Grau de anomalia de 0 a 1 (quanto maior, mais suspeito)';

CREATE INDEX ix_alerta_fraude_status  ON alerta_fraude (status);
CREATE INDEX ix_alerta_fraude_usuario ON alerta_fraude (usuario_id);
