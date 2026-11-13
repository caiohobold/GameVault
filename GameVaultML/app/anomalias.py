"""Deteccao de fraude por deteccao de anomalias.

O Isolation Forest aprende como e o comportamento normal dos jogadores e aponta quem
foge muito do padrao, sem precisar de exemplos de fraude. Quando o admin ja revisou
alertas suficientes, um RandomForest treinado com essas decisoes entra na pontuacao.
So vira suspeito quem tem pelo menos um motivo explicavel.
"""

import logging

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest, RandomForestClassifier

from app import banco, config

log = logging.getLogger(__name__)

PROPORCAO_ESPERADA_DE_ANOMALIAS = 0.06
MINIMO_ROTULOS_SUPERVISIONADO = 30
LIMIAR_SUPERVISIONADO = 0.6

CARACTERISTICAS = [
    "idade_conta_dias",
    "avaliacoes_7d",
    "avaliacoes_total",
    "proporcao_extremas",
    "avaliacoes_zero_horas",
    "media_horas_avaliados",
    "cancelados_30d",
    "proporcao_cancelados",
]


def carregar_caracteristicas() -> pd.DataFrame:
    linhas = banco.consultar(
        """
        select u.id as usuario_id,
               extract(epoch from (now() - u.data_cadastro)) / 86400 as idade_conta_dias,
               coalesce(av.ultimos_7d, 0)      as avaliacoes_7d,
               coalesce(av.total, 0)           as avaliacoes_total,
               coalesce(av.extremas, 0)        as extremas,
               coalesce(av.zero_horas, 0)      as avaliacoes_zero_horas,
               coalesce(av.media_horas, 0)     as media_horas_avaliados,
               coalesce(pe.cancelados_30d, 0)  as cancelados_30d,
               coalesce(pe.cancelados, 0)      as cancelados,
               coalesce(pe.total, 0)           as pedidos_total
          from usuario u
          left join (
                select a.usuario_id,
                       count(*) filter (where a.data_avaliacao >= now() - interval '7 days') as ultimos_7d,
                       count(*) as total,
                       count(*) filter (where a.nota in (1, 5)) as extremas,
                       count(*) filter (where coalesce(b.horas_jogadas, 0) = 0) as zero_horas,
                       avg(coalesce(b.horas_jogadas, 0)) as media_horas
                  from avaliacao a
                  left join biblioteca b on b.usuario_id = a.usuario_id and b.jogo_id = a.jogo_id
                 group by a.usuario_id
          ) av on av.usuario_id = u.id
          left join (
                select p.usuario_id,
                       count(*) filter (where p.status = 'CANCELADO' and p.data_pedido >= now() - interval '30 days')
                           as cancelados_30d,
                       count(*) filter (where p.status = 'CANCELADO') as cancelados,
                       count(*) as total
                  from pedido p
                 group by p.usuario_id
          ) pe on pe.usuario_id = u.id
         where u.role = 'USUARIO' and u.ativo = true
        """
    )
    tabela = pd.DataFrame(linhas)
    if tabela.empty:
        return tabela

    tabela = tabela.set_index("usuario_id").astype(float)
    tabela["proporcao_extremas"] = np.where(
        tabela["avaliacoes_total"] > 0, tabela["extremas"] / tabela["avaliacoes_total"].clip(lower=1), 0.0
    )
    tabela["proporcao_cancelados"] = np.where(
        tabela["pedidos_total"] > 0, tabela["cancelados"] / tabela["pedidos_total"].clip(lower=1), 0.0
    )
    return tabela[CARACTERISTICAS]


def motivos_do_usuario(linha: pd.Series, referencia: pd.DataFrame) -> list[str]:
    """Explica em portugues o que foge do padrao da loja (percentil 95 ou 5)."""
    alto = referencia.quantile(0.95)
    baixo = referencia.quantile(0.05)
    motivos = []

    if linha["avaliacoes_7d"] >= 3 and linha["avaliacoes_7d"] > alto["avaliacoes_7d"]:
        motivos.append(f"{int(linha['avaliacoes_7d'])} avaliações nos últimos 7 dias")
    if linha["avaliacoes_total"] >= 3 and linha["proporcao_extremas"] >= 0.9 and linha["proporcao_extremas"] > alto["proporcao_extremas"] - 1e-9:
        motivos.append(f"{round(linha['proporcao_extremas'] * 100)}% das notas são 1 ou 5")
    if linha["avaliacoes_zero_horas"] >= 2 and linha["avaliacoes_zero_horas"] > alto["avaliacoes_zero_horas"]:
        motivos.append(f"{int(linha['avaliacoes_zero_horas'])} avaliações de jogos com zero horas jogadas")
    if linha["cancelados_30d"] >= 3 and linha["cancelados_30d"] > alto["cancelados_30d"]:
        motivos.append(f"{int(linha['cancelados_30d'])} pedidos cancelados nos últimos 30 dias")
    if motivos and linha["idade_conta_dias"] < max(baixo["idade_conta_dias"], 7):
        motivos.append(f"Conta criada há {max(int(linha['idade_conta_dias']), 0)} dias")

    return motivos


def _rotulos() -> pd.Series:
    linhas = banco.consultar(
        """
        select distinct on (usuario_id) usuario_id, status
          from alerta_fraude
         where status in ('CONFIRMADO', 'DESCARTADO')
         order by usuario_id, revisado_em desc nulls last
        """
    )
    return pd.Series({l["usuario_id"]: 1 if l["status"] == "CONFIRMADO" else 0 for l in linhas}, dtype=float)


def detectar() -> dict:
    tabela = carregar_caracteristicas()
    if len(tabela) < 10:
        return {"suspeitos": [], "usuariosAnalisados": len(tabela), "classificadorSupervisionado": False}

    floresta = IsolationForest(
        n_estimators=300, contamination=PROPORCAO_ESPERADA_DE_ANOMALIAS, random_state=config.SEMENTE
    ).fit(tabela.values)

    anomalia = -floresta.score_samples(tabela.values)
    pontuacao = (anomalia - anomalia.min()) / max(anomalia.max() - anomalia.min(), 1e-9)
    anomalo = floresta.predict(tabela.values) == -1

    rotulos = _rotulos()
    rotulados = rotulos[rotulos.index.isin(tabela.index)]
    supervisionado = len(rotulados) >= MINIMO_ROTULOS_SUPERVISIONADO and rotulados.nunique() == 2

    if supervisionado:
        classificador = RandomForestClassifier(n_estimators=200, random_state=config.SEMENTE)
        classificador.fit(tabela.loc[rotulados.index].values, rotulados.values)
        probabilidade = classificador.predict_proba(tabela.values)[:, 1]
        pontuacao = (pontuacao + probabilidade) / 2
        anomalo = anomalo | (pontuacao >= LIMIAR_SUPERVISIONADO)

    suspeitos = []
    for posicao, (usuario_id, linha) in enumerate(tabela.iterrows()):
        if not anomalo[posicao]:
            continue
        motivos = motivos_do_usuario(linha, tabela)
        if motivos:
            suspeitos.append({"usuarioId": int(usuario_id), "pontuacao": round(float(pontuacao[posicao]), 4), "motivos": motivos})

    suspeitos.sort(key=lambda s: -s["pontuacao"])
    log.info("Deteccao de anomalias: %d usuarios, %d suspeitos, supervisionado=%s", len(tabela), len(suspeitos), supervisionado)
    return {"suspeitos": suspeitos, "usuariosAnalisados": len(tabela), "classificadorSupervisionado": bool(supervisionado)}
