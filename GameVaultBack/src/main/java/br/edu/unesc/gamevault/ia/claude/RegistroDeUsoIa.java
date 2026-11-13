package br.edu.unesc.gamevault.ia.claude;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.entity.IaUso;
import br.edu.unesc.gamevault.repository.IaUsoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Grava cada chamada ao Claude em {@code ia_uso}. Roda numa transação própria para
 * que uma falha da chamada (ou do fluxo que a fez) não apague o registro, e um erro
 * ao gravar o registro nunca derrube o fluxo principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistroDeUsoIa {
    private static final int TAMANHO_MAXIMO_ERRO = 500;

    private final IaUsoRepository iaUsoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(ModuloIa modulo, String modelo, Long usuarioId, long tokensEntrada, long tokensSaida,
            long tokensCacheLeitura, long duracaoMs, boolean sucesso, String erro) {
        try {
            iaUsoRepository.save(IaUso.builder()
                    .modulo(modulo.name())
                    .modelo(modelo)
                    .usuarioId(usuarioId)
                    .tokensEntrada(tokensEntrada)
                    .tokensSaida(tokensSaida)
                    .tokensCacheLeitura(tokensCacheLeitura)
                    .duracaoMs(duracaoMs)
                    .sucesso(sucesso)
                    .erro(cortar(erro))
                    .criadoEm(LocalDateTime.now())
                    .build());
        } catch (RuntimeException excecao) {
            log.error("Nao foi possivel registrar o uso de IA do modulo {}", modulo, excecao);
        }
    }

    private String cortar(String texto) {
        if (texto == null || texto.length() <= TAMANHO_MAXIMO_ERRO) {
            return texto;
        }
        return texto.substring(0, TAMANHO_MAXIMO_ERRO);
    }
}
