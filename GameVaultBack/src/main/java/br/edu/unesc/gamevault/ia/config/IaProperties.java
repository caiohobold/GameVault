package br.edu.unesc.gamevault.ia.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import br.edu.unesc.gamevault.ia.claude.ModuloIa;

/** Configuração dos módulos que usam o Claude (prefixo {@code gamevault.ia}). */
@ConfigurationProperties(prefix = "gamevault.ia")
public record IaProperties(
        boolean habilitada,
        String chaveApi,
        String modelo,
        Map<String, String> modelos,
        int timeoutSegundos,
        int maxTentativas,
        BigDecimal precoEntradaPorMilhao,
        BigDecimal precoSaidaPorMilhao,
        BigDecimal precoCacheLeituraPorMilhao,
        Map<String, Integer> limitePorHora,
        Resumo resumo) {

    public record Resumo(int minimoAvaliacoes, int novasParaRefazer) {
    }

    public boolean chaveConfigurada() {
        return chaveApi != null && !chaveApi.isBlank();
    }

    /** Modelo do módulo, ou o modelo padrão quando não há troca configurada. */
    public String modeloDe(ModuloIa modulo) {
        if (modelos != null && modelos.containsKey(modulo.name())) {
            return modelos.get(modulo.name());
        }
        return modelo;
    }

    public int limitePorHora(String recurso) {
        if (limitePorHora == null) {
            return Integer.MAX_VALUE;
        }
        return limitePorHora.getOrDefault(recurso, Integer.MAX_VALUE);
    }
}
