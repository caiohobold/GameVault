package br.edu.unesc.gamevault.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.Promocao;

@Repository
public interface PromocaoRepository extends JpaRepository<Promocao, Long> {
    Page<Promocao> findByJogo_Id(Long jogoId, Pageable paginacao);

    List<Promocao> findByJogo_IdAndAtivaTrue(Long jogoId);

    @Query("""
            select p from Promocao p
             where p.jogo.id = :jogoId
               and p.ativa = true
               and p.dataInicio <= :momento
               and p.dataFim > :momento
             order by p.percentualDesconto desc
             limit 1
            """)
    Optional<Promocao> buscarVigente(@Param("jogoId") Long jogoId,
            @Param("momento") LocalDateTime momento);
}
