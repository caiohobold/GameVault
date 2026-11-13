package br.edu.unesc.gamevault.ia.promocao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.unesc.gamevault.dto.request.SimulacaoPromocaoRequest;
import br.edu.unesc.gamevault.dto.response.CenarioPromocaoResponse;
import br.edu.unesc.gamevault.dto.response.SimulacaoPromocaoResponse;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.MetricaModeloService;
import br.edu.unesc.gamevault.ia.ml.ClienteML;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoDemandaResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoJanelaMl;
import br.edu.unesc.gamevault.mapper.JogoMapper;
import br.edu.unesc.gamevault.service.JogoService;
import br.edu.unesc.gamevault.service.PromocaoService;

@ExtendWith(MockitoExtension.class)
class SimulacaoPromocaoServiceTest {
    @Mock
    private ClienteML clienteML;
    @Mock
    private JogoService jogoService;
    @Mock
    private PromocaoService promocaoService;
    @Mock
    private JogoMapper jogoMapper;
    @Mock
    private MetricaModeloService metricaService;

    @InjectMocks
    private SimulacaoPromocaoService simulacao;

    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private final SimulacaoPromocaoRequest requisicao = new SimulacaoPromocaoRequest(2L,
            INICIO.atStartOfDay(), INICIO.plusDays(7).atStartOfDay());

    @Test
    void receitaEhPrecoComDescontoVezesVendasEmBigDecimal() {
        CenarioPromocaoResponse cenario = SimulacaoPromocaoService.cenario(new BigDecimal("99.90"),
                new PrevisaoJanelaMl(INICIO, INICIO.plusDays(7), 30, 3.34), false);

        assertThat(cenario.precoComDesconto()).isEqualByComparingTo("69.93");
        assertThat(cenario.vendasPrevistas()).isEqualTo(3.3);
        assertThat(cenario.receitaEstimada()).isEqualByComparingTo("230.77");
    }

    @Test
    void sugereOPercentualDeMaiorReceita() {
        when(jogoService.buscarEntidadeDoDono(2L, 9L))
                .thenReturn(Jogo.builder().id(2L).titulo("Velocidade Terminal").preco(new BigDecimal("100.00")).build());
        // Vendas sobem com o desconto, mas a receita tem ponto máximo em 30%
        double[] vendas = { 1, 2, 2.6, 3.5, 3.6, 3.8, 4, 4.1, 4.2, 4.2 };
        List<PrevisaoJanelaMl> previsoes = IntStream.range(0, 10)
                .mapToObj(i -> new PrevisaoJanelaMl(INICIO, INICIO.plusDays(7), i * 10, vendas[i]))
                .toList();
        when(clienteML.preverDemanda(any())).thenReturn(new PrevisaoDemandaResposta(2L, previsoes, 0.5));

        SimulacaoPromocaoResponse resposta = simulacao.simular(requisicao, 9L);

        assertThat(resposta.percentualSugerido()).isEqualTo(30);
        assertThat(resposta.cenarios()).filteredOn(CenarioPromocaoResponse::sugerido).hasSize(1);
        assertThat(resposta.cenarios()).extracting(CenarioPromocaoResponse::percentualDesconto)
                .allMatch(p -> p >= 0 && p <= 90);
    }

    @Test
    void semPromocaoRendendoMaisNaoHaSugestao() {
        when(jogoService.buscarEntidadeDoDono(2L, 9L))
                .thenReturn(Jogo.builder().id(2L).titulo("Jogo").preco(new BigDecimal("100.00")).build());
        List<PrevisaoJanelaMl> previsoes = IntStream.range(0, 10)
                .mapToObj(i -> new PrevisaoJanelaMl(INICIO, INICIO.plusDays(7), i * 10, 5))
                .toList();
        when(clienteML.preverDemanda(any())).thenReturn(new PrevisaoDemandaResposta(2L, previsoes, 0.5));

        SimulacaoPromocaoResponse resposta = simulacao.simular(requisicao, 9L);

        assertThat(resposta.percentualSugerido()).isNull();
        assertThat(resposta.recomendacao()).contains("sem promoção");
    }

    @Test
    void publicadoraNaoSimulaJogoDeOutra() {
        when(jogoService.buscarEntidadeDoDono(2L, 9L)).thenThrow(new AcessoNegadoException("não é a dona"));

        assertThatThrownBy(() -> simulacao.simular(requisicao, 9L)).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(clienteML);
    }

    @Test
    void periodoInvertidoEhRecusadoAntesDeChamarOModelo() {
        when(jogoService.buscarEntidadeDoDono(2L, 9L))
                .thenReturn(Jogo.builder().id(2L).titulo("Jogo").preco(BigDecimal.TEN).build());
        SimulacaoPromocaoRequest invertida = new SimulacaoPromocaoRequest(2L, LocalDateTime.of(2026, 12, 8, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0));

        assertThatThrownBy(() -> simulacao.simular(invertida, 9L)).isInstanceOf(RegraDeNegocioException.class);
        verifyNoInteractions(clienteML);
    }
}
