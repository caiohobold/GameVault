package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

public record BibliotecaResponse(
        Long id,
        UsuarioResumoResponse usuario,
        JogoResumoResponse jogo,
        LocalDateTime dataAquisicao,
        Integer horasJogadas) {
}
