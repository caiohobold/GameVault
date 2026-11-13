package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conversa com o assistente; só as mensagens de texto, sem os bastidores das ferramentas")
public record ConversaAssistenteResponse(
        Long id,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        List<MensagemConversaResponse> mensagens) {
}
