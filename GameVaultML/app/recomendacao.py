"""Recomendacao de jogos por filtragem colaborativa (fatoracao de matrizes).

Monta a matriz usuario x jogo com feedback implicito (posse, horas jogadas, nota e
lista de desejos), fatora com TruncatedSVD e recomenda, para cada usuario, os jogos
ativos que ele ainda nao tem com maior pontuacao. Tambem calcula os jogos parecidos
de cada jogo e mede a qualidade do modelo (hit rate nos 10 primeiros).
"""

import logging
import math

import numpy as np
from sklearn.decomposition import TruncatedSVD

from app import banco, config
from app.embeddings import vetor_do_banco

log = logging.getLogger(__name__)

RECOMENDACOES_POR_USUARIO = 20
# Poucos fatores: com dezenas de jogos, mais componentes decoram a matriz em vez de generalizar
# (medido com o hit rate@10 nos dados de demonstracao: 6 foi o melhor valor)
MAXIMO_COMPONENTES = 6
SIMILARES_POR_JOGO = 10
MINIMO_JOGOS_PARA_AVALIAR = 3
AJUSTE_POR_NOTA = {5: 1.0, 4: 0.5, 3: 0.0, 2: -0.5, 1: -1.0}
PESO_LISTA_DE_DESEJOS = 0.5


def _carregar():
    posses = banco.consultar(
        """
        select b.usuario_id, b.jogo_id, b.horas_jogadas, a.nota
          from biblioteca b
          left join avaliacao a on a.usuario_id = b.usuario_id
                               and a.jogo_id = b.jogo_id
                               and a.status_moderacao = 'APROVADA'
        """
    )
    desejos = banco.consultar("select usuario_id, jogo_id from lista_desejos")
    jogos = banco.consultar("select id, ativo from jogo order by id")
    embeddings = banco.consultar("select jogo_id, embedding::text as vetor from jogo_embedding")
    return posses, desejos, jogos, embeddings


def peso_da_interacao(horas: int, nota: int | None) -> float:
    """1 por ter o jogo + log(1 + horas) + ajuste pela nota da avaliacao."""
    peso = 1.0 + math.log1p(max(horas or 0, 0))
    if nota is not None:
        peso += AJUSTE_POR_NOTA.get(int(nota), 0.0)
    return max(peso, 0.1)


def montar_matriz(posses, desejos, usuarios, jogos):
    indice_usuario = {u: i for i, u in enumerate(usuarios)}
    indice_jogo = {j: i for i, j in enumerate(jogos)}
    matriz = np.zeros((len(usuarios), len(jogos)), dtype=np.float64)

    for linha in posses:
        matriz[indice_usuario[linha["usuario_id"]], indice_jogo[linha["jogo_id"]]] = peso_da_interacao(
            linha["horas_jogadas"], linha["nota"]
        )
    for linha in desejos:
        i, j = indice_usuario[linha["usuario_id"]], indice_jogo[linha["jogo_id"]]
        matriz[i, j] += PESO_LISTA_DE_DESEJOS

    return matriz, indice_usuario, indice_jogo


def fatorar(matriz: np.ndarray):
    """Devolve os vetores de gosto dos usuarios e os vetores dos jogos."""
    componentes = max(1, min(MAXIMO_COMPONENTES, matriz.shape[0] - 1, matriz.shape[1] - 1))
    # arpack: decomposicao exata, deterministica e adequada para matrizes pequenas como a da loja
    svd = TruncatedSVD(n_components=componentes, algorithm="arpack", random_state=config.SEMENTE)
    # O numpy 2.x com o Accelerate do macOS emite avisos falsos de overflow no matmul;
    # o resultado nao tem NaN nem infinito. Os avisos sao silenciados so aqui.
    with np.errstate(divide="ignore", over="ignore", invalid="ignore"):
        vetores_usuarios = svd.fit_transform(matriz)
    if not np.isfinite(vetores_usuarios).all():
        raise ValueError("A fatoracao da matriz de recomendacao gerou valores invalidos")
    vetores_jogos = svd.components_.T
    return vetores_usuarios, vetores_jogos


def _normalizar_linhas(matriz: np.ndarray) -> np.ndarray:
    normas = np.linalg.norm(matriz, axis=1, keepdims=True)
    normas[normas == 0] = 1.0
    return matriz / normas


def hit_rate_10(matriz: np.ndarray, possuidos_por_usuario: dict[int, list[int]],
                desejados_por_usuario: dict[int, set[int]]) -> tuple[float | None, float | None, int]:
    """Separa um jogo de cada usuario com 3+ jogos, treina sem ele e verifica se ele volta
    entre os 10 primeiros recomendados. Mede tambem o baseline de recomendar os jogos
    mais vendidos, para a comparacao ser honesta.

    Devolve (hit rate do modelo, hit rate da popularidade, usuarios avaliados)."""
    gerador = np.random.default_rng(config.SEMENTE)
    treino = matriz.copy()
    separados: dict[int, int] = {}

    for usuario, possuidos in possuidos_por_usuario.items():
        if len(possuidos) >= MINIMO_JOGOS_PARA_AVALIAR:
            jogo = int(gerador.choice(possuidos))
            separados[usuario] = jogo
            treino[usuario, jogo] = 0.0

    if not separados:
        return None, None, 0

    vetores_usuarios, vetores_jogos = fatorar(treino)
    pontuacoes = vetores_usuarios @ vetores_jogos.T
    popularidade = (treino > 0).sum(axis=0)
    acertos_modelo, acertos_popularidade = 0, 0

    for usuario, jogo in separados.items():
        # Mesmas exclusoes da recomendacao de verdade: o que ja tem e o que ja esta na lista de desejos
        conhecidos = (set(possuidos_por_usuario[usuario]) - {jogo}) | desejados_por_usuario.get(usuario, set())
        candidatos = [j for j in np.argsort(-pontuacoes[usuario]) if j not in conhecidos]
        mais_vendidos = [j for j in np.argsort(-popularidade, kind="stable") if j not in conhecidos]
        acertos_modelo += jogo in candidatos[:10]
        acertos_popularidade += jogo in mais_vendidos[:10]

    total = len(separados)
    return acertos_modelo / total, acertos_popularidade / total, total


