package br.edu.unesc.gamevault.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Usuário autenticado, mas sem permissão para o recurso: 403. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcessoNegadoManipulador implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta,
            AccessDeniedException excecao) throws IOException {
        log.warn("Acesso negado em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        RespostaDeSeguranca.escrever(objectMapper, requisicao, resposta, HttpStatus.FORBIDDEN,
                "Acesso negado",
                "O seu perfil não tem permissão para acessar este recurso");
    }
}
