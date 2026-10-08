package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        @Schema(description = "E-mail cadastrado", example = "jogador@gamevault.dev")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(description = "Senha em texto puro; o servidor compara com o hash BCrypt", example = "user123")
        String senha) {
}
