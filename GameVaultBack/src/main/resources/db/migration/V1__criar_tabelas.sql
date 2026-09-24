-- =====================================================================
-- V1 - Criacao do schema inicial do GameVault
-- Loja digital de jogos: usuarios, catalogo, pedidos, biblioteca,
-- avaliacoes, promocoes e lista de desejos.
-- =====================================================================

-- ---------------------------------------------------------------------
-- usuario: contas da plataforma (ADMIN, PUBLICADORA, USUARIO)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            BIGSERIAL,
    nome          VARCHAR(100)   NOT NULL,
    email         VARCHAR(150)   NOT NULL,
    senha         VARCHAR(100)   NOT NULL,
    role          VARCHAR(20)    NOT NULL,
    saldo         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    data_cadastro TIMESTAMP      NOT NULL,
    ativo         BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario       PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_saldo CHECK (saldo >= 0),
    CONSTRAINT ck_usuario_role  CHECK (role IN ('ADMIN', 'PUBLICADORA', 'USUARIO'))
);

COMMENT ON TABLE  usuario       IS 'Contas da plataforma';
COMMENT ON COLUMN usuario.saldo IS 'Carteira virtual usada no pagamento dos pedidos';
COMMENT ON COLUMN usuario.ativo IS 'Desativacao logica; a conta inativa nao e removida do banco';

-- ---------------------------------------------------------------------
-- categoria: generos/rotulos aplicados aos jogos
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id        BIGSERIAL,
    nome      VARCHAR(60) NOT NULL,
    descricao VARCHAR(255),
    CONSTRAINT pk_categoria      PRIMARY KEY (id),
    CONSTRAINT uk_categoria_nome UNIQUE (nome)
);

COMMENT ON TABLE categoria IS 'Generos usados para classificar e filtrar o catalogo';

-- ---------------------------------------------------------------------
-- jogo: catalogo, cada titulo pertence a uma publicadora
-- ---------------------------------------------------------------------
CREATE TABLE jogo (
    id               BIGSERIAL,
    titulo           VARCHAR(150)   NOT NULL,
    descricao        VARCHAR(2000),
    preco            NUMERIC(10, 2) NOT NULL,
    data_lancamento  DATE,
    publicadora_id   BIGINT         NOT NULL,
    nota_media       NUMERIC(3, 2)  NOT NULL DEFAULT 0,
    total_avaliacoes INTEGER        NOT NULL DEFAULT 0,
    ativo            BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_jogo                  PRIMARY KEY (id),
    CONSTRAINT fk_jogo_publicadora      FOREIGN KEY (publicadora_id) REFERENCES usuario (id),
    CONSTRAINT ck_jogo_preco            CHECK (preco >= 0),
    CONSTRAINT ck_jogo_nota_media       CHECK (nota_media >= 0 AND nota_media <= 5),
    CONSTRAINT ck_jogo_total_avaliacoes CHECK (total_avaliacoes >= 0)
);

COMMENT ON COLUMN jogo.nota_media       IS 'Calculado a partir das avaliacoes; nunca enviado pelo cliente';
COMMENT ON COLUMN jogo.total_avaliacoes IS 'Calculado a partir das avaliacoes; nunca enviado pelo cliente';
COMMENT ON COLUMN jogo.ativo            IS 'Soft delete; jogo inativo some das listagens mas segue visivel em pedidos antigos';

CREATE INDEX ix_jogo_publicadora ON jogo (publicadora_id);
CREATE INDEX ix_jogo_ativo       ON jogo (ativo);
CREATE INDEX ix_jogo_titulo      ON jogo (titulo);

