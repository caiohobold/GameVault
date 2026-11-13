# GameVaultML

Serviço interno em Python com os modelos de aprendizado de máquina do GameVault. Só o
backend Spring chama este serviço; ele não fica exposto ao frontend.

| Rota | O que faz | Usado pelo módulo |
|---|---|---|
| `POST /embeddings` | Transforma textos em vetores de 384 dimensões (`paraphrase-multilingual-MiniLM-L12-v2`, roda localmente) | Busca em linguagem natural, duplicidade no cadastro |
| `POST /recomendacao/treinar` | Filtragem colaborativa (TruncatedSVD) + jogos similares + hit rate@10 | Recomendação |
| `POST /demanda/treinar` e `/demanda/prever` | Gradient boosting com restrição monotônica no desconto | Sugestão de promoções |
| `POST /anomalias/detectar` | Isolation Forest e, com rótulos do admin, RandomForest | Detecção de fraude |
| `GET /saude` | Health check | — |

O serviço **só lê** o banco. Quem grava recomendações, alertas e métricas é o backend
Java, que também aplica as regras de negócio.

## Como rodar

Requer Python 3.12 (o PyTorch ainda não tem versão para 3.15).

```bash
cd GameVaultML
python3.12 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --port 8000
```

No Windows: `py -3.12 -m venv .venv`, `.venv\Scripts\pip install -r requirements.txt` e
`.venv\Scripts\uvicorn app.main:app --port 8000`.

Na primeira chamada de `/embeddings` o modelo é baixado (cerca de 470 MB) e fica em cache.

Variáveis de ambiente (os padrões batem com o `.env.example` da raiz):

| Variável | Padrão |
|---|---|
| `ML_DB_HOST` / `ML_DB_PORT` | `localhost` / `5432` |
| `ML_DB_NAME` / `ML_DB_USER` / `ML_DB_PASSWORD` | `gamevault` |

Com Docker, o serviço sobe junto com o banco pelo `docker compose up -d` da raiz.

## Dados de demonstração

O seed da migration V2 é pequeno demais para treinar qualquer modelo. Depois que o
backend aplicou as migrations, rode **num banco de testes**:

```bash
ML_DB_NAME=gamevault_ia .venv/bin/python -m scripts.popular_demo
```

Cria 150 jogadores, 20 jogos, um ano de pedidos (com mais vendas nas promoções),
avaliações com texto, comentários ofensivos e com spoiler para a moderação e 8 contas de
fraude plantadas. Todas as contas têm senha `user123` e e-mail `@demo.gamevault.dev`
(por exemplo `jogador001@demo.gamevault.dev` e a publicadora `lumen@demo.gamevault.dev`).

Os textos das avaliações estão em `dados_demo/avaliacoes.json`. Foram gerados uma vez com
o Claude e versionados, para o script não depender da API.

## Testes

```bash
.venv/bin/python -m pytest tests
```
