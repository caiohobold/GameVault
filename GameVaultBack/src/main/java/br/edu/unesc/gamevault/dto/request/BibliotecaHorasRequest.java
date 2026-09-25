package br.edu.unesc.gamevault.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BibliotecaHorasRequest(

        @NotNull(message = "As horas jogadas são obrigatórias")
        @Min(value = 0, message = "As horas jogadas não podem ser negativas")
        Integer horasJogadas) {
}
