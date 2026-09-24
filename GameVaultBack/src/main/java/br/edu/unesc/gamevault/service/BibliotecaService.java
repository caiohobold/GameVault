package br.edu.unesc.gamevault.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.BibliotecaHorasRequest;
import br.edu.unesc.gamevault.dto.response.BibliotecaResponse;
import br.edu.unesc.gamevault.entity.Biblioteca;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.mapper.BibliotecaMapper;
import br.edu.unesc.gamevault.repository.BibliotecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BibliotecaService {
    private final BibliotecaRepository bibliotecaRepository;
    private final UsuarioService usuarioService;
    private final BibliotecaMapper bibliotecaMapper;

    @Transactional(readOnly = true)
    public Page<BibliotecaResponse> listarDoUsuario(Long usuarioId, Pageable paginacao) {
        usuarioService.buscarEntidade(usuarioId);

        return bibliotecaRepository.findByUsuario_Id(usuarioId, paginacao)
                .map(bibliotecaMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public BibliotecaResponse buscarPorId(Long id) {
        return bibliotecaMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public BibliotecaResponse atualizarHoras(Long id, BibliotecaHorasRequest requisicao,
            Long solicitanteId) {
        Biblioteca entrada = buscarEntidade(id);
        validarDono(entrada, solicitanteId);

        entrada.setHorasJogadas(requisicao.horasJogadas());
        log.info("Horas jogadas atualizadas: biblioteca={} horas={}", id, requisicao.horasJogadas());

        return bibliotecaMapper.paraResposta(entrada);
    }

    @Transactional
    public void conceder(Usuario usuario, Jogo jogo) {
        if (bibliotecaRepository.existsByUsuario_IdAndJogo_Id(usuario.getId(), jogo.getId())) {
            log.warn("Jogo {} já estava na biblioteca do usuário {}; concessão ignorada",
                    jogo.getId(), usuario.getId());
            return;
        }

        Biblioteca entrada = Biblioteca.builder()
                .usuario(usuario)
                .jogo(jogo)
                .horasJogadas(0)
                .build();

        bibliotecaRepository.save(entrada);
        log.info("Jogo {} entregue na biblioteca do usuário {}", jogo.getId(), usuario.getId());
    }

    @Transactional(readOnly = true)
    public boolean usuarioPossui(Long usuarioId, Long jogoId) {
        return bibliotecaRepository.existsByUsuario_IdAndJogo_Id(usuarioId, jogoId);
    }

    @Transactional(readOnly = true)
    public Biblioteca buscarEntidade(Long id) {
        return bibliotecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrada de biblioteca", id));
    }

    private void validarDono(Biblioteca entrada, Long solicitanteId) {
        if (solicitanteId == null) {
            return;
        }

        Usuario solicitante = usuarioService.buscarEntidade(solicitanteId);

        if (solicitante.getRole() == Role.ADMIN) {
            return;
        }

        if (!entrada.getUsuario().getId().equals(solicitanteId)) {
            throw new AcessoNegadoException(
                    "O usuário %d não pode alterar a biblioteca de outro usuário".formatted(solicitanteId));
        }
    }
}
