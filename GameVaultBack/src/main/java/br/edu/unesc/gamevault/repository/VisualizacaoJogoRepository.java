package br.edu.unesc.gamevault.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.VisualizacaoJogo;

@Repository
public interface VisualizacaoJogoRepository extends JpaRepository<VisualizacaoJogo, Long> {
    Optional<VisualizacaoJogo> findByUsuarioIdAndJogoId(Long usuarioId, Long jogoId);

    List<VisualizacaoJogo> findTop10ByUsuarioIdOrderByVisualizadoEmDesc(Long usuarioId);
}
