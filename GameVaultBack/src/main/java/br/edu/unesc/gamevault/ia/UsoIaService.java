package br.edu.unesc.gamevault.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.response.UsoIaResponse;
import br.edu.unesc.gamevault.ia.config.IaProperties;
import br.edu.unesc.gamevault.repository.IaUsoRepository;
import br.edu.unesc.gamevault.repository.IaUsoRepository.ResumoDeUso;
import lombok.RequiredArgsConstructor;

/** Relatório de uso e custo estimado das chamadas ao Claude, a partir de {@code ia_uso}. */
@Service
@RequiredArgsConstructor
public class UsoIaService {
    private static final BigDecimal UM_MILHAO = BigDecimal.valueOf(1_000_000);

    private final IaUsoRepository iaUsoRepository;
    private final IaProperties propriedades;

    @Transactional(readOnly = true)
    public List<UsoIaResponse> resumir(int dias) {
        int periodo = Math.max(1, Math.min(dias, 365));
        LocalDateTime desde = LocalDateTime.now().minusDays(periodo);

        return iaUsoRepository.resumirDesde(desde).stream().map(this::paraResposta).toList();
    }

    private UsoIaResponse paraResposta(ResumoDeUso uso) {
        BigDecimal custo = custo(uso.getTokensEntrada(), propriedades.precoEntradaPorMilhao())
                .add(custo(uso.getTokensSaida(), propriedades.precoSaidaPorMilhao()))
                .add(custo(uso.getTokensCacheLeitura(), propriedades.precoCacheLeituraPorMilhao()))
                .setScale(4, RoundingMode.HALF_UP);

        return new UsoIaResponse(uso.getModulo(), uso.getDia(), uso.getChamadas(), uso.getFalhas(),
                uso.getTokensEntrada(), uso.getTokensSaida(), uso.getTokensCacheLeitura(), custo);
    }

    private static BigDecimal custo(Long tokens, BigDecimal precoPorMilhao) {
        if (tokens == null || precoPorMilhao == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(tokens).multiply(precoPorMilhao).divide(UM_MILHAO, 6, RoundingMode.HALF_UP);
    }
}
