package br.edu.unesc.gamevault.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.Pedido;
import br.edu.unesc.gamevault.entity.enums.StatusPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Page<Pedido> findByUsuario_Id(Long usuarioId, Pageable paginacao);

    Page<Pedido> findByUsuario_IdAndStatus(Long usuarioId, StatusPedido status, Pageable paginacao);

    Page<Pedido> findByStatus(StatusPedido status, Pageable paginacao);

    @Query("""
            select distinct p from Pedido p
              left join fetch p.itens i
              left join fetch i.jogo
             where p.id = :id
            """)
    Optional<Pedido> buscarComItens(@Param("id") Long id);
}
