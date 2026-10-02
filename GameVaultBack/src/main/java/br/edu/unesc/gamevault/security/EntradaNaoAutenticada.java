package br.edu.unesc.gamevault.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Requisição sem token, ou com token inválido, em rota protegida: 401. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntradaNaoAutenticada implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta,
            AuthenticationException excecao) throws IOException {
        log.warn("Acesso nao autenticado em {}: {}", requisicao.getRequestURI(), excecao.getMessage());

        RespostaDeSeguranca.escrever(objectMapper, requisicao, resposta, HttpStatus.UNAUTHORIZED,
                "Não autenticado",
                "É necessário enviar um token JWT válido no cabeçalho Authorization");
    }
}
