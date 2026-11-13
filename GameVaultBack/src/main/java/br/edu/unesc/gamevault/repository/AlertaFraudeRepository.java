package br.edu.unesc.gamevault.repository;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.AlertaFraude;
import br.edu.unesc.gamevault.entity.enums.StatusAlertaFraude;

@Repository
public interface AlertaFraudeRepository extends JpaRepository<AlertaFraude, Long> {
    @EntityGraph(attributePaths = { "usuario", "revisadoPor" })
    Page<AlertaFraude> findByStatus(StatusAlertaFraude status, Pageable paginacao);

    boolean existsByUsuario_IdAndStatus(Long usuarioId, StatusAlertaFraude status);

    boolean existsByUsuario_IdAndStatusIn(Long usuarioId, Collection<StatusAlertaFraude> status);

    boolean existsByUsuario_IdAndStatusAndRevisadoEmAfter(Long usuarioId, StatusAlertaFraude status,
            LocalDateTime momento);
}
