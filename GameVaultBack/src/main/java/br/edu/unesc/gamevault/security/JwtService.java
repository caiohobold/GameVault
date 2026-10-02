package br.edu.unesc.gamevault.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import br.edu.unesc.gamevault.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Geração, leitura e validação dos tokens JWT.
 * O segredo e o tempo de expiração vêm de variáveis de ambiente
 * (gamevault.jwt.secret e gamevault.jwt.expiracao).
 */
@Service
public class JwtService {
    private final SecretKey chave;
    private final long expiracaoEmMilissegundos;

    public JwtService(
            @Value("${gamevault.jwt.secret}") String segredo,
            @Value("${gamevault.jwt.expiracao}") long expiracaoEmMilissegundos) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "A chave JWT (gamevault.jwt.secret) precisa ter no mínimo 32 caracteres");
        }

        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoEmMilissegundos = expiracaoEmMilissegundos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plusMillis(expiracaoEmMilissegundos);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("role", usuario.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(chave)
                .compact();
    }

    public String extrairEmail(String token) {
        return lerClaims(token).getSubject();
    }

    public Instant extrairExpiracao(String token) {
        return lerClaims(token).getExpiration().toInstant();
    }

    public long getExpiracaoEmMilissegundos() {
        return expiracaoEmMilissegundos;
    }

    /**
     * O token é válido quando a assinatura confere, não expirou e pertence
     * ao usuário informado. Assinatura inválida ou token expirado fazem
     * {@link #lerClaims(String)} lançar exceção, tratada no filtro.
     */
    public boolean tokenValido(String token, UserDetails usuario) {
        Claims claims = lerClaims(token);

        boolean mesmoUsuario = claims.getSubject().equals(usuario.getUsername());
        boolean naoExpirou = claims.getExpiration().toInstant().isAfter(Instant.now());

        return mesmoUsuario && naoExpirou;
    }

    private Claims lerClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
