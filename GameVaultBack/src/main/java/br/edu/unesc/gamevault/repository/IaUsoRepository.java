package br.edu.unesc.gamevault.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.IaUso;

@Repository
public interface IaUsoRepository extends JpaRepository<IaUso, Long> {

    interface ResumoDeUso {
        String getModulo();

        java.time.LocalDate getDia();

        Long getChamadas();

        Long getFalhas();

        Long getTokensEntrada();

        Long getTokensSaida();

        Long getTokensCacheLeitura();
    }

    @Query(value = """
            select u.modulo                                   as modulo,
                   cast(u.criado_em as date)                  as dia,
                   count(*)                                   as chamadas,
                   count(*) filter (where not u.sucesso)      as falhas,
                   coalesce(sum(u.tokens_entrada), 0)         as tokensEntrada,
                   coalesce(sum(u.tokens_saida), 0)           as tokensSaida,
                   coalesce(sum(u.tokens_cache_leitura), 0)   as tokensCacheLeitura
              from ia_uso u
             where u.criado_em >= :desde
             group by u.modulo, cast(u.criado_em as date)
             order by dia desc, modulo
            """, nativeQuery = true)
    List<ResumoDeUso> resumirDesde(@Param("desde") LocalDateTime desde);
}
