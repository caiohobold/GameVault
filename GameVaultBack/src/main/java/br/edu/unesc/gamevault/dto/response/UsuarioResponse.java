package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unesc.gamevault.entity.enums.Role;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Role role,
        BigDecimal saldo,
        LocalDateTime dataCadastro,
        Boolean ativo) {
}
