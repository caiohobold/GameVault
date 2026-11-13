"""Embeddings de texto com sentence-transformers.

O modelo e carregado uma vez, na primeira chamada, e fica em memoria. Os vetores sao
normalizados, entao a similaridade de cosseno no pgvector fica entre -1 e 1.
"""

import logging
from functools import lru_cache

from app import config

log = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def _modelo():
    from sentence_transformers import SentenceTransformer

    log.info("Carregando o modelo de embeddings %s", config.MODELO_EMBEDDINGS)
    return SentenceTransformer(config.MODELO_EMBEDDINGS)


def gerar(textos: list[str]) -> list[list[float]]:
    if not textos:
        return []
    vetores = _modelo().encode(textos, normalize_embeddings=True, convert_to_numpy=True)
    return [[float(valor) for valor in vetor] for vetor in vetores]


def vetor_do_banco(texto_pgvector: str) -> list[float]:
    """Converte o formato texto do pgvector ('[0.1,0.2,...]') em lista de floats."""
    return [float(valor) for valor in texto_pgvector.strip("[]").split(",") if valor]
