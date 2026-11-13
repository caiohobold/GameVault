"""Previsao de demanda: quantas copias um jogo vende numa janela com certo desconto.

Cada exemplo de treino e um jogo numa semana do historico. O modelo e um gradient
boosting (HistGradientBoostingRegressor) com restricao monotonica no desconto: mais
desconto nunca preve menos vendas, o que deixa a simulacao coerente para a publicadora.
"""

import logging
from datetime import date, timedelta
from threading import Lock

import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split

from app import banco, config

log = logging.getLogger(__name__)

MINIMO_AMOSTRAS = 30
COLUNAS_BASE = [
    "preco",
    "percentual",
    "dias_promocao",
    "mes",
    "nota_media",
    "total_avaliacoes",
    "desejos",
    "dias_desde_lancamento",
]


class DadosInsuficientes(Exception):
    pass


_estado = {"modelo": None, "colunas": None, "categorias": None, "mae": None, "amostras": 0}
_trava = Lock()


def _carregar_jogos() -> pd.DataFrame:
    linhas = banco.consultar(
        """
        select j.id, j.preco, j.nota_media, j.total_avaliacoes, j.data_lancamento,
               (select count(*) from lista_desejos l where l.jogo_id = j.id) as desejos,
               coalesce(array_agg(jc.categoria_id) filter (where jc.categoria_id is not null), '{}') as categorias
          from jogo j
          left join jogo_categoria jc on jc.jogo_id = j.id
         group by j.id
        """
    )
    return pd.DataFrame(linhas).set_index("id") if linhas else pd.DataFrame()


def _carregar_promocoes() -> pd.DataFrame:
    linhas = banco.consultar("select jogo_id, percentual_desconto, data_inicio, data_fim from promocao where ativa")
    return pd.DataFrame(linhas, columns=["jogo_id", "percentual_desconto", "data_inicio", "data_fim"])


def _desconto_na_janela(promocoes: pd.DataFrame, jogo_id: int, inicio: date, fim: date) -> tuple[int, int]:
    """Maior percentual vigente na janela e quantos dias da janela tiveram promocao."""
    if promocoes.empty:
        return 0, 0
    do_jogo = promocoes[promocoes["jogo_id"] == jogo_id]
    maior, dias = 0, 0
    dia = inicio
    while dia < fim:
        momento = pd.Timestamp(dia) + pd.Timedelta(hours=12)
        vigentes = do_jogo[(do_jogo["data_inicio"] <= momento) & (do_jogo["data_fim"] > momento)]
        if not vigentes.empty:
            dias += 1
            maior = max(maior, int(vigentes["percentual_desconto"].max()))
        dia += timedelta(days=1)
    return maior, dias


def _linha(jogo: pd.Series, categorias: list[int], inicio: date, percentual: int, dias_promocao: int) -> dict:
    lancamento = jogo["data_lancamento"]
    dias_lancamento = (inicio - lancamento).days if lancamento is not None else 365
    linha = {
        "preco": float(jogo["preco"]),
        "percentual": percentual,
        "dias_promocao": dias_promocao,
        "mes": inicio.month,
        "nota_media": float(jogo["nota_media"]),
        "total_avaliacoes": int(jogo["total_avaliacoes"]),
        "desejos": int(jogo["desejos"]),
        "dias_desde_lancamento": max(dias_lancamento, 0),
    }
    for categoria in categorias:
        linha[f"cat_{categoria}"] = 1 if categoria in (jogo["categorias"] or []) else 0
    return linha


