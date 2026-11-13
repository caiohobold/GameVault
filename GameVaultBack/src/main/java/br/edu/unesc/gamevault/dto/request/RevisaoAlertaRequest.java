package br.edu.unesc.gamevault.dto.request;

import br.edu.unesc.gamevault.entity.enums.StatusAlertaFraude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Decisão do administrador sobre um alerta de fraude")
public record RevisaoAlertaRequest(
        @NotNull(message = "A decisão é obrigatória")
        @Schema(description = "CONFIRMADO ou DESCARTADO", example = "CONFIRMADO")
        StatusAlertaFraude status) {
}
