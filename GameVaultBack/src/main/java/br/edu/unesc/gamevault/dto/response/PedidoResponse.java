package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.unesc.gamevault.entity.enums.StatusPedido;

public record PedidoResponse(
        Long id,
        UsuarioResumoResponse usuario,
        LocalDateTime dataPedido,
        StatusPedido status,
        BigDecimal valorTotal,
        List<ItemPedidoResponse> itens) {
}
