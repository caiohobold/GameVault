package br.edu.unesc.gamevault.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.ItemPedido;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
    List<ItemPedido> findByPedido_Id(Long pedidoId);

    boolean existsByPedido_IdAndJogo_Id(Long pedidoId, Long jogoId);

    @Query("""
            select j.id            as jogoId,
                   j.titulo        as titulo,
                   count(i)        as quantidadeVendida,
                   sum(i.precoUnitario - i.descontoAplicado) as valorArrecadado
              from ItemPedido i
              join i.jogo j
              join i.pedido p
             where j.publicadora.id = :publicadoraId
               and p.status = br.edu.unesc.gamevault.entity.enums.StatusPedido.PAGO
             group by j.id, j.titulo
             order by sum(i.precoUnitario - i.descontoAplicado) desc
            """)
    Page<VendaProjecao> resumirVendasDaPublicadora(@Param("publicadoraId") Long publicadoraId,
            Pageable paginacao);

    interface VendaProjecao {
        Long getJogoId();

        String getTitulo();

        Long getQuantidadeVendida();

        java.math.BigDecimal getValorArrecadado();
    }
}
