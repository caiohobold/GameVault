package br.edu.unesc.gamevault.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.edu.unesc.gamevault.entity.MetricaModeloIa;

@Repository
public interface MetricaModeloIaRepository extends JpaRepository<MetricaModeloIa, Long> {

    /** Valor mais recente de cada par modelo + métrica. */
    @Query("""
            select m from MetricaModeloIa m
             where m.calculadaEm = (select max(x.calculadaEm) from MetricaModeloIa x
                                     where x.modelo = m.modelo and x.metrica = m.metrica)
             order by m.modelo, m.metrica
            """)
    List<MetricaModeloIa> buscarMaisRecentes();

    Optional<MetricaModeloIa> findFirstByModeloAndMetricaOrderByCalculadaEmDesc(String modelo, String metrica);
}
