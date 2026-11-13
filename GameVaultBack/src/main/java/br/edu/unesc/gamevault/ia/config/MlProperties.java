package br.edu.unesc.gamevault.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Endereço do serviço Python GameVaultML (prefixo {@code gamevault.ml}). */
@ConfigurationProperties(prefix = "gamevault.ml")
public record MlProperties(String url, int timeoutSegundos) {
}
