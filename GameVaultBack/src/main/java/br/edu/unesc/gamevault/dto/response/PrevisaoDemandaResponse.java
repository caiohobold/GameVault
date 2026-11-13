package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Previsão de vendas semana a semana, considerando as promoções já cadastradas")
public record PrevisaoDemandaResponse(
        JogoResumoResponse jogo,
        List<Semana> semanas,
        @Schema(example = "37.2") Double totalPrevisto,
        @Schema(description = "Erro médio absoluto do modelo, em cópias por semana", example = "1.8")
        Double erroMedioModelo) {

    public record Semana(LocalDate inicio, LocalDate fim, Integer percentualDesconto, Double vendasPrevistas) {
    }
}
