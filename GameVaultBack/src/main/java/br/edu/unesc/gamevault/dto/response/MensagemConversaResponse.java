package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

import br.edu.unesc.gamevault.entity.enums.PapelMensagem;

public record MensagemConversaResponse(PapelMensagem papel, String texto, LocalDateTime criadaEm) {
}
