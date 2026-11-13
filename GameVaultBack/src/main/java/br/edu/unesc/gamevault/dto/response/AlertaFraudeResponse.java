package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.unesc.gamevault.entity.enums.StatusAlertaFraude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuário apontado pela detecção de anomalias")
public record AlertaFraudeResponse(
        Long id,
        UsuarioResumoResponse usuario,
        Boolean usuarioAtivo,
        LocalDateTime usuarioCadastradoEm,
        @Schema(description = "De 0 a 1; quanto maior, mais suspeito", example = "0.87")
        Double pontuacao,
        @Schema(example = "[\"12 avaliações nota 1 nos últimos 7 dias\", \"Conta criada há 2 dias\"]")
        List<String> motivos,
        StatusAlertaFraude status,
        LocalDateTime criadoEm,
        UsuarioResumoResponse revisadoPor,
        LocalDateTime revisadoEm) {
}
