package br.edu.unesc.gamevault.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.ItemPedidoRequest;
import br.edu.unesc.gamevault.dto.request.PedidoRequest;
import br.edu.unesc.gamevault.dto.response.PedidoResponse;
import br.edu.unesc.gamevault.entity.ItemPedido;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Pedido;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.entity.enums.StatusPedido;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.mapper.PedidoMapper;
import br.edu.unesc.gamevault.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final UsuarioService usuarioService;
    private final JogoService jogoService;
    private final PromocaoService promocaoService;
    private final BibliotecaService bibliotecaService;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarDoUsuario(Long usuarioId, StatusPedido status, Pageable paginacao) {
        usuarioService.buscarEntidade(usuarioId);

        Page<Pedido> pagina = status == null
                ? pedidoRepository.findByUsuario_Id(usuarioId, paginacao)
                : pedidoRepository.findByUsuario_IdAndStatus(usuarioId, status, paginacao);

        return pagina.map(pedidoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        return pedidoMapper.paraResposta(buscarComItens(id));
    }

    @Transactional
    public PedidoResponse criar(PedidoRequest requisicao) {
        Usuario usuario = usuarioService.buscarEntidade(requisicao.usuarioId());
        LocalDateTime agora = LocalDateTime.now();

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .dataPedido(agora)
                .status(StatusPedido.PENDENTE)
                .valorTotal(BigDecimal.ZERO)
                .build();

        Set<Long> jogosDoPedido = new LinkedHashSet<>();
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (ItemPedidoRequest itemRequisicao : requisicao.itens()) {
            Jogo jogo = jogoService.buscarEntidadeParaCompra(itemRequisicao.jogoId());

            if (!jogosDoPedido.add(jogo.getId())) {
                throw new RegraDeNegocioException(
                        "O jogo '%s' foi informado mais de uma vez no mesmo pedido"
                                .formatted(jogo.getTitulo()));
            }

            if (bibliotecaService.usuarioPossui(usuario.getId(), jogo.getId())) {
                throw new RegraDeNegocioException(
                        "O jogo '%s' já está na biblioteca deste usuário".formatted(jogo.getTitulo()));
            }

            BigDecimal desconto = promocaoService.calcularDesconto(jogo, agora);

            ItemPedido item = ItemPedido.builder()
                    .jogo(jogo)
                    .precoUnitario(jogo.getPreco())
                    .descontoAplicado(desconto)
                    .build();

            pedido.adicionarItem(item);
            valorTotal = valorTotal.add(item.getSubtotal());
        }

        pedido.setValorTotal(valorTotal);
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido criado: id={} usuario={} itens={} total={}", pedido.getId(), usuario.getId(),
                pedido.getItens().size(), valorTotal);

        return pedidoMapper.paraResposta(pedido);
    }

    @Transactional
    public PedidoResponse pagar(Long id, Long solicitanteId) {
        Pedido pedido = buscarComItens(id);
        validarDono(pedido, solicitanteId);

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new RegraDeNegocioException(
                    "Somente pedidos PENDENTE podem ser pagos; este está %s".formatted(pedido.getStatus()));
        }

        Usuario usuario = pedido.getUsuario();

        for (ItemPedido item : pedido.getItens()) {
            Jogo jogo = item.getJogo();

            if (!Boolean.TRUE.equals(jogo.getAtivo())) {
                throw new RegraDeNegocioException(
                        "O jogo '%s' foi desativado e o pedido não pode mais ser pago"
                                .formatted(jogo.getTitulo()));
            }

            if (bibliotecaService.usuarioPossui(usuario.getId(), jogo.getId())) {
                throw new RegraDeNegocioException(
                        "O jogo '%s' já está na biblioteca deste usuário".formatted(jogo.getTitulo()));
            }
        }

        usuarioService.debitarSaldo(usuario, pedido.getValorTotal());

        for (ItemPedido item : pedido.getItens()) {
            bibliotecaService.conceder(usuario, item.getJogo());
        }

        pedido.setStatus(StatusPedido.PAGO);
        log.info("Pedido {} pago: usuario={} valor={}", id, usuario.getId(), pedido.getValorTotal());

        return pedidoMapper.paraResposta(pedido);
    }

    @Transactional
    public PedidoResponse cancelar(Long id, Long solicitanteId) {
        Pedido pedido = buscarComItens(id);
        validarDono(pedido, solicitanteId);

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new RegraDeNegocioException(
                    "Somente pedidos PENDENTE podem ser cancelados; este está %s"
                            .formatted(pedido.getStatus()));
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        log.info("Pedido {} cancelado", id);

        return pedidoMapper.paraResposta(pedido);
    }

    @Transactional
    public void excluir(Long id, Long solicitanteId) {
        Pedido pedido = buscarComItens(id);
        validarDono(pedido, solicitanteId);

        if (pedido.getStatus() == StatusPedido.PAGO) {
            throw new RegraDeNegocioException("Um pedido pago não pode ser excluído");
        }

        pedidoRepository.delete(pedido);
        log.info("Pedido {} excluído", id);
    }

    private Pedido buscarComItens(Long id) {
        return pedidoRepository.buscarComItens(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    private void validarDono(Pedido pedido, Long solicitanteId) {
        if (solicitanteId == null) {
            return;
        }

        Usuario solicitante = usuarioService.buscarEntidade(solicitanteId);

        if (solicitante.getRole() == Role.ADMIN) {
            return;
        }

        if (!pedido.getUsuario().getId().equals(solicitanteId)) {
            throw new AcessoNegadoException(
                    "O usuário %d não é o dono do pedido %d".formatted(solicitanteId, pedido.getId()));
        }
    }
}
