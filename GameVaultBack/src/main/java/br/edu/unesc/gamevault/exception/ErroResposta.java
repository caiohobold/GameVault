package br.edu.unesc.gamevault.exception;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Formato único de erro devolvido pela API em qualquer falha")
public record ErroResposta(

        @Schema(description = "Momento em que o erro ocorreu", example = "2026-10-08T14:32:10.512")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP", example = "422")
        int status,

        @Schema(description = "Categoria do erro", example = "Regra de negócio violada")
        String erro,

        @Schema(description = "Explicação legível para o usuário final",
                example = "Saldo insuficiente: a carteira tem R$ 10.00 e o pedido custa R$ 199.90")
        String mensagem,

        @Schema(description = "Rota que originou o erro", example = "/pedidos/42/pagar")
        String path,

        @Schema(description = "Preenchido apenas em erros de validação, mapeando campo para mensagem",
                example = "{\"nota\": \"A nota máxima é 5\"}")
        Map<String, String> campos) {
    public static ErroResposta de(int status, String erro, String mensagem, String path) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, path, null);
    }

    public static ErroResposta deValidacao(int status, String erro, String mensagem, String path,
            Map<String, String> campos) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, path, campos);
    }
}
