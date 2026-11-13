package br.edu.unesc.gamevault.ia.promocao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.edu.unesc.gamevault.dto.request.SimulacaoPromocaoRequest;
import br.edu.unesc.gamevault.dto.response.CenarioPromocaoResponse;
import br.edu.unesc.gamevault.dto.response.PrevisaoDemandaResponse;
import br.edu.unesc.gamevault.dto.response.SimulacaoPromocaoResponse;
import br.edu.unesc.gamevault.dto.response.TreinoDemandaResponse;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.MetricaModeloService;
import br.edu.unesc.gamevault.ia.ml.ClienteML;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.JanelaMl;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoDemandaRequisicao;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoDemandaResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoJanelaMl;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.TreinoDemandaResposta;
import br.edu.unesc.gamevault.mapper.JogoMapper;
import br.edu.unesc.gamevault.service.JogoService;
import br.edu.unesc.gamevault.service.PromocaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sugestão de promoções (módulo 6). O modelo de regressão do GameVaultML prevê as
 * vendas para cada percentual de desconto; aqui a receita é calculada em BigDecimal e o
 * cenário de maior receita vira a sugestão. Só a publicadora dona do jogo (ou um ADMIN)
 * simula (RN09), e a sugestão fica dentro de 1% a 90% (RN15). Criar a promoção continua
 * sendo decisão da publicadora, pelo POST /promocoes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacaoPromocaoService {
    private static final int PASSO_PERCENTUAL = 10;
    private static final int PERCENTUAL_MAXIMO = 90;
    private static final int MAXIMO_SEMANAS = 12;

    private final ClienteML clienteML;
    private final JogoService jogoService;
    private final PromocaoService promocaoService;
    private final JogoMapper jogoMapper;
    private final MetricaModeloService metricaService;

    public SimulacaoPromocaoResponse simular(SimulacaoPromocaoRequest requisicao, Long solicitanteId) {
        Jogo jogo = jogoService.buscarEntidadeDoDono(requisicao.jogoId(), solicitanteId);

        if (!requisicao.dataFim().isAfter(requisicao.dataInicio())) {
            throw new RegraDeNegocioException("A data de fim da promoção precisa ser posterior à data de início");
        }

        LocalDate inicio = requisicao.dataInicio().toLocalDate();
        LocalDate fim = requisicao.dataFim().toLocalDate();
        if (!fim.isAfter(inicio)) {
            fim = inicio.plusDays(1);
        }

        List<JanelaMl> janelas = new ArrayList<>();
        for (int percentual = 0; percentual <= PERCENTUAL_MAXIMO; percentual += PASSO_PERCENTUAL) {
            janelas.add(new JanelaMl(inicio, fim, percentual));
        }

        PrevisaoDemandaResposta previsao = clienteML.preverDemanda(new PrevisaoDemandaRequisicao(jogo.getId(), janelas));

        List<CenarioPromocaoResponse> semMarcacao = previsao.previsoes().stream()
                .map(p -> cenario(jogo.getPreco(), p, false))
                .toList();

        CenarioPromocaoResponse melhor = semMarcacao.stream()
                .max(Comparator.comparing(CenarioPromocaoResponse::receitaEstimada))
                .orElseThrow(() -> new IaIndisponivelException("O modelo não devolveu previsões"));

        Integer sugerido = melhor.percentualDesconto() > 0 ? melhor.percentualDesconto() : null;
        List<CenarioPromocaoResponse> cenarios = semMarcacao.stream()
                .map(c -> new CenarioPromocaoResponse(c.percentualDesconto(), c.precoComDesconto(), c.vendasPrevistas(),
                        c.receitaEstimada(), sugerido != null && c.percentualDesconto().equals(sugerido)))
                .toList();

        String recomendacao = sugerido != null
                ? "Com %d%% de desconto a receita prevista é a maior do período.".formatted(sugerido)
                : "Neste período a receita prevista é maior sem promoção.";

        log.info("Simulacao de promocao do jogo {}: sugerido={}%", jogo.getId(), sugerido);
        return new SimulacaoPromocaoResponse(jogoMapper.paraResumo(jogo), requisicao.dataInicio(),
                requisicao.dataFim(), cenarios, sugerido, recomendacao, previsao.mae());
    }

    public PrevisaoDemandaResponse preverProximasSemanas(Long jogoId, int semanasSolicitadas, Long solicitanteId) {
        Jogo jogo = jogoService.buscarEntidadeDoDono(jogoId, solicitanteId);
        int semanas = Math.max(1, Math.min(semanasSolicitadas, MAXIMO_SEMANAS));

        List<JanelaMl> janelas = new ArrayList<>();
        LocalDate inicio = LocalDate.now();
        for (int i = 0; i < semanas; i++) {
            LocalDate inicioDaSemana = inicio.plusWeeks(i);
            // Considera as promoções já cadastradas para aquela semana
            int percentual = promocaoService.percentualVigente(jogoId, inicioDaSemana.atStartOfDay());
            janelas.add(new JanelaMl(inicioDaSemana, inicioDaSemana.plusWeeks(1), percentual));
        }

        PrevisaoDemandaResposta previsao = clienteML.preverDemanda(new PrevisaoDemandaRequisicao(jogo.getId(), janelas));

        List<PrevisaoDemandaResponse.Semana> semanasPrevistas = previsao.previsoes().stream()
                .map(p -> new PrevisaoDemandaResponse.Semana(p.inicio(), p.fim(), p.percentualDesconto(),
                        arredondar(p.vendasPrevistas())))
                .toList();
        double total = semanasPrevistas.stream().mapToDouble(PrevisaoDemandaResponse.Semana::vendasPrevistas).sum();

        return new PrevisaoDemandaResponse(jogoMapper.paraResumo(jogo), semanasPrevistas, arredondar(total),
                previsao.mae());
    }

    public TreinoDemandaResponse treinar() {
        TreinoDemandaResposta resultado = clienteML.treinarDemanda();
        metricaService.registrar(MetricaModeloService.MODELO_DEMANDA, MetricaModeloService.METRICA_MAE,
                resultado.mae(), resultado.amostras());
        log.info("Modelo de demanda treinado: MAE={} com {} amostras", resultado.mae(), resultado.amostras());
        return new TreinoDemandaResponse(resultado.mae(), resultado.amostras());
    }

    @Scheduled(cron = "${gamevault.ia.demanda.cron}")
    public void treinarAgendado() {
        try {
            treinar();
        } catch (IaIndisponivelException excecao) {
            log.warn("Treino agendado do modelo de demanda nao rodou: {}", excecao.getMessage());
        }
    }

    /** Receita = preço com desconto × vendas previstas, sempre em BigDecimal. */
    static CenarioPromocaoResponse cenario(BigDecimal precoCheio, PrevisaoJanelaMl previsao, boolean sugerido) {
        BigDecimal precoComDesconto = precoCheio
                .multiply(BigDecimal.valueOf(100 - previsao.percentualDesconto()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        double vendas = arredondar(Math.max(0, previsao.vendasPrevistas()));
        BigDecimal receita = precoComDesconto.multiply(BigDecimal.valueOf(vendas)).setScale(2, RoundingMode.HALF_UP);

        return new CenarioPromocaoResponse(previsao.percentualDesconto(), precoComDesconto, vendas, receita, sugerido);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
