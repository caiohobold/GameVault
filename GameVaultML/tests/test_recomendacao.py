import numpy as np

from app import recomendacao


def test_peso_cresce_com_horas_e_nota():
    sem_jogar = recomendacao.peso_da_interacao(0, None)
    muito_jogado = recomendacao.peso_da_interacao(200, None)
    adorou = recomendacao.peso_da_interacao(10, 5)
    odiou = recomendacao.peso_da_interacao(10, 1)

    assert sem_jogar == 1.0
    assert muito_jogado > sem_jogar
    assert adorou > odiou > 0


def test_modelo_aprende_gosto_e_supera_popularidade():
    """Dois grupos de jogadores com gostos diferentes: o modelo deve recomendar dentro do grupo."""
    gerador = np.random.default_rng(0)
    jogadores, jogos = 80, 20
    matriz = np.zeros((jogadores, jogos))
    possuidos = {}
    for usuario in range(jogadores):
        grupo = range(0, 10) if usuario < 40 else range(10, 20)
        escolhidos = gerador.choice(list(grupo), size=5, replace=False)
        matriz[usuario, escolhidos] = 1.0
        possuidos[usuario] = [int(j) for j in escolhidos]

    taxa, popularidade, avaliados = recomendacao.hit_rate_10(matriz, possuidos, {})

    assert avaliados == jogadores
    assert taxa > popularidade
    assert taxa > 0.75


def test_fatoracao_nao_gera_valores_invalidos():
    matriz = np.random.default_rng(1).random((30, 12))
    usuarios, jogos = recomendacao.fatorar(matriz)

    assert np.isfinite(usuarios).all() and np.isfinite(jogos).all()
    assert usuarios.shape[0] == 30 and jogos.shape[0] == 12
