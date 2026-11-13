package br.edu.unesc.gamevault.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.Biblioteca;

@Repository
public interface BibliotecaRepository extends JpaRepository<Biblioteca, Long> {
    Page<Biblioteca> findByUsuario_Id(Long usuarioId, Pageable paginacao);

    Optional<Biblioteca> findByUsuario_IdAndJogo_Id(Long usuarioId, Long jogoId);

    boolean existsByUsuario_IdAndJogo_Id(Long usuarioId, Long jogoId);

    long countByJogo_Id(Long jogoId);

    @Query("select b.jogo.id from Biblioteca b where b.usuario.id = :usuarioId")
    List<Long> buscarIdsDeJogosDoUsuario(@Param("usuarioId") Long usuarioId);
}
