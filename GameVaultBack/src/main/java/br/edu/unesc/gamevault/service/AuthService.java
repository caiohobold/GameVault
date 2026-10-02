package br.edu.unesc.gamevault.service;

import java.math.BigDecimal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.LoginRequest;
import br.edu.unesc.gamevault.dto.request.RegistroRequest;
import br.edu.unesc.gamevault.dto.response.LoginResponse;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.mapper.UsuarioMapper;
import br.edu.unesc.gamevault.repository.UsuarioRepository;
import br.edu.unesc.gamevault.security.JwtService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder codificadorDeSenha;
    private final AuthenticationManager gerenciadorDeAutenticacao;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse registrar(RegistroRequest requisicao) {
        String email = normalizarEmail(requisicao.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException(
                    "Já existe um usuário cadastrado com o e-mail '%s'".formatted(email));
        }

        Role role = requisicao.role() == null ? Role.USUARIO : requisicao.role();

        // Auto-cadastro nunca cria administrador: isso seria escalonamento de privilégio.
        if (role == Role.ADMIN) {
            throw new RegraDeNegocioException(
                    "Contas de administrador não podem ser criadas por auto-cadastro");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(requisicao.nome().trim())
                .email(email)
                .senha(codificadorDeSenha.encode(requisicao.senha()))
                .role(role)
                .saldo(BigDecimal.ZERO)
                .ativo(Boolean.TRUE)
                .build());

        log.info("Usuario registrado via /auth/registrar: id={} email={} role={}",
                usuario.getId(), usuario.getEmail(), usuario.getRole());

        return montarResposta(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest requisicao) {
        String email = normalizarEmail(requisicao.email());

        // Credencial errada ou conta desativada lançam exceção aqui,
        // tratadas no GlobalExceptionHandler como 401 e 403.
        Authentication autenticacao = gerenciadorDeAutenticacao.authenticate(
                new UsernamePasswordAuthenticationToken(email, requisicao.senha()));

        UsuarioAutenticado autenticado = (UsuarioAutenticado) autenticacao.getPrincipal();

        log.info("Login realizado: id={} email={} role={}", autenticado.getId(),
                autenticado.getUsername(), autenticado.getRole());

        return montarResposta(autenticado.getUsuario());
    }

    private LoginResponse montarResposta(Usuario usuario) {
        String token = jwtService.gerarToken(usuario);

        return LoginResponse.de(token, jwtService.extrairExpiracao(token),
                usuarioMapper.paraResposta(usuario));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
