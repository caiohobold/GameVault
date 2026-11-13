"""Popula um banco de desenvolvimento com dados de demonstracao para os modelos de IA.

O seed da migration V2 tem 3 usuarios e 10 jogos: pouco para qualquer modelo aprender.
Este script cria cerca de 150 jogadores, 20 jogos novos, um ano de pedidos (com mais
vendas nos periodos de promocao), biblioteca com horas jogadas, lista de desejos e
avaliacoes com texto. Tambem planta casos para demonstrar a moderacao (comentarios
ofensivos e com spoiler) e a deteccao de fraude (contas novas dando nota 1 em massa e
um usuario que cancela pedidos sem parar).

Rode depois que o backend aplicou as migrations, apontando para um banco de testes:

    ML_DB_NAME=gamevault_ia python -m scripts.popular_demo

Nao e uma migration: so roda quando chamado. A senha de todas as contas criadas e
user123. Os e-mails terminam em @demo.gamevault.dev.
"""

import json
import math
import random
import sys
from datetime import date, datetime, time, timedelta
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

import psycopg
from psycopg.rows import dict_row

from app import config

SEMENTE = 42
DOMINIO = "demo.gamevault.dev"
# BCrypt de "user123", o mesmo hash usado pela migration V2
SENHA_USER123 = "$2a$10$ltPO0//iQZ11Is8q/WmxHemQEyADSCfDO8iWk48NqA0JswMCPAWN6"
TOTAL_JOGADORES = 150
CONTAS_FRAUDE = 8
MOTIVO_OK = "Comentário dentro das regras da comunidade"

NOMES = ["Ana", "Bruno", "Carla", "Diego", "Eduarda", "Felipe", "Gabriela", "Henrique", "Isabela", "João",
         "Larissa", "Marcos", "Natália", "Otávio", "Paula", "Rafael", "Sofia", "Thiago", "Vitória", "William",
         "Beatriz", "Caio", "Daniela", "Enzo", "Fernanda", "Gustavo", "Helena", "Igor", "Júlia", "Lucas"]
SOBRENOMES = ["Silva", "Souza", "Oliveira", "Pereira", "Lima", "Costa", "Rodrigues", "Almeida", "Nunes",
              "Carvalho", "Ribeiro", "Martins", "Rocha", "Mendes", "Barbosa", "Freitas", "Zanette", "Machado"]

