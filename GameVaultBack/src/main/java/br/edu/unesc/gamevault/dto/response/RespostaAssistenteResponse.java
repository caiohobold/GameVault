package br.edu.unesc.gamevault.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do assistente da loja")
public record RespostaAssistenteResponse(
        @Schema(description = "Use nas próximas mensagens para continuar a conversa", example = "12")
        Long conversaId,
        @Schema(example = "Velocidade Terminal está com 30% de desconto: sai por R$ 104,93.")
        String resposta,
        List<AcaoPendenteResponse> acoesPendentes) {
}
