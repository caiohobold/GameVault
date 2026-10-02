package br.edu.unesc.gamevault.dto.request;

import jakarta.validation.constraints.NotNull;

public record ListaDesejosRequest(

        @NotNull(message = "O jogo é obrigatório")
        Long jogoId) {
}
