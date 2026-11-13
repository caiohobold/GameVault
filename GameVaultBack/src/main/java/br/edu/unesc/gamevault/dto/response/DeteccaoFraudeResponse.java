package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de uma rodada da detecção de fraude")
public record DeteccaoFraudeResponse(
        @Schema(example = "158") Integer usuariosAnalisados,
        @Schema(example = "9") Integer suspeitos,
        @Schema(description = "Alertas novos; quem já tem alerta aberto não ganha outro", example = "7")
        Integer alertasCriados,
        @Schema(description = "true quando já há alertas revisados suficientes para o classificador supervisionado")
        Boolean classificadorSupervisionado) {
}
