package br.edu.unesc.gamevault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.JogoRequest;
import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.dto.response.VendaResponse;
import br.edu.unesc.gamevault.entity.Categoria;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.mapper.JogoMapper;
import br.edu.unesc.gamevault.repository.ItemPedidoRepository;
import br.edu.unesc.gamevault.repository.JogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JogoService {
    private final JogoRepository jogoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final JogoMapper jogoMapper;

    @Transactional(readOnly = true)
    public Page<JogoResponse> listar(String titulo, Long categoriaId, Pageable paginacao) {
        Page<Jogo> pagina;

        if (titulo != null && !titulo.isBlank()) {
            pagina = jogoRepository.findByAtivoTrueAndTituloContainingIgnoreCase(titulo.trim(), paginacao);
        } else if (categoriaId != null) {
            pagina = jogoRepository.findByAtivoTrueAndCategorias_Id(categoriaId, paginacao);
        } else {
            pagina = jogoRepository.findByAtivoTrue(paginacao);
        }

        return pagina.map(jogoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public Page<JogoResponse> listarDaPublicadora(Long publicadoraId, Pageable paginacao) {
        return jogoRepository.findByPublicadora_Id(publicadoraId, paginacao).map(jogoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public JogoResponse buscarPorId(Long id) {
        return jogoMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public JogoResponse criar(JogoRequest requisicao) {
        Usuario publicadora = usuarioService.buscarEntidade(requisicao.publicadoraId());
        validarPapelDePublicadora(publicadora);

        Jogo jogo = Jogo.builder()
                .titulo(requisicao.titulo().trim())
                .descricao(normalizar(requisicao.descricao()))
                .preco(requisicao.preco())
                .dataLancamento(requisicao.dataLancamento())
                .publicadora(publicadora)
                .notaMedia(BigDecimal.ZERO)
                .totalAvaliacoes(0)
                .ativo(Boolean.TRUE)
                .categorias(carregarCategorias(requisicao.categoriaIds()))
                .build();

        jogo = jogoRepository.save(jogo);
        log.info("Jogo criado: id={} titulo={} publicadora={}", jogo.getId(), jogo.getTitulo(),
                publicadora.getId());

        return jogoMapper.paraResposta(jogo);
    }

    @Transactional
    public JogoResponse atualizar(Long id, JogoRequest requisicao, Long solicitanteId) {
        Jogo jogo = buscarEntidade(id);
        validarDono(jogo, solicitanteId);

        Usuario publicadora = usuarioService.buscarEntidade(requisicao.publicadoraId());
        validarPapelDePublicadora(publicadora);

        jogo.setTitulo(requisicao.titulo().trim());
        jogo.setDescricao(normalizar(requisicao.descricao()));
        jogo.setPreco(requisicao.preco());
        jogo.setDataLancamento(requisicao.dataLancamento());
        jogo.setPublicadora(publicadora);

        jogo.getCategorias().clear();
        jogo.getCategorias().addAll(carregarCategorias(requisicao.categoriaIds()));

        log.info("Jogo atualizado: id={}", id);
        return jogoMapper.paraResposta(jogo);
    }

    @Transactional
    public void desativar(Long id, Long solicitanteId) {
        Jogo jogo = buscarEntidade(id);
        validarDono(jogo, solicitanteId);

        jogo.setAtivo(Boolean.FALSE);
        log.info("Jogo desativado (soft delete): id={}", id);
    }

    @Transactional
    public JogoResponse reativar(Long id, Long solicitanteId) {
        Jogo jogo = buscarEntidade(id);
        validarDono(jogo, solicitanteId);

        jogo.setAtivo(Boolean.TRUE);
        log.info("Jogo reativado: id={}", id);

        return jogoMapper.paraResposta(jogo);
    }

    @Transactional(readOnly = true)
    public Page<VendaResponse> resumirVendas(Long publicadoraId, Pageable paginacao) {
        usuarioService.buscarEntidade(publicadoraId);

        return itemPedidoRepository.resumirVendasDaPublicadora(publicadoraId, paginacao)
                .map(projecao -> new VendaResponse(
                        projecao.getJogoId(),
                        projecao.getTitulo(),
                        projecao.getQuantidadeVendida(),
                        projecao.getValorArrecadado()));
    }

    @Transactional
    public void atualizarEstatisticas(Long jogoId, Double media, long total) {
        BigDecimal notaMedia = media == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP);

        Jogo jogo = buscarEntidade(jogoId);
        jogo.setNotaMedia(notaMedia);
        jogo.setTotalAvaliacoes((int) total);

        log.debug("Estatisticas do jogo {} recalculadas: media={} total={}", jogoId, notaMedia, total);
    }

    @Transactional(readOnly = true)
    public Jogo buscarEntidadeParaCompra(Long id) {
        Jogo jogo = buscarEntidade(id);

        if (!Boolean.TRUE.equals(jogo.getAtivo())) {
            throw new RegraDeNegocioException(
                    "O jogo '%s' está inativo e não pode ser comprado".formatted(jogo.getTitulo()));
        }

        return jogo;
    }

    @Transactional(readOnly = true)
    public Jogo buscarEntidade(Long id) {
        return jogoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogo", id));
    }

    private void validarDono(Jogo jogo, Long solicitanteId) {
        if (solicitanteId == null) {
            return;
        }

        Usuario solicitante = usuarioService.buscarEntidade(solicitanteId);

        if (solicitante.getRole() == Role.ADMIN) {
            return;
        }

        if (!jogo.getPublicadora().getId().equals(solicitanteId)) {
            throw new AcessoNegadoException(
                    "O usuário %d não é a publicadora do jogo '%s'".formatted(solicitanteId, jogo.getTitulo()));
        }
    }

    private void validarPapelDePublicadora(Usuario usuario) {
        if (usuario.getRole() != Role.PUBLICADORA && usuario.getRole() != Role.ADMIN) {
            throw new RegraDeNegocioException(
                    "O usuário '%s' não pode publicar jogos: o papel precisa ser PUBLICADORA ou ADMIN"
                            .formatted(usuario.getNome()));
        }
    }

    private Set<Categoria> carregarCategorias(Set<Long> categoriaIds) {
        Set<Categoria> categorias = new LinkedHashSet<>();

        for (Long categoriaId : categoriaIds) {
            categorias.add(categoriaService.buscarEntidade(categoriaId));
        }

        return categorias;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
