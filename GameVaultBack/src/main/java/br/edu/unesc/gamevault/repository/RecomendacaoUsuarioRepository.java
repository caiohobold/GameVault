package br.edu.unesc.gamevault.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.RecomendacaoUsuario;

@Repository
public interface RecomendacaoUsuarioRepository extends JpaRepository<RecomendacaoUsuario, Long> {
    List<RecomendacaoUsuario> findByUsuarioIdOrderByPontuacaoDesc(Long usuarioId);
}
