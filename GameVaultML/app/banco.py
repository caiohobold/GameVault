"""Acesso somente leitura ao PostgreSQL do GameVault.

O GameVaultML so le dados para treinar e prever. Quem grava os resultados
(recomendacoes, alertas, metricas) e o backend Java, que aplica as regras de negocio.
"""

from contextlib import contextmanager

import psycopg
from psycopg.rows import dict_row

from app import config


@contextmanager
def conexao():
    con = psycopg.connect(
        host=config.DB_HOST,
        port=config.DB_PORT,
        dbname=config.DB_NAME,
        user=config.DB_USER,
        password=config.DB_PASSWORD,
        row_factory=dict_row,
    )
    try:
        con.read_only = True
        yield con
    finally:
        con.close()


def consultar(sql: str, parametros: dict | tuple | None = None) -> list[dict]:
    with conexao() as con, con.cursor() as cursor:
        cursor.execute(sql, parametros)
        return list(cursor.fetchall())
