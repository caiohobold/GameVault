package br.edu.unesc.gamevault.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException excecao, HttpServletRequest requisicao) {
        log.warn("Recurso nao encontrado em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        return construir(HttpStatus.NOT_FOUND, "Recurso não encontrado", excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> tratarRegraDeNegocio(
            RegraDeNegocioException excecao, HttpServletRequest requisicao) {
        log.warn("Regra de negocio violada em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        return construir(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", excecao.getMessage(),
                requisicao);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(
            AcessoNegadoException excecao, HttpServletRequest requisicao) {
        log.warn("Acesso negado em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        return construir(HttpStatus.FORBIDDEN, "Acesso negado", excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoDuplicado(
            RecursoDuplicadoException excecao, HttpServletRequest requisicao) {
        log.warn("Recurso duplicado em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        return construir(HttpStatus.CONFLICT, "Recurso duplicado", excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(
            MethodArgumentNotValidException excecao, HttpServletRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erroDeCampo : excecao.getBindingResult().getFieldErrors()) {
            campos.merge(erroDeCampo.getField(), erroDeCampo.getDefaultMessage(),
                    (atual, novo) -> atual + "; " + novo);
        }

        log.warn("Validacao falhou em {}: {}", requisicao.getRequestURI(), campos);

        ErroResposta corpo = ErroResposta.deValidacao(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                "Um ou mais campos estão inválidos",
                requisicao.getRequestURI(),
                campos);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(
            MethodArgumentTypeMismatchException excecao, HttpServletRequest requisicao) {
        String mensagem = "O parâmetro '%s' recebeu um valor inválido: %s"
                .formatted(excecao.getName(), excecao.getValue());

        return construir(HttpStatus.BAD_REQUEST, "Parâmetro inválido", mensagem, requisicao);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoIlegivel(
            HttpMessageNotReadableException excecao, HttpServletRequest requisicao) {
        log.warn("Corpo de requisicao ilegivel em {}: {}", requisicao.getRequestURI(),
                excecao.getMessage());

        return construir(HttpStatus.BAD_REQUEST, "Corpo inválido",
                "O corpo da requisição não é um JSON válido ou tem campos com tipo incompatível",
                requisicao);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarIntegridade(
            DataIntegrityViolationException excecao, HttpServletRequest requisicao) {
        log.error("Violacao de integridade em {}", requisicao.getRequestURI(), excecao);

        return construir(HttpStatus.CONFLICT, "Conflito de dados",
                "A operação viola uma restrição de integridade do banco de dados", requisicao);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegadoDoSpring(
            AccessDeniedException excecao, HttpServletRequest requisicao) {
        log.warn("Acesso negado por regra de autorizacao em {}: {}", requisicao.getRequestURI(),
                excecao.getMessage());

        return construir(HttpStatus.FORBIDDEN, "Acesso negado",
                "O seu perfil não tem permissão para acessar este recurso", requisicao);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResposta> tratarCredenciaisInvalidas(
            BadCredentialsException excecao, HttpServletRequest requisicao) {
        log.warn("Tentativa de login com credenciais invalidas em {}", requisicao.getRequestURI());

        return construir(HttpStatus.UNAUTHORIZED, "Credenciais inválidas",
                "E-mail ou senha incorretos", requisicao);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErroResposta> tratarContaDesativada(
            DisabledException excecao, HttpServletRequest requisicao) {
        log.warn("Tentativa de login em conta desativada em {}", requisicao.getRequestURI());

        return construir(HttpStatus.FORBIDDEN, "Conta desativada",
                "Esta conta está desativada e não pode ser utilizada", requisicao);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResposta> tratarFalhaDeAutenticacao(
            AuthenticationException excecao, HttpServletRequest requisicao) {
        log.warn("Falha de autenticacao em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        return construir(HttpStatus.UNAUTHORIZED, "Não autenticado",
                "Não foi possível autenticar a requisição", requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroInesperado(
            Exception excecao, HttpServletRequest requisicao) {
        log.error("Erro inesperado em {}", requisicao.getRequestURI(), excecao);

        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisição", requisicao);
    }

    private ResponseEntity<ErroResposta> construir(HttpStatus status, String erro, String mensagem,
            HttpServletRequest requisicao) {
        ErroResposta corpo = ErroResposta.de(status.value(), erro, mensagem, requisicao.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
