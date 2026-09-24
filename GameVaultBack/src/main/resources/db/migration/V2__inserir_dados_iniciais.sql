-- =====================================================================
-- V2 - Dados iniciais do GameVault
-- Categorias, um administrador, uma publicadora, um usuario comum
-- e dez jogos ja classificados.
--
-- Senhas (BCrypt): admin123 / publi123 / user123
-- =====================================================================

-- ---------------------------------------------------------------------
-- Categorias
-- ---------------------------------------------------------------------
INSERT INTO categoria (nome, descricao) VALUES
    ('Acao',        'Jogos de ritmo rapido com foco em combate e reflexo'),
    ('Aventura',    'Exploracao, narrativa e resolucao de enigmas'),
    ('RPG',         'Evolucao de personagem, atributos e escolhas com consequencia'),
    ('Estrategia',  'Planejamento, gerenciamento de recursos e taticas'),
    ('Esporte',     'Simulacoes e arcades de modalidades esportivas'),
    ('Corrida',     'Competicoes de velocidade com veiculos'),
    ('Terror',      'Ambientacao de suspense e sobrevivencia'),
    ('Indie',       'Producoes independentes de estudios menores'),
    ('Simulacao',   'Simuladores de atividades e sistemas reais'),
    ('Multiplayer', 'Titulos com foco em partidas online entre jogadores');

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
INSERT INTO usuario (nome, email, senha, role, saldo, data_cadastro, ativo) VALUES
    ('Administrador GameVault',
     'admin@gamevault.dev',
     '$2a$10$b1Ls8sVsOuyWVkVfvVcqfeVhWicwj1Y.Yxb0bi9veW1v1Grsg42AK',
     'ADMIN',
     0.00,
     CURRENT_TIMESTAMP,
     TRUE),
    ('Pixel Forge Studios',
     'contato@pixelforge.dev',
     '$2a$10$uGFeNHCXROlEnSJKAt63QOafcFMO5W5G0yWAd7xWlWRa687IxLLtC',
     'PUBLICADORA',
     0.00,
     CURRENT_TIMESTAMP,
     TRUE),
    ('Jogador de Teste',
     'jogador@gamevault.dev',
     '$2a$10$ltPO0//iQZ11Is8q/WmxHemQEyADSCfDO8iWk48NqA0JswMCPAWN6',
     'USUARIO',
     500.00,
     CURRENT_TIMESTAMP,
     TRUE);

-- ---------------------------------------------------------------------
-- Jogos (todos da publicadora Pixel Forge Studios)
-- nota_media e total_avaliacoes nascem zerados: sao calculados (RN08)
-- ---------------------------------------------------------------------
INSERT INTO jogo (titulo, descricao, preco, data_lancamento, publicadora_id, nota_media, total_avaliacoes, ativo)
SELECT j.titulo, j.descricao, j.preco, j.data_lancamento, u.id, 0, 0, TRUE
FROM (VALUES
    ('Sombras de Aldoria',
     'Um RPG de mundo aberto onde cada escolha reescreve o destino do reino de Aldoria.',
     199.90, DATE '2024-03-15'),
    ('Velocidade Terminal',
     'Corridas urbanas noturnas com carros altamente personalizaveis e perseguicoes policiais.',
     149.90, DATE '2024-07-02'),
    ('Colonia Orbital',
     'Simulador de gestao de uma colonia espacial: recursos, moral da tripulacao e imprevistos.',
     89.90, DATE '2023-11-20'),
    ('O Ultimo Farol',
     'Terror psicologico em um farol isolado; a luz e o unico recurso contra o que vem do mar.',
     59.90, DATE '2025-01-31'),
    ('Taticas de Ferro',
     'Estrategia por turnos em um conflito industrial, com pelotoes e terreno destrutivel.',
     119.90, DATE '2024-05-09'),
    ('Pixel Arena',
     'Arena multiplayer 2D de partidas rapidas com dezenas de campeoes destravaveis.',
     39.90, DATE '2023-08-14'),
    ('Copa Total 25',
     'Simulador de futebol com licencas nacionais, modo carreira e temporada online.',
     249.90, DATE '2025-06-10'),
    ('Jardim de Vidro',
     'Aventura indie contemplativa sobre memoria, feita a mao em aquarela.',
     29.90, DATE '2024-09-27'),
    ('Fenda Profunda',
     'Acao roguelike em cavernas geradas proceduralmente; cada descida e inedita.',
     74.90, DATE '2025-02-18'),
    ('Rotas de Comercio',
     'Estrategia economica de rotas maritimas no seculo XVII, com diplomacia e pirataria.',
     99.90, DATE '2023-04-05')
) AS j (titulo, descricao, preco, data_lancamento)
CROSS JOIN (SELECT id FROM usuario WHERE email = 'contato@pixelforge.dev') AS u;

-- ---------------------------------------------------------------------
-- Classificacao dos jogos por categoria (N:N)
-- ---------------------------------------------------------------------
INSERT INTO jogo_categoria (jogo_id, categoria_id)
SELECT j.id, c.id
FROM (VALUES
    ('Sombras de Aldoria',  'RPG'),
    ('Sombras de Aldoria',  'Aventura'),
    ('Velocidade Terminal', 'Corrida'),
    ('Velocidade Terminal', 'Acao'),
    ('Colonia Orbital',     'Simulacao'),
    ('Colonia Orbital',     'Estrategia'),
    ('O Ultimo Farol',      'Terror'),
    ('O Ultimo Farol',      'Aventura'),
    ('Taticas de Ferro',    'Estrategia'),
    ('Pixel Arena',         'Multiplayer'),
    ('Pixel Arena',         'Acao'),
    ('Pixel Arena',         'Indie'),
    ('Copa Total 25',       'Esporte'),
    ('Jardim de Vidro',     'Indie'),
    ('Jardim de Vidro',     'Aventura'),
    ('Fenda Profunda',      'Acao'),
    ('Fenda Profunda',      'Indie'),
    ('Rotas de Comercio',   'Estrategia'),
    ('Rotas de Comercio',   'Simulacao')
) AS v (titulo, categoria)
JOIN jogo      j ON j.titulo = v.titulo
JOIN categoria c ON c.nome   = v.categoria;
