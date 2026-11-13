package br.edu.unesc.gamevault.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.ResumoAvaliacao;

@Repository
public interface ResumoAvaliacaoRepository extends JpaRepository<ResumoAvaliacao, Long> {
    Optional<ResumoAvaliacao> findByJogoId(Long jogoId);

    List<ResumoAvaliacao> findByJogoIdIn(Collection<Long> jogoIds);
}
