package br.edu.unesc.gamevault.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categorias sugeridas pela IA, sempre entre as já cadastradas")
public record CategoriasSugeridasResponse(
        List<CategoriaResponse> categorias,
        @Schema(example = "Roguelike de ação com geração procedural e cooperativo local.") String justificativa) {
}
