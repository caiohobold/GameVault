package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unesc.gamevault.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuário da plataforma. A senha nunca é exposta")
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Role role,
        BigDecimal saldo,
        LocalDateTime dataCadastro,
        Boolean ativo) {
}
