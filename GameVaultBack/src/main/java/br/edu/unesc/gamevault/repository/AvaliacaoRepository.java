package br.edu.unesc.gamevault.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.Avaliacao;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Page<Avaliacao> findByJogo_Id(Long jogoId, Pageable paginacao);

    Page<Avaliacao> findByUsuario_Id(Long usuarioId, Pageable paginacao);

    Optional<Avaliacao> findByUsuario_IdAndJogo_Id(Long usuarioId, Long jogoId);

    boolean existsByUsuario_IdAndJogo_Id(Long usuarioId, Long jogoId);

    long countByJogo_Id(Long jogoId);

    @Query("select avg(a.nota) from Avaliacao a where a.jogo.id = :jogoId")
    Double calcularMediaDoJogo(@Param("jogoId") Long jogoId);
}
