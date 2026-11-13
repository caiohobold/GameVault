package br.edu.unesc.gamevault.ia.config;

import java.time.Duration;
import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * Beans compartilhados pelos módulos de IA: o cliente oficial da Anthropic, o
 * cliente HTTP do GameVaultML e o executor das tarefas assíncronas.
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({ IaProperties.class, MlProperties.class })
public class IaConfig {
    /** Usada só para o SDK montar o cliente; nenhuma chamada sai sem chave configurada. */
    private static final String CHAVE_AUSENTE = "chave-nao-configurada";

    @Bean
    public AnthropicClient clienteAnthropic(IaProperties propriedades) {
        return AnthropicOkHttpClient.builder()
                .apiKey(propriedades.chaveConfigurada() ? propriedades.chaveApi() : CHAVE_AUSENTE)
                .timeout(Duration.ofSeconds(propriedades.timeoutSegundos()))
                .maxRetries(propriedades.maxTentativas())
                .build();
    }

    @Bean
    public RestClient clienteHttpMl(RestClient.Builder construtor, MlProperties propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(propriedades.timeoutSegundos()));

        return construtor
                .baseUrl(propriedades.url())
                .requestFactory(fabrica)
                .build();
    }

    @Bean(name = "executorIa")
    public Executor executorIa() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ia-");
        executor.initialize();
        return executor;
    }
}
