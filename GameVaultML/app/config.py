"""Configuracao do GameVaultML, lida das variaveis de ambiente."""

import os

DB_HOST = os.getenv("ML_DB_HOST", "localhost")
DB_PORT = int(os.getenv("ML_DB_PORT", "5432"))
DB_NAME = os.getenv("ML_DB_NAME", "gamevault")
DB_USER = os.getenv("ML_DB_USER", "gamevault")
DB_PASSWORD = os.getenv("ML_DB_PASSWORD", "gamevault")

# Modelo multilingue (portugues incluso) que gera vetores de 384 dimensoes.
# Roda localmente: nenhum texto sai da maquina.
MODELO_EMBEDDINGS = os.getenv("ML_MODELO_EMBEDDINGS", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
DIMENSOES_EMBEDDING = 384

SEMENTE = 42