def treinar() -> dict:
    posses, desejos, jogos, embeddings = _carregar()

    ids_jogos = [j["id"] for j in jogos]
    ativos = {j["id"] for j in jogos if j["ativo"]}
    ids_usuarios = sorted({p["usuario_id"] for p in posses} | {d["usuario_id"] for d in desejos})

    if len(ids_usuarios) < 2 or len(ids_jogos) < 2:
        log.warning("Poucos dados para treinar a recomendacao: %d usuarios, %d jogos", len(ids_usuarios), len(ids_jogos))
        return {
            "recomendacoes": [],
            "similares": [],
            "metricas": {"hitRate10": None, "hitRate10Popularidade": None, "usuariosAvaliados": 0,
                         "usuarios": len(ids_usuarios), "jogos": len(ids_jogos)},
        }

    matriz, indice_usuario, indice_jogo = montar_matriz(posses, desejos, ids_usuarios, ids_jogos)

    possuidos: dict[int, list[int]] = {}
    for linha in posses:
        possuidos.setdefault(indice_usuario[linha["usuario_id"]], []).append(indice_jogo[linha["jogo_id"]])
    desejados: dict[int, set[int]] = {}
    for linha in desejos:
        desejados.setdefault(indice_usuario[linha["usuario_id"]], set()).add(indice_jogo[linha["jogo_id"]])

    taxa, taxa_popularidade, avaliados = hit_rate_10(matriz, possuidos, desejados)

    vetores_usuarios, vetores_jogos = fatorar(matriz)
    pontuacoes = vetores_usuarios @ vetores_jogos.T
    jogos_normalizados = _normalizar_linhas(vetores_jogos)
    similaridade_colaborativa = jogos_normalizados @ jogos_normalizados.T

    recomendacoes = []
    for usuario_id, i in indice_usuario.items():
        excluir = set(possuidos.get(i, [])) | desejados.get(i, set())
        candidatos = [j for j in np.argsort(-pontuacoes[i]) if j not in excluir and ids_jogos[j] in ativos]
        escolhidos = [j for j in candidatos[:RECOMENDACOES_POR_USUARIO] if pontuacoes[i, j] > 0]
        if not escolhidos:
            continue

        maior = float(pontuacoes[i, escolhidos[0]]) or 1.0
        for j in escolhidos:
            motivo = None
            if possuidos.get(i):
                # O jogo da biblioteca mais parecido com o recomendado: "porque voce jogou X"
                motivo = ids_jogos[max(possuidos[i], key=lambda p: similaridade_colaborativa[p, j])]
            recomendacoes.append(
                {
                    "usuarioId": usuario_id,
                    "jogoId": ids_jogos[j],
                    "pontuacao": round(min(1.0, max(0.0, float(pontuacoes[i, j]) / maior)), 4),
                    "motivoJogoId": motivo,
                }
            )

    similares = _calcular_similares(ids_jogos, ativos, similaridade_colaborativa, embeddings)

    log.info(
        "Recomendacao treinada: %d usuarios, %d jogos, %d recomendacoes, hit rate@10=%s (popularidade=%s)",
        len(ids_usuarios), len(ids_jogos), len(recomendacoes), taxa, taxa_popularidade,
    )
    return {
        "recomendacoes": recomendacoes,
        "similares": similares,
        "metricas": {
            "hitRate10": None if taxa is None else round(taxa, 4),
            "hitRate10Popularidade": None if taxa_popularidade is None else round(taxa_popularidade, 4),
            "usuariosAvaliados": avaliados,
            "usuarios": len(ids_usuarios),
            "jogos": len(ids_jogos),
        },
    }


def _calcular_similares(ids_jogos, ativos, similaridade_colaborativa, embeddings) -> list[dict]:
    """Media entre a similaridade de quem joga (SVD) e a de conteudo (embeddings)."""
    similaridade = similaridade_colaborativa.copy()

    vetores_por_jogo = {e["jogo_id"]: vetor_do_banco(e["vetor"]) for e in embeddings}
    if vetores_por_jogo:
        dimensao = len(next(iter(vetores_por_jogo.values())))
        conteudo = np.array([vetores_por_jogo.get(j, [0.0] * dimensao) for j in ids_jogos], dtype=np.float32)
        conteudo = _normalizar_linhas(conteudo)
        tem_embedding = np.array([j in vetores_por_jogo for j in ids_jogos])
        similaridade_conteudo = conteudo @ conteudo.T
        ambos = np.outer(tem_embedding, tem_embedding)
        similaridade = np.where(ambos, (similaridade + similaridade_conteudo) / 2, similaridade)

    similares = []
    for i, jogo_id in enumerate(ids_jogos):
        ordem = [j for j in np.argsort(-similaridade[i]) if j != i and ids_jogos[j] in ativos]
        for j in ordem[:SIMILARES_POR_JOGO]:
            if similaridade[i, j] <= 0:
                break
            similares.append({"jogoId": jogo_id, "similarId": ids_jogos[j], "pontuacao": round(float(similaridade[i, j]), 4)})
    return similares
