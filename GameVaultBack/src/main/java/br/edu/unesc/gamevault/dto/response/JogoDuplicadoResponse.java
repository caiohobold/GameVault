package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jogo já cadastrado muito parecido com o que está sendo cadastrado")
public record JogoDuplicadoResponse(
        JogoResumoResponse jogo,
        @Schema(description = "De 0 a 1; acima de 0,9 indica provável duplicidade", example = "0.94")
        Double similaridade) {
}
