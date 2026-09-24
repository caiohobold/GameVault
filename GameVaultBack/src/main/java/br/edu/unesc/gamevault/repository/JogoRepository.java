package br.edu.unesc.gamevault.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.Jogo;

@Repository
public interface JogoRepository extends JpaRepository<Jogo, Long> {
    Page<Jogo> findByAtivoTrue(Pageable paginacao);

    Page<Jogo> findByAtivoTrueAndTituloContainingIgnoreCase(String titulo, Pageable paginacao);

    Page<Jogo> findByAtivoTrueAndCategorias_Id(Long categoriaId, Pageable paginacao);

    Page<Jogo> findByPublicadora_Id(Long publicadoraId, Pageable paginacao);

    Optional<Jogo> findByIdAndAtivoTrue(Long id);

    @Query("select j from Jogo j left join fetch j.categorias where j.id = :id")
    Optional<Jogo> buscarComCategorias(@Param("id") Long id);
}
