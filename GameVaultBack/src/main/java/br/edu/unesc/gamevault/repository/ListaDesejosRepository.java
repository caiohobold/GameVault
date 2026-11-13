package br.edu.unesc.gamevault.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.ListaDesejos;

@Repository
public interface ListaDesejosRepository extends JpaRepository<ListaDesejos, Long> {
    Page<ListaDesejos> findByUsuario_Id(Long usuarioId, Pageable paginacao);

    Optional<ListaDesejos> findByUsuario_IdAndJogo_Id(Long usuarioId, Long jogoId);

    boolean existsByUsuario_IdAndJogo_Id(Long usuarioId, Long jogoId);

    @Query("select l.jogo.id from ListaDesejos l where l.usuario.id = :usuarioId")
    List<Long> buscarIdsDeJogosDoUsuario(@Param("usuarioId") Long usuarioId);
}
