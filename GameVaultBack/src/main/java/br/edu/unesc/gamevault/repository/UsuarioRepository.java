package br.edu.unesc.gamevault.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Usuario> findByAtivoTrue(Pageable paginacao);

    Page<Usuario> findByRole(Role role, Pageable paginacao);

    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable paginacao);
}
