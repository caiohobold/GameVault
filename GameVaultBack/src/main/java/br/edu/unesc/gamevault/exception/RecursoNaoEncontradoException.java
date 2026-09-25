package br.edu.unesc.gamevault.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super("%s não encontrado(a) para o identificador %s".formatted(recurso, identificador));
    }
}
