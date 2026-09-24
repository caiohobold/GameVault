package br.edu.unesc.gamevault.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.ItemPedidoResponse;
import br.edu.unesc.gamevault.dto.response.PedidoResponse;
import br.edu.unesc.gamevault.entity.ItemPedido;
import br.edu.unesc.gamevault.entity.Pedido;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PedidoMapper {
    private final UsuarioMapper usuarioMapper;
    private final JogoMapper jogoMapper;

    public PedidoResponse paraResposta(Pedido pedido) {
        List<ItemPedidoResponse> itens = pedido.getItens().stream()
                .map(this::paraRespostaDeItem)
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                usuarioMapper.paraResumo(pedido.getUsuario()),
                pedido.getDataPedido(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                itens);
    }

    public ItemPedidoResponse paraRespostaDeItem(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getId(),
                jogoMapper.paraResumo(item.getJogo()),
                item.getPrecoUnitario(),
                item.getDescontoAplicado(),
                item.getSubtotal());
    }
}
