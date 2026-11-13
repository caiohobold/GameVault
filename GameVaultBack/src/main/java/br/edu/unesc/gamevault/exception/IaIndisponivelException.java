package br.edu.unesc.gamevault.exception;

/**
 * A API do Claude ou o serviço GameVaultML não puderam responder: chave ausente,
 * serviço fora do ar, tempo esgotado ou recusa do modelo. Vira 503.
 */
public class IaIndisponivelException extends RuntimeException {
    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
