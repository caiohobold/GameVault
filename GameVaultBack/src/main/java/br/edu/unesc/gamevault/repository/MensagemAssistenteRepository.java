package br.edu.unesc.gamevault.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.MensagemAssistente;

@Repository
public interface MensagemAssistenteRepository extends JpaRepository<MensagemAssistente, Long> {
    List<MensagemAssistente> findByConversaIdOrderByIdAsc(Long conversaId);

    long countByConversaId(Long conversaId);
}
