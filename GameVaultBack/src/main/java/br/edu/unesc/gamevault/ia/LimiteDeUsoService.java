package br.edu.unesc.gamevault.ia;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.edu.unesc.gamevault.exception.LimiteDeUsoExcedidoException;
import br.edu.unesc.gamevault.ia.config.IaProperties;
import lombok.RequiredArgsConstructor;

/**
 * Limite de chamadas por hora, por usuário, nos recursos de IA que geram custo a cada
 * clique (chat e assistente de cadastro). Janela deslizante guardada em memória: basta
 * para uma instância só, que é o caso do projeto.
 */
@Service
@RequiredArgsConstructor
public class LimiteDeUsoService {
    public static final String CHAT = "chat";
    public static final String ASSISTENTE_CADASTRO = "assistente-cadastro";

    private static final Duration JANELA = Duration.ofHours(1);

    private final IaProperties propriedades;
    private final Map<String, Deque<Instant>> chamadas = new ConcurrentHashMap<>();

    public void registrarChamada(Long usuarioId, String recurso) {
        int limite = propriedades.limitePorHora(recurso);
        Deque<Instant> historico = chamadas.computeIfAbsent(usuarioId + ":" + recurso, chave -> new ArrayDeque<>());
        Instant agora = Instant.now();

        synchronized (historico) {
            while (!historico.isEmpty() && historico.peekFirst().isBefore(agora.minus(JANELA))) {
                historico.pollFirst();
            }

            if (historico.size() >= limite) {
                throw new LimiteDeUsoExcedidoException(
                        "Você atingiu o limite de %d usos por hora deste recurso. Tente novamente mais tarde."
                                .formatted(limite));
            }

            historico.addLast(agora);
        }
    }
}
