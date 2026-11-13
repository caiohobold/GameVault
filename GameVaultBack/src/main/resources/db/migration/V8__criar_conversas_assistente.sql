-- =====================================================================
-- V8 - Chat da loja (agente com ferramentas)
-- O conteudo de cada mensagem e guardado exatamente como a API do Claude
-- devolveu (blocos de texto, raciocinio e chamadas de ferramenta) e
-- reenviado sem alteracao: o historico so recebe acrescimos.
-- =====================================================================

CREATE TABLE conversa_assistente (
    id            BIGSERIAL,
    usuario_id    BIGINT    NOT NULL,
    criada_em     TIMESTAMP NOT NULL,
    atualizada_em TIMESTAMP NOT NULL,
    CONSTRAINT pk_conversa_assistente         PRIMARY KEY (id),
    CONSTRAINT fk_conversa_assistente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);

CREATE INDEX ix_conversa_assistente_usuario ON conversa_assistente (usuario_id);

CREATE TABLE mensagem_assistente (
    id          BIGSERIAL,
    conversa_id BIGINT      NOT NULL,
    papel       VARCHAR(20) NOT NULL,
    conteudo    JSONB       NOT NULL,
    criada_em   TIMESTAMP   NOT NULL,
    CONSTRAINT pk_mensagem_assistente          PRIMARY KEY (id),
    CONSTRAINT fk_mensagem_assistente_conversa FOREIGN KEY (conversa_id) REFERENCES conversa_assistente (id) ON DELETE CASCADE,
    CONSTRAINT ck_mensagem_assistente_papel    CHECK (papel IN ('USUARIO', 'ASSISTENTE'))
);

CREATE INDEX ix_mensagem_assistente_conversa ON mensagem_assistente (conversa_id, id);
