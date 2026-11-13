package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da reindexação dos embeddings dos jogos")
public record ReindexacaoResponse(@Schema(example = "30") Integer embeddingsGerados) {
}
