package br.edu.unesc.gamevault.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.JogoSimilar;

@Repository
public interface JogoSimilarRepository extends JpaRepository<JogoSimilar, Long> {
    List<JogoSimilar> findByJogoIdOrderByPontuacaoDesc(Long jogoId);
}
