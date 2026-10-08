package br.edu.unesc.gamevault.dto.request;

import br.edu.unesc.gamevault.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Criação de usuário pelo administrador")
public record UsuarioRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @Schema(description = "Nome de exibição", example = "Pixel Forge Studios")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        @Schema(description = "E-mail único", example = "contato@pixelforge.dev")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        @Schema(description = "Mínimo de 6 caracteres", example = "publi123")
        String senha,

        @NotNull(message = "O papel do usuário é obrigatório")
        @Schema(description = "ADMIN, PUBLICADORA ou USUARIO", example = "PUBLICADORA")
        Role role) {
}
