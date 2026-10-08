package br.edu.unesc.gamevault.dto.request;

import br.edu.unesc.gamevault.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Atualização de cadastro. Só o ADMIN pode alterar o papel")
public record UsuarioAtualizacaoRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @Schema(description = "Nome de exibição", example = "Jogador de Teste")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        @Schema(description = "E-mail único", example = "jogador@gamevault.dev")
        String email,

        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        @Schema(description = "Deixe em branco para manter a senha atual", example = "novaSenha123")
        String senha,

        @NotNull(message = "O papel do usuário é obrigatório")
        @Schema(description = "Alterável apenas por ADMIN", example = "USUARIO")
        Role role) {
}
