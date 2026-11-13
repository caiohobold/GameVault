"""API interna do GameVaultML.

So o backend Java chama estes endpoints. Eles nao sao expostos ao frontend e nao
aplicam regra de negocio: recebem um pedido, leem o banco e devolvem numeros.
"""

import logging
from datetime import date

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from app import anomalias, demanda, embeddings, recomendacao
from app import config

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

app = FastAPI(title="GameVaultML", description="Embeddings e modelos de aprendizado de maquina do GameVault")


class EmbeddingsRequisicao(BaseModel):
    textos: list[str] = Field(min_length=1, max_length=64)


class Janela(BaseModel):
    inicio: date
    fim: date
    percentualDesconto: int = Field(ge=0, le=90)


class PrevisaoRequisicao(BaseModel):
    jogoId: int
    janelas: list[Janela] = Field(min_length=1, max_length=52)


@app.get("/saude")
def saude():
    return {"status": "ok", "modeloEmbeddings": config.MODELO_EMBEDDINGS}


@app.post("/embeddings")
def gerar_embeddings(requisicao: EmbeddingsRequisicao):
    vetores = embeddings.gerar(requisicao.textos)
    return {"vetores": vetores, "modelo": config.MODELO_EMBEDDINGS, "dimensoes": config.DIMENSOES_EMBEDDING}


@app.post("/recomendacao/treinar")
def treinar_recomendacao():
    return recomendacao.treinar()


@app.post("/demanda/treinar")
def treinar_demanda():
    try:
        return demanda.treinar()
    except demanda.DadosInsuficientes as erro:
        raise HTTPException(status_code=422, detail=str(erro)) from erro


@app.post("/demanda/prever")
def prever_demanda(requisicao: PrevisaoRequisicao):
    try:
        return demanda.prever(requisicao.jogoId, [janela.model_dump() for janela in requisicao.janelas])
    except demanda.DadosInsuficientes as erro:
        raise HTTPException(status_code=422, detail=str(erro)) from erro


@app.post("/anomalias/detectar")
def detectar_anomalias():
    return anomalias.detectar()