NOVOS_JOGOS = [
    ("Nevoeiro em Ouro Preto", "Terror investigativo numa cidade histórica tomada por uma névoa que não vai embora. Explore casarões, resolva enigmas e fuja do que se esconde nas ladeiras.", "69.90", "2025-03-14", ["Terror", "Aventura"]),
    ("Esquadrão Cooperativo", "Tiro cooperativo para até quatro amigos contra hordas de robôs. Partidas curtas, classes que se completam e muita correria.", "79.90", "2024-10-03", ["Acao", "Multiplayer"]),
    ("Fazenda Serena", "Simulador de fazenda tranquilo: plante, cuide dos animais, faça amizade com os vizinhos e decore sua casa no ritmo que quiser.", "49.90", "2023-06-22", ["Simulacao", "Indie"]),
    ("Grand Prix Brasil", "Corridas de Fórmula com circuitos brasileiros, clima dinâmico e modo carreira completo.", "199.90", "2025-04-30", ["Corrida", "Esporte"]),
    ("Lendas de Pindorama", "RPG de ação inspirado no folclore brasileiro: enfrente o Curupira, negocie com a Iara e escolha o destino da floresta.", "159.90", "2024-12-05", ["RPG", "Aventura", "Acao"]),
    ("Império das Areias", "Estratégia em tempo real no deserto: construa cidades, controle os oásis e forme alianças com tribos rivais.", "129.90", "2024-02-16", ["Estrategia"]),
    ("Quintal dos Monstros", "Terror cooperativo para até quatro jogadores: sobreviva a uma noite num sítio assombrado juntando peças para consertar a caminhonete.", "59.90", "2025-07-18", ["Terror", "Multiplayer"]),
    ("Basquete de Rua 3x3", "Partidas rápidas de basquete 3x3 com jogadas de efeito e torneios online.", "89.90", "2024-08-09", ["Esporte", "Multiplayer"]),
    ("Pixel Dungeon Café", "Roguelike fofo em pixel art: explore masmorras de dia e administre um café de aventureiros à noite.", "34.90", "2025-01-10", ["Indie", "RPG"]),
    ("Torre de Controle", "Simulador de controle de tráfego aéreo com aeroportos reais e situações de emergência.", "64.90", "2023-09-01", ["Simulacao", "Estrategia"]),
    ("Reino Partido", "Estratégia por turnos com diplomacia profunda, traições e um mapa que muda a cada campanha.", "109.90", "2025-05-23", ["Estrategia", "RPG"]),
    ("Asfalto Selvagem", "Corridas off-road em trilhas de terra e lama, com veículos destrutíveis e multiplayer online.", "119.90", "2024-04-11", ["Corrida", "Multiplayer", "Acao"]),
    ("Os Silenciosos", "Terror psicológico em primeira pessoa num hospital abandonado. Sem armas: só uma lanterna e o som dos próprios passos.", "54.90", "2024-10-31", ["Terror", "Indie"]),
    ("Capitã Estelar", "Aventura espacial com exploração de planetas, tripulação com histórias próprias e escolhas que mudam o final.", "149.90", "2025-08-28", ["Aventura", "RPG"]),
    ("Arena dos Titãs", "Luta em arena com personagens gigantes, combos e ranqueadas online.", "99.90", "2023-12-07", ["Acao", "Multiplayer"]),
    ("Cozinha Caótica", "Cozinhe com até quatro amigos numa cozinha que pega fogo, gira e muda de lugar. Caos cooperativo garantido.", "44.90", "2024-06-14", ["Multiplayer", "Indie", "Simulacao"]),
    ("Sertão Infinito", "Mundo aberto de aventura no sertão nordestino, com cangaço, mistérios e cavalgadas pela caatinga.", "179.90", "2025-09-19", ["Aventura", "Acao"]),
    ("Xadrez dos Reinos", "Estratégia tática que mistura xadrez e cartas, com partidas rápidas contra amigos ou a máquina.", "29.90", "2023-04-28", ["Estrategia", "Multiplayer", "Indie"]),
    ("Skate Urbano", "Skate em uma cidade inteira para explorar, com manobras, desafios e edição de vídeos das suas sessões.", "69.90", "2024-03-21", ["Esporte", "Indie"]),
    ("Ecos da Floresta", "Aventura contemplativa sobre uma menina que conversa com os animais para salvar a floresta do avanço das queimadas.", "39.90", "2025-02-07", ["Aventura", "Indie"]),
]


