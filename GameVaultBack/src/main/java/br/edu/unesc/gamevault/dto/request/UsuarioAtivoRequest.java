package br.edu.unesc.gamevault.dto.request;

import jakarta.validation.constraints.NotNull;

public record UsuarioAtivoRequest(

        @NotNull(message = "O campo 'ativo' é obrigatório")
        Boolean ativo) {
}
