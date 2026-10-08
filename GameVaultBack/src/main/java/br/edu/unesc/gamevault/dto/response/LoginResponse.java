package br.edu.unesc.gamevault.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da autenticação")
public record LoginResponse(

        @Schema(description = "Token JWT a ser enviado no cabeçalho Authorization",
                example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqb2dhZG9yQGdhbWV2YXVsdC5kZXYifQ.abc123")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tipo,

        @Schema(description = "Instante em que o token expira", example = "2026-10-09T14:32:10Z")
        Instant expiraEm,

        @Schema(description = "Dados do usuário autenticado, sem a senha")
        UsuarioResponse usuario) {

    public static LoginResponse de(String token, Instant expiraEm, UsuarioResponse usuario) {
        return new LoginResponse(token, "Bearer", expiraEm, usuario);
    }
}
