package br.edu.unesc.gamevault.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.unesc.gamevault.exception.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Escreve o mesmo corpo de erro usado pelo GlobalExceptionHandler.
 * Os erros levantados dentro da cadeia de filtros não passam pelo
 * @RestControllerAdvice, então precisam ser serializados aqui.
 */
final class RespostaDeSeguranca {
    private RespostaDeSeguranca() {
    }

    static void escrever(ObjectMapper objectMapper, HttpServletRequest requisicao,
            HttpServletResponse resposta, HttpStatus status, String erro, String mensagem)
            throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");

        ErroResposta corpo = ErroResposta.de(status.value(), erro, mensagem, requisicao.getRequestURI());
        objectMapper.writeValue(resposta.getWriter(), corpo);
    }
}
