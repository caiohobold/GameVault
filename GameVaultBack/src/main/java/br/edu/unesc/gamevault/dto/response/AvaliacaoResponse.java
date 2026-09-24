package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

public record AvaliacaoResponse(
        Long id,
        UsuarioResumoResponse usuario,
        JogoResumoResponse jogo,
        Integer nota,
        String comentario,
        LocalDateTime dataAvaliacao) {
}
