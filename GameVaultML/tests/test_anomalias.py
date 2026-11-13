import pandas as pd

from app import anomalias


def _populacao():
    normais = [
        {"idade_conta_dias": 200 + i, "avaliacoes_7d": i % 2, "avaliacoes_total": 4, "proporcao_extremas": 0.25,
         "avaliacoes_zero_horas": 0, "media_horas_avaliados": 30, "cancelados_30d": i % 2, "proporcao_cancelados": 0.1}
        for i in range(60)
    ]
    return pd.DataFrame(normais)


def test_conta_nova_dando_nota_1_em_massa_tem_motivos_explicaveis():
    referencia = _populacao()
    suspeito = pd.Series({"idade_conta_dias": 2, "avaliacoes_7d": 6, "avaliacoes_total": 6, "proporcao_extremas": 1.0,
                          "avaliacoes_zero_horas": 6, "media_horas_avaliados": 0, "cancelados_30d": 0,
                          "proporcao_cancelados": 0.0})

    motivos = anomalias.motivos_do_usuario(suspeito, pd.concat([referencia, suspeito.to_frame().T]))

    assert "6 avaliações nos últimos 7 dias" in motivos
    assert "100% das notas são 1 ou 5" in motivos
    assert "6 avaliações de jogos com zero horas jogadas" in motivos
    assert "Conta criada há 2 dias" in motivos


def test_jogador_comum_nao_tem_motivo():
    referencia = _populacao()
    assert anomalias.motivos_do_usuario(referencia.iloc[10], referencia) == []
