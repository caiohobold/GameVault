package br.edu.unesc.gamevault.ia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.response.MetricaModeloResponse;
import br.edu.unesc.gamevault.entity.MetricaModeloIa;
import br.edu.unesc.gamevault.repository.MetricaModeloIaRepository;
import lombok.RequiredArgsConstructor;

/** Guarda e consulta as métricas de avaliação dos modelos treinados no GameVaultML. */
@Service
@RequiredArgsConstructor
public class MetricaModeloService {
    public static final String MODELO_RECOMENDACAO = "RECOMENDACAO";
    public static final String MODELO_DEMANDA = "DEMANDA";
    public static final String METRICA_HIT_RATE_10 = "HIT_RATE_10";
    /** Mesma medida para quem só recomendasse os mais vendidos: o modelo precisa ganhar dela. */
    public static final String METRICA_HIT_RATE_10_POPULARIDADE = "HIT_RATE_10_POPULARIDADE";
    public static final String METRICA_MAE = "MAE";

    private final MetricaModeloIaRepository metricaRepository;

    @Transactional
    public void registrar(String modelo, String metrica, Double valor, int amostras) {
        if (valor == null) {
            return;
        }

        metricaRepository.save(MetricaModeloIa.builder()
                .modelo(modelo)
                .metrica(metrica)
                .valor(valor)
                .amostras(amostras)
                .calculadaEm(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<MetricaModeloResponse> listarMaisRecentes() {
        return metricaRepository.buscarMaisRecentes().stream()
                .map(m -> new MetricaModeloResponse(m.getModelo(), m.getMetrica(), m.getValor(), m.getAmostras(),
                        m.getCalculadaEm()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Double> valorMaisRecente(String modelo, String metrica) {
        return metricaRepository.findFirstByModeloAndMetricaOrderByCalculadaEmDesc(modelo, metrica)
                .map(MetricaModeloIa::getValor);
    }
}
