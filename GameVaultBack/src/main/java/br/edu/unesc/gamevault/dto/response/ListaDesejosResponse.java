package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

public record ListaDesejosResponse(
        Long id,
        UsuarioResumoResponse usuario,
        JogoResumoResponse jogo,
        LocalDateTime dataAdicao) {
}
