package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Descrição sugerida pela IA; só é gravada se a publicadora usar no cadastro")
public record DescricaoGeradaResponse(String descricao, @Schema(example = "812") Integer caracteres) {
}