def preco_com_desconto(preco: Decimal, percentual: int) -> tuple[Decimal, Decimal]:
    desconto = (preco * percentual / Decimal(100)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return preco - desconto, desconto


def momento(dia: date, aleatorio: random.Random) -> datetime:
    return datetime.combine(dia, time(aleatorio.randint(8, 23), aleatorio.randint(0, 59)))


def main(forcar: bool = False) -> None:
    aleatorio = random.Random(SEMENTE)
    textos = json.loads((Path(__file__).resolve().parent.parent / "dados_demo" / "avaliacoes.json").read_text())
    hoje = date.today()
    agora = datetime.now()

    with psycopg.connect(host=config.DB_HOST, port=config.DB_PORT, dbname=config.DB_NAME, user=config.DB_USER,
                         password=config.DB_PASSWORD, row_factory=dict_row) as con, con.cursor() as cur:
        cur.execute("select count(*) as total from usuario where email like %s", (f"%@{DOMINIO}",))
        if cur.fetchone()["total"] > 0 and not forcar:
            print(f"O banco {config.DB_NAME} já tem dados de demonstração. Nada foi feito.")
            return

        cur.execute("select id, nome from categoria")
        categoria_por_nome = {c["nome"]: c["id"] for c in cur.fetchall()}

        # Segunda publicadora, para a demonstração da RN09 (cada uma só vê os próprios jogos)
        cur.execute(
            """insert into usuario (nome, email, senha, role, saldo, data_cadastro, ativo)
               values ('Lumen Games', %s, %s, 'PUBLICADORA', 0, %s, true) returning id""",
            (f"lumen@{DOMINIO}", SENHA_USER123, agora - timedelta(days=500)),
        )
        lumen_id = cur.fetchone()["id"]

        for indice, (titulo, descricao, preco, lancamento, categorias) in enumerate(NOVOS_JOGOS):
            cur.execute(
                """insert into jogo (titulo, descricao, preco, data_lancamento, publicadora_id, nota_media,
                                     total_avaliacoes, ativo)
                   values (%s, %s, %s, %s, %s, 0, 0, true) returning id""",
                (titulo, descricao, Decimal(preco), date.fromisoformat(lancamento), lumen_id),
            )
            jogo_id = cur.fetchone()["id"]
            for nome in categorias:
                cur.execute("insert into jogo_categoria (jogo_id, categoria_id) values (%s, %s)",
                            (jogo_id, categoria_por_nome[nome]))

        cur.execute(
            """select j.id, j.preco, j.data_lancamento, array_agg(jc.categoria_id) as categorias
                 from jogo j join jogo_categoria jc on jc.jogo_id = j.id
                where j.ativo group by j.id order by j.id"""
        )
        jogos = cur.fetchall()
        qualidade = {j["id"]: aleatorio.uniform(2.6, 4.7) for j in jogos}

        # Promoções: algumas por jogo ao longo do último ano e algumas valendo agora
        promocoes: dict[int, list[tuple[date, date, int]]] = {j["id"]: [] for j in jogos}
        for jogo in jogos:
            inicio_possivel = max(hoje - timedelta(days=360), jogo["data_lancamento"] + timedelta(days=20))
            for _ in range(aleatorio.randint(2, 4)):
                if inicio_possivel >= hoje - timedelta(days=20):
                    break
                inicio = inicio_possivel + timedelta(days=aleatorio.randint(0, (hoje - inicio_possivel).days - 20))
                fim = inicio + timedelta(days=aleatorio.randint(5, 14))
                percentual = aleatorio.choice([10, 20, 25, 30, 40, 50, 60, 75])
                if any(not (fim <= a or inicio >= b) for a, b, _ in promocoes[jogo["id"]]):
                    continue
                promocoes[jogo["id"]].append((inicio, fim, percentual))
        for jogo in aleatorio.sample(jogos, 7):
            inicio, fim = hoje - timedelta(days=3), hoje + timedelta(days=10)
            if not any(not (fim <= a or inicio >= b) for a, b, _ in promocoes[jogo["id"]]):
                promocoes[jogo["id"]].append((inicio, fim, aleatorio.choice([20, 30, 40, 50])))
        for jogo_id, lista in promocoes.items():
            for inicio, fim, percentual in lista:
                cur.execute(
                    """insert into promocao (jogo_id, percentual_desconto, data_inicio, data_fim, ativa)
                       values (%s, %s, %s, %s, true)""",
                    (jogo_id, percentual, datetime.combine(inicio, time()), datetime.combine(fim, time())),
                )

        def percentual_no_dia(jogo_id: int, dia: date) -> int:
            return max((p for a, b, p in promocoes[jogo_id] if a <= dia < b), default=0)

        total_pedidos = 0

        def registrar_compra(usuario_id: int, jogo: dict, dia: date, status: str = "PAGO") -> datetime:
            nonlocal total_pedidos
            percentual = percentual_no_dia(jogo["id"], dia)
            valor, desconto = preco_com_desconto(jogo["preco"], percentual)
            quando = momento(dia, aleatorio)
            cur.execute(
                """insert into pedido (usuario_id, data_pedido, status, valor_total)
                   values (%s, %s, %s, %s) returning id""",
                (usuario_id, quando, status, valor),
            )
            pedido_id = cur.fetchone()["id"]
            cur.execute(
                """insert into item_pedido (pedido_id, jogo_id, preco_unitario, desconto_aplicado)
                   values (%s, %s, %s, %s)""",
                (pedido_id, jogo["id"], jogo["preco"], desconto),
            )
            total_pedidos += 1
            return quando

        # Jogadores com gosto definido por duas categorias favoritas
        categorias_ids = list(categoria_por_nome.values())
        jogadores = []
        for numero in range(1, TOTAL_JOGADORES + 1):
            nome = f"{aleatorio.choice(NOMES)} {aleatorio.choice(SOBRENOMES)}"
            cadastro = agora - timedelta(days=aleatorio.randint(40, 420))
            cur.execute(
                """insert into usuario (nome, email, senha, role, saldo, data_cadastro, ativo)
                   values (%s, %s, %s, 'USUARIO', %s, %s, true) returning id""",
                (nome, f"jogador{numero:03d}@{DOMINIO}", SENHA_USER123, Decimal(aleatorio.randint(50, 600)), cadastro),
            )
            jogadores.append({"id": cur.fetchone()["id"], "cadastro": cadastro.date(),
                              "favoritas": set(aleatorio.sample(categorias_ids, 2))})

        def afinidade(jogador: dict, jogo: dict) -> float:
            em_comum = len(jogador["favoritas"] & set(jogo["categorias"]))
            return 0.3 + 3.0 * em_comum + (qualidade[jogo["id"]] - 2.5) * 0.6

        avaliacoes = []
        possuidos: dict[int, dict[int, int]] = {}

        for jogador in jogadores:
            candidatos = [j for j in jogos if j["data_lancamento"] < hoje]
            pesos = [afinidade(jogador, j) for j in candidatos]
            escolhidos = set()
            for _ in range(aleatorio.randint(4, 11)):
                escolhidos.add(aleatorio.choices(candidatos, weights=pesos)[0]["id"])

            possuidos[jogador["id"]] = {}
            for jogo in (j for j in candidatos if j["id"] in escolhidos):
                inicio = max(jogador["cadastro"], jogo["data_lancamento"])
                dias = [inicio + timedelta(days=d) for d in range((hoje - inicio).days)]
                if not dias:
                    continue
                # Mais vendas nos dias de promoção, e mais quanto maior o desconto
                pesos_dia = [1 + percentual_no_dia(jogo["id"], d) / 10 * 1.6 for d in dias]
                dia = aleatorio.choices(dias, weights=pesos_dia)[0]
                quando = registrar_compra(jogador["id"], jogo, dia)

                favorito = bool(jogador["favoritas"] & set(jogo["categorias"]))
                horas = int(math.exp(aleatorio.gauss(3.2 if favorito else 2.0, 0.9)))
                cur.execute(
                    """insert into biblioteca (usuario_id, jogo_id, data_aquisicao, horas_jogadas)
                       values (%s, %s, %s, %s)""",
                    (jogador["id"], jogo["id"], quando, horas),
                )
                possuidos[jogador["id"]][jogo["id"]] = horas

                if aleatorio.random() < 0.45:
                    nota = round(qualidade[jogo["id"]] + (0.6 if favorito else -0.3) + aleatorio.gauss(0, 0.8))
                    nota = min(5, max(1, nota))
                    pool = "positivos" if nota >= 4 else "neutros" if nota == 3 else "negativos"
                    comentario = None if aleatorio.random() < 0.15 else aleatorio.choice(textos[pool])
                    data = min(quando + timedelta(days=aleatorio.randint(1, 30)), agora - timedelta(hours=1))
                    avaliacoes.append((jogador["id"], jogo["id"], nota, comentario, data, "APROVADA", MOTIVO_OK, False))

            # Lista de desejos: jogos que combinam e que ainda não tem
            nao_possuidos = [j for j in jogos if j["id"] not in possuidos[jogador["id"]]]
            for jogo in aleatorio.sample(nao_possuidos, min(len(nao_possuidos), aleatorio.randint(0, 4))):
                if afinidade(jogador, jogo) > 2:
                    cur.execute(
                        """insert into lista_desejos (usuario_id, jogo_id, data_adicao) values (%s, %s, %s)
                           on conflict do nothing""",
                        (jogador["id"], jogo["id"], agora - timedelta(days=aleatorio.randint(1, 90))),
                    )

        # Alguns pedidos pendentes e cancelados normais
        for jogador in aleatorio.sample(jogadores, 25):
            opcoes = [j for j in jogos if j["id"] not in possuidos[jogador["id"]]]
            jogo = aleatorio.choice(opcoes)
            status = aleatorio.choice(["PENDENTE", "CANCELADO"])
            registrar_compra(jogador["id"], jogo, hoje - timedelta(days=aleatorio.randint(0, 60)), status)

        # Comentários para a fila de moderação e com spoiler
        donos = [(u, j) for u, jogos_do_usuario in possuidos.items() for j in jogos_do_usuario]
        ja_avaliados = {(a[0], a[1]) for a in avaliacoes}
        livres = [par for par in donos if par not in ja_avaliados]
        aleatorio.shuffle(livres)
        for texto, (usuario_id, jogo_id) in zip(textos["ofensivos"], livres):
            avaliacoes.append((usuario_id, jogo_id, aleatorio.choice([1, 2]), texto, agora - timedelta(days=aleatorio.randint(1, 5)),
                               "EM_REVISAO", "Inserida pelo script de demonstração para a fila de moderação", False))
        for texto, (usuario_id, jogo_id) in zip(textos["spoilers"], livres[len(textos["ofensivos"]):]):
            avaliacoes.append((usuario_id, jogo_id, 5, texto, agora - timedelta(days=aleatorio.randint(1, 20)),
                               "APROVADA", "Comentário com spoiler, exibido atrás de um aviso", True))

        # Fraude plantada 1: contas novas compram jogos baratos e dão nota 1 em massa
        baratos = sorted(jogos, key=lambda j: j["preco"])[:6]
        for numero in range(1, CONTAS_FRAUDE + 1):
            cadastro = agora - timedelta(days=aleatorio.randint(1, 3))
            cur.execute(
                """insert into usuario (nome, email, senha, role, saldo, data_cadastro, ativo)
                   values (%s, %s, %s, 'USUARIO', 0, %s, true) returning id""",
                (f"user{aleatorio.randint(1000, 9999)}", f"conta{numero:02d}@{DOMINIO}", SENHA_USER123, cadastro),
            )
            usuario_id = cur.fetchone()["id"]
            for jogo in aleatorio.sample(baratos, 4):
                quando = registrar_compra(usuario_id, jogo, hoje - timedelta(days=1))
                cur.execute(
                    """insert into biblioteca (usuario_id, jogo_id, data_aquisicao, horas_jogadas)
                       values (%s, %s, %s, 0)""",
                    (usuario_id, jogo["id"], quando),
                )
                avaliacoes.append((usuario_id, jogo["id"], 1, aleatorio.choice(textos["fraude"]),
                                   min(quando + timedelta(minutes=aleatorio.randint(5, 120)), agora),
                                   "APROVADA", MOTIVO_OK, False))

        # Fraude plantada 2: um usuário que cria e cancela pedidos sem parar
        cancelador = aleatorio.choice(jogadores)
        for _ in range(12):
            jogo = aleatorio.choice(jogos)
            registrar_compra(cancelador["id"], jogo, hoje - timedelta(days=aleatorio.randint(0, 20)), "CANCELADO")

        for usuario_id, jogo_id, nota, comentario, data, status, motivo, spoiler in avaliacoes:
            cur.execute(
                """insert into avaliacao (usuario_id, jogo_id, nota, comentario, data_avaliacao, status_moderacao,
                                          motivo_moderacao, contem_spoiler, moderada_em)
                   values (%s, %s, %s, %s, %s, %s, %s, %s, %s)""",
                (usuario_id, jogo_id, nota, comentario, data, status, motivo, spoiler, data),
            )

        # O jogador de teste da V2 ganha uma lista de desejos, para mostrar a recomendação por conteúdo
        cur.execute("select id from usuario where email = 'jogador@gamevault.dev'")
        jogador_teste = cur.fetchone()
        if jogador_teste:
            for jogo in aleatorio.sample(jogos, 2):
                cur.execute(
                    """insert into lista_desejos (usuario_id, jogo_id, data_adicao) values (%s, %s, %s)
                       on conflict do nothing""",
                    (jogador_teste["id"], jogo["id"], agora),
                )

        # RN08: nota média e total só com as aprovadas
        cur.execute(
            """update jogo j
                  set nota_media = coalesce(s.media, 0), total_avaliacoes = coalesce(s.total, 0)
                 from (select j2.id,
                              round(avg(a.nota)::numeric, 2) as media,
                              count(a.id) as total
                         from jogo j2
                         left join avaliacao a on a.jogo_id = j2.id and a.status_moderacao = 'APROVADA'
                        group by j2.id) s
                where s.id = j.id"""
        )
        con.commit()

    print(f"Banco {config.DB_NAME} populado: {len(NOVOS_JOGOS)} jogos novos, {TOTAL_JOGADORES} jogadores, "
          f"{CONTAS_FRAUDE} contas de fraude plantadas, {total_pedidos} pedidos, {len(avaliacoes)} avaliações.")


if __name__ == "__main__":
    main(forcar="--forcar" in sys.argv)
