from fastapi.testclient import TestClient

from app.main import app

cliente = TestClient(app)


def test_saude():
    resposta = cliente.get("/saude")
    assert resposta.status_code == 200
    assert resposta.json()["status"] == "ok"


def test_previsao_recusa_desconto_fora_da_faixa_da_rn15():
    resposta = cliente.post("/demanda/prever", json={
        "jogoId": 1,
        "janelas": [{"inicio": "2026-12-01", "fim": "2026-12-08", "percentualDesconto": 95}],
    })
    assert resposta.status_code == 422


def test_embeddings_exige_ao_menos_um_texto():
    assert cliente.post("/embeddings", json={"textos": []}).status_code == 422
