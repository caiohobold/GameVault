package br.edu.unesc.gamevault.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.ConversaAssistente;

@Repository
public interface ConversaAssistenteRepository extends JpaRepository<ConversaAssistente, Long> {
    List<ConversaAssistente> findTop20ByUsuarioIdOrderByAtualizadaEmDesc(Long usuarioId);
}
