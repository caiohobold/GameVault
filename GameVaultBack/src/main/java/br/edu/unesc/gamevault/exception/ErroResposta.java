package br.edu.unesc.gamevault.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErroResposta(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        Map<String, String> campos) {
    public static ErroResposta de(int status, String erro, String mensagem, String path) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, path, null);
    }

    public static ErroResposta deValidacao(int status, String erro, String mensagem, String path,
            Map<String, String> campos) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, path, campos);
    }
}
