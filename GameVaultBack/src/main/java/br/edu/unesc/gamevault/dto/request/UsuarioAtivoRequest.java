package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ativação ou desativação de conta")
public record UsuarioAtivoRequest(

        @NotNull(message = "O campo 'ativo' é obrigatório")
        @Schema(description = "false desativa a conta e impede o login", example = "false")
        Boolean ativo) {
}