def montar_exemplos() -> tuple[pd.DataFrame, pd.Series, list[int]]:
    jogos = _carregar_jogos()
    promocoes = _carregar_promocoes()
    vendas = pd.DataFrame(
        banco.consultar(
            """
            select ip.jogo_id, cast(p.data_pedido as date) as dia, count(*) as vendas
              from item_pedido ip
              join pedido p on p.id = ip.pedido_id
             where p.status = 'PAGO'
             group by ip.jogo_id, cast(p.data_pedido as date)
            """
        ),
        columns=["jogo_id", "dia", "vendas"],
    )

    if jogos.empty or vendas.empty:
        raise DadosInsuficientes("Ainda não há vendas pagas para treinar o modelo de demanda.")

    categorias = sorted({c for lista in jogos["categorias"] for c in (lista or [])})
    primeiro_dia = vendas["dia"].min()
    inicio_semana = primeiro_dia - timedelta(days=primeiro_dia.weekday())
    hoje = date.today()
    ultima_semana = hoje - timedelta(days=hoje.weekday())

    exemplos, alvos = [], []
    semana = inicio_semana
    while semana < ultima_semana:
        fim = semana + timedelta(days=7)
        na_semana = vendas[(vendas["dia"] >= semana) & (vendas["dia"] < fim)].groupby("jogo_id")["vendas"].sum()
        for jogo_id, jogo in jogos.iterrows():
            lancamento = jogo["data_lancamento"]
            if lancamento is not None and lancamento > fim:
                continue
            percentual, dias = _desconto_na_janela(promocoes, jogo_id, semana, fim)
            exemplos.append(_linha(jogo, categorias, semana, percentual, dias))
            alvos.append(float(na_semana.get(jogo_id, 0)))
        semana = fim

    return pd.DataFrame(exemplos), pd.Series(alvos), categorias


def treinar() -> dict:
    with _trava:
        x, y, categorias = montar_exemplos()
        if len(x) < MINIMO_AMOSTRAS:
            raise DadosInsuficientes(f"São necessárias pelo menos {MINIMO_AMOSTRAS} semanas de histórico.")

        colunas = list(x.columns)
        restricao = [1 if coluna == "percentual" else 0 for coluna in colunas]

        def novo_modelo():
            return HistGradientBoostingRegressor(
                max_iter=300, learning_rate=0.05, max_depth=4, monotonic_cst=restricao, random_state=config.SEMENTE
            )

        x_treino, x_teste, y_treino, y_teste = train_test_split(x, y, test_size=0.2, random_state=config.SEMENTE)
        avaliacao = novo_modelo().fit(x_treino, y_treino)
        mae = float(mean_absolute_error(y_teste, np.clip(avaliacao.predict(x_teste), 0, None)))

        modelo = novo_modelo().fit(x, y)
        _estado.update(modelo=modelo, colunas=colunas, categorias=categorias, mae=round(mae, 4), amostras=len(x))
        log.info("Modelo de demanda treinado com %d exemplos, MAE=%.3f", len(x), mae)
        return {"mae": round(mae, 4), "amostras": len(x)}


def prever(jogo_id: int, janelas: list[dict]) -> dict:
    if _estado["modelo"] is None:
        treinar()

    jogos = _carregar_jogos()
    if jogo_id not in jogos.index:
        raise DadosInsuficientes(f"Jogo {jogo_id} não encontrado.")
    jogo = jogos.loc[jogo_id]

    previsoes = []
    for janela in janelas:
        inicio, fim, percentual = janela["inicio"], janela["fim"], int(janela["percentualDesconto"])
        dias = max((fim - inicio).days, 1)
        semana = _linha(jogo, _estado["categorias"], inicio, percentual, 7 if percentual > 0 else 0)
        entrada = pd.DataFrame([semana]).reindex(columns=_estado["colunas"], fill_value=0)
        por_semana = max(float(_estado["modelo"].predict(entrada)[0]), 0.0)
        previsoes.append(
            {
                "inicio": inicio.isoformat(),
                "fim": fim.isoformat(),
                "percentualDesconto": percentual,
                "vendasPrevistas": round(por_semana * dias / 7, 2),
            }
        )

    return {"jogoId": jogo_id, "previsoes": previsoes, "mae": _estado["mae"]}
