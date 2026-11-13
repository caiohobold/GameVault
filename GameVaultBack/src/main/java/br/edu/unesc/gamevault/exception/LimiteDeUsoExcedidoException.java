package br.edu.unesc.gamevault.exception;

/** O usuário passou do limite de chamadas por hora de um recurso de IA. Vira 429. */
public class LimiteDeUsoExcedidoException extends RuntimeException {
    public LimiteDeUsoExcedidoException(String mensagem) {
        super(mensagem);
    }
}
