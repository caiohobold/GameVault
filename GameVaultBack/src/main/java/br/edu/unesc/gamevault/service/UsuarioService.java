package br.edu.unesc.gamevault.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.UsuarioAtivoRequest;
import br.edu.unesc.gamevault.dto.request.UsuarioAtualizacaoRequest;
import br.edu.unesc.gamevault.dto.request.UsuarioRequest;
import br.edu.unesc.gamevault.dto.response.UsuarioResponse;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.mapper.UsuarioMapper;
import br.edu.unesc.gamevault.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder codificadorDeSenha;

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String nome, Role role, Boolean apenasAtivos, Pageable paginacao) {
        Page<Usuario> pagina;

        if (nome != null && !nome.isBlank()) {
            pagina = usuarioRepository.findByNomeContainingIgnoreCase(nome.trim(), paginacao);
        } else if (role != null) {
            pagina = usuarioRepository.findByRole(role, paginacao);
        } else if (Boolean.TRUE.equals(apenasAtivos)) {
            pagina = usuarioRepository.findByAtivoTrue(paginacao);
        } else {
            pagina = usuarioRepository.findAll(paginacao);
        }

        return pagina.map(usuarioMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest requisicao) {
        String email = normalizarEmail(requisicao.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("Já existe um usuário cadastrado com o e-mail '%s'"
                    .formatted(email));
        }

        Usuario usuario = Usuario.builder()
                .nome(requisicao.nome().trim())
                .email(email)
                .senha(codificadorDeSenha.encode(requisicao.senha()))
                .role(requisicao.role())
                .saldo(BigDecimal.ZERO)
                .ativo(Boolean.TRUE)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario criado: id={} email={} role={}", usuario.getId(), usuario.getEmail(),
                usuario.getRole());

        return usuarioMapper.paraResposta(usuario);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest requisicao) {
        Usuario usuario = buscarEntidade(id);
        String email = normalizarEmail(requisicao.email());

        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new RecursoDuplicadoException("Já existe um usuário cadastrado com o e-mail '%s'"
                    .formatted(email));
        }

        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(email);
        usuario.setRole(requisicao.role());

        if (requisicao.senha() != null && !requisicao.senha().isBlank()) {
            usuario.setSenha(codificadorDeSenha.encode(requisicao.senha()));
        }

        log.info("Usuario atualizado: id={}", id);
        return usuarioMapper.paraResposta(usuario);
    }

    @Transactional
    public UsuarioResponse alterarAtivo(Long id, UsuarioAtivoRequest requisicao) {
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo(requisicao.ativo());

        log.info("Usuario id={} teve o campo ativo definido como {}", id, requisicao.ativo());
        return usuarioMapper.paraResposta(usuario);
    }

    @Transactional
    public void debitarSaldo(Usuario usuario, BigDecimal valor) {
        if (usuario.getSaldo().compareTo(valor) < 0) {
            throw new RegraDeNegocioException(
                    "Saldo insuficiente: a carteira tem R$ %s e o pedido custa R$ %s"
                            .formatted(usuario.getSaldo(), valor));
        }

        usuario.setSaldo(usuario.getSaldo().subtract(valor));
        log.info("Saldo debitado: usuario={} valor={} novoSaldo={}", usuario.getId(), valor,
                usuario.getSaldo());
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
