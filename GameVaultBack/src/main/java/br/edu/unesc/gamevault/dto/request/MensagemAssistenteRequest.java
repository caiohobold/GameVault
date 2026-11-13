package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Mensagem do usuário para o assistente da loja")
public record MensagemAssistenteRequest(
        @Schema(description = "Conversa em andamento; vazio para começar uma nova", example = "12")
        Long conversaId,
        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
        @Schema(example = "Tem algum jogo de corrida em promoção?")
        String mensagem) {
}
