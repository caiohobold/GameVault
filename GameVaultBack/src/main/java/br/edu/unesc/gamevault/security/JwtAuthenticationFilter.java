package br.edu.unesc.gamevault.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lê o cabeçalho Authorization: Bearer {token} em cada requisição e, se o
 * token for válido, coloca o usuário no SecurityContext.
 *
 * Token ausente ou inválido não gera erro aqui: a requisição simplesmente
 * segue sem autenticação e quem decide o 401 é o SecurityConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String CABECALHO = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest requisicao,
            @NonNull HttpServletResponse resposta,
            @NonNull FilterChain cadeia) throws ServletException, IOException {

        String cabecalho = requisicao.getHeader(CABECALHO);

        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        String token = cabecalho.substring(PREFIXO.length()).trim();

        try {
            String email = jwtService.extrairEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);

                if (usuario.isEnabled() && jwtService.tokenValido(token, usuario)) {
                    UsernamePasswordAuthenticationToken autenticacao =
                            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));

                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            }
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException excecao) {
            log.debug("Token rejeitado em {}: {}", requisicao.getRequestURI(), excecao.getMessage());
            SecurityContextHolder.clearContext();
        }

        cadeia.doFilter(requisicao, resposta);
    }
}
