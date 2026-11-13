package br.edu.unesc.gamevault.ia.claude;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Lê os system prompts de {@code resources/prompts} uma vez, na subida da aplicação.
 * Os prompts ficam versionados no Git e passam por revisão no PR como qualquer código.
 */
@Slf4j
@Component
public class CarregadorDePrompts {
    private final Map<ModuloIa, String> prompts = new EnumMap<>(ModuloIa.class);

    @PostConstruct
    void carregar() {
        for (ModuloIa modulo : ModuloIa.values()) {
            String caminho = "prompts/" + modulo.arquivoPrompt() + ".md";

            try (InputStream entrada = new ClassPathResource(caminho).getInputStream()) {
                prompts.put(modulo, new String(entrada.readAllBytes(), StandardCharsets.UTF_8).strip());
            } catch (IOException excecao) {
                throw new IllegalStateException("Prompt não encontrado: " + caminho, excecao);
            }
        }

        log.info("{} prompts de IA carregados", prompts.size());
    }

    public String de(ModuloIa modulo) {
        return prompts.get(modulo);
    }
}
