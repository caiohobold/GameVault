package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Registro de horas jogadas")
public record BibliotecaHorasRequest(

        @NotNull(message = "As horas jogadas são obrigatórias")
        @Min(value = 0, message = "As horas jogadas não podem ser negativas")
        @Schema(description = "Total acumulado de horas, nunca negativo", example = "32")
        Integer horasJogadas) {
}