-- ---------------------------------------------------------------------
-- jogo_categoria: tabela de juncao N:N entre jogo e categoria
-- ---------------------------------------------------------------------
CREATE TABLE jogo_categoria (
    jogo_id      BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    CONSTRAINT pk_jogo_categoria           PRIMARY KEY (jogo_id, categoria_id),
    CONSTRAINT fk_jogo_categoria_jogo      FOREIGN KEY (jogo_id)      REFERENCES jogo (id) ON DELETE CASCADE,
    CONSTRAINT fk_jogo_categoria_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

CREATE INDEX ix_jogo_categoria_categoria ON jogo_categoria (categoria_id);

-- ---------------------------------------------------------------------
-- pedido: carrinho fechado; vira PAGO quando a carteira e debitada
-- ---------------------------------------------------------------------
CREATE TABLE pedido (
    id          BIGSERIAL,
    usuario_id  BIGINT         NOT NULL,
    data_pedido TIMESTAMP      NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    valor_total NUMERIC(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_pedido             PRIMARY KEY (id),
    CONSTRAINT fk_pedido_usuario     FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_pedido_status      CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADO')),
    CONSTRAINT ck_pedido_valor_total CHECK (valor_total >= 0)
);

COMMENT ON COLUMN pedido.valor_total IS 'Somatorio calculado no backend; nunca enviado pelo cliente';

CREATE INDEX ix_pedido_usuario ON pedido (usuario_id);
CREATE INDEX ix_pedido_status  ON pedido (status);

-- ---------------------------------------------------------------------
-- item_pedido: linha do pedido, com o preco congelado no momento da compra
-- ---------------------------------------------------------------------
CREATE TABLE item_pedido (
    id                BIGSERIAL,
    pedido_id         BIGINT         NOT NULL,
    jogo_id           BIGINT         NOT NULL,
    preco_unitario    NUMERIC(10, 2) NOT NULL,
    desconto_aplicado NUMERIC(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_item_pedido           PRIMARY KEY (id),
    CONSTRAINT fk_item_pedido_pedido    FOREIGN KEY (pedido_id) REFERENCES pedido (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_pedido_jogo      FOREIGN KEY (jogo_id)   REFERENCES jogo (id),
    CONSTRAINT uk_item_pedido_pedido_jogo UNIQUE (pedido_id, jogo_id),
    CONSTRAINT ck_item_pedido_preco     CHECK (preco_unitario >= 0),
    CONSTRAINT ck_item_pedido_desconto  CHECK (desconto_aplicado >= 0)
);

COMMENT ON COLUMN item_pedido.preco_unitario    IS 'Preco congelado no momento da compra (RN02)';
COMMENT ON COLUMN item_pedido.desconto_aplicado IS 'Valor do desconto calculado no servidor (RN03)';

CREATE INDEX ix_item_pedido_pedido ON item_pedido (pedido_id);
CREATE INDEX ix_item_pedido_jogo   ON item_pedido (jogo_id);

-- ---------------------------------------------------------------------
-- biblioteca: posse do jogo; so recebe linha quando o pedido e pago (RN04)
-- ---------------------------------------------------------------------
CREATE TABLE biblioteca (
    id             BIGSERIAL,
    usuario_id     BIGINT    NOT NULL,
    jogo_id        BIGINT    NOT NULL,
    data_aquisicao TIMESTAMP NOT NULL,
    horas_jogadas  INTEGER   NOT NULL DEFAULT 0,
    CONSTRAINT pk_biblioteca              PRIMARY KEY (id),
    CONSTRAINT fk_biblioteca_usuario      FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_biblioteca_jogo         FOREIGN KEY (jogo_id)    REFERENCES jogo (id),
    CONSTRAINT uk_biblioteca_usuario_jogo UNIQUE (usuario_id, jogo_id),
    CONSTRAINT ck_biblioteca_horas        CHECK (horas_jogadas >= 0)
);

COMMENT ON TABLE biblioteca IS 'N:N com atributos entre usuario e jogo; a unica impede compra duplicada (RN01)';

CREATE INDEX ix_biblioteca_usuario ON biblioteca (usuario_id);
CREATE INDEX ix_biblioteca_jogo    ON biblioteca (jogo_id);

-- ---------------------------------------------------------------------
-- avaliacao: uma por usuario por jogo, so de quem possui o jogo
-- ---------------------------------------------------------------------
CREATE TABLE avaliacao (
    id             BIGSERIAL,
    usuario_id     BIGINT        NOT NULL,
    jogo_id        BIGINT        NOT NULL,
    nota           INTEGER       NOT NULL,
    comentario     VARCHAR(1000),
    data_avaliacao TIMESTAMP     NOT NULL,
    CONSTRAINT pk_avaliacao              PRIMARY KEY (id),
    CONSTRAINT fk_avaliacao_usuario      FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_avaliacao_jogo         FOREIGN KEY (jogo_id)    REFERENCES jogo (id),
    CONSTRAINT uk_avaliacao_usuario_jogo UNIQUE (usuario_id, jogo_id),
    CONSTRAINT ck_avaliacao_nota         CHECK (nota >= 1 AND nota <= 5)
);

COMMENT ON TABLE avaliacao IS 'Uma avaliacao por usuario por jogo (RN07)';

CREATE INDEX ix_avaliacao_jogo    ON avaliacao (jogo_id);
CREATE INDEX ix_avaliacao_usuario ON avaliacao (usuario_id);

-- ---------------------------------------------------------------------
-- promocao: desconto percentual com janela de vigencia
-- ---------------------------------------------------------------------
CREATE TABLE promocao (
    id                  BIGSERIAL,
    jogo_id             BIGINT    NOT NULL,
    percentual_desconto INTEGER   NOT NULL,
    data_inicio         TIMESTAMP NOT NULL,
    data_fim            TIMESTAMP NOT NULL,
    ativa               BOOLEAN   NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_promocao            PRIMARY KEY (id),
    CONSTRAINT fk_promocao_jogo       FOREIGN KEY (jogo_id) REFERENCES jogo (id) ON DELETE CASCADE,
    CONSTRAINT ck_promocao_percentual CHECK (percentual_desconto >= 1 AND percentual_desconto <= 90),
    CONSTRAINT ck_promocao_periodo    CHECK (data_fim > data_inicio)
);

COMMENT ON TABLE promocao IS 'Desconto de 1% a 90% com data_fim maior que data_inicio (RN15)';

CREATE INDEX ix_promocao_jogo_ativa ON promocao (jogo_id, ativa);

-- ---------------------------------------------------------------------
-- lista_desejos: jogos marcados pelo usuario para comprar depois
-- ---------------------------------------------------------------------
CREATE TABLE lista_desejos (
    id          BIGSERIAL,
    usuario_id  BIGINT    NOT NULL,
    jogo_id     BIGINT    NOT NULL,
    data_adicao TIMESTAMP NOT NULL,
    CONSTRAINT pk_lista_desejos              PRIMARY KEY (id),
    CONSTRAINT fk_lista_desejos_usuario      FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_lista_desejos_jogo         FOREIGN KEY (jogo_id)    REFERENCES jogo (id) ON DELETE CASCADE,
    CONSTRAINT uk_lista_desejos_usuario_jogo UNIQUE (usuario_id, jogo_id)
);

CREATE INDEX ix_lista_desejos_usuario ON lista_desejos (usuario_id);
