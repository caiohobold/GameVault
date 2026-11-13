package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import io.swagger.v3.oas.annotations.media.Schema;

public record AvaliacaoResponse(
        Long id,
        UsuarioResumoResponse usuario,
        JogoResumoResponse jogo,
        Integer nota,
        String comentario,
        LocalDateTime dataAvaliacao,
        @Schema(description = "Só APROVADA aparece na página do jogo e conta na nota média", example = "APROVADA")
        StatusModeracao statusModeracao,
        @Schema(description = "Explicação da moderação para o autor", example = "Comentário dentro das regras da comunidade")
        String motivoModeracao,
        @Schema(description = "Quando true, o frontend esconde o texto atrás de um aviso de spoiler")
        Boolean contemSpoiler) {
}
