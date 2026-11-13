package br.edu.unesc.gamevault.ia.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.edu.unesc.gamevault.dto.response.AcaoPendenteResponse;
import br.edu.unesc.gamevault.dto.response.PedidoResponse;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.StatusPedido;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.ia.busca.BuscaSemanticaService;
import br.edu.unesc.gamevault.ia.recomendacao.RecomendacaoService;
import br.edu.unesc.gamevault.ia.resumo.ResumoAvaliacoesService;
import br.edu.unesc.gamevault.service.BibliotecaService;
import br.edu.unesc.gamevault.service.JogoService;
import br.edu.unesc.gamevault.service.ListaDesejosService;
import br.edu.unesc.gamevault.service.PedidoService;
import br.edu.unesc.gamevault.service.PromocaoService;
import br.edu.unesc.gamevault.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class FerramentasDoAssistenteTest {
    private static final Long USUARIO_DO_JWT = 5L;

    @Mock
    private BuscaSemanticaService buscaSemanticaService;
    @Mock
    private JogoService jogoService;
    @Mock
    private PromocaoService promocaoService;
    @Mock
    private PedidoService pedidoService;
    @Mock
    private BibliotecaService bibliotecaService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private ListaDesejosService listaDesejosService;
    @Mock
    private RecomendacaoService recomendacaoService;
    @Mock
    private ResumoAvaliacoesService resumoService;

    private FerramentasDoAssistente ferramentas;
    private final List<AcaoPendenteResponse> acoes = new ArrayList<>();

    @BeforeEach
    void preparar() {
        ferramentas = new FerramentasDoAssistente(buscaSemanticaService, jogoService, promocaoService, pedidoService,
                bibliotecaService, usuarioService, listaDesejosService, recomendacaoService, resumoService,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void ferramentasSaoEstritasESemParametrosAlemDosDeclarados() {
        List<Tool> definicoes = ferramentas.definicoes();

        assertThat(definicoes).hasSize(11);
        assertThat(definicoes).allSatisfy(ferramenta -> {
            assertThat(ferramenta.strict()).contains(true);
            assertThat(ferramenta.inputSchema()._additionalProperties())
                    .containsEntry("additionalProperties", JsonValue.from(false));
        });
    }

    @Test
    void consultaSempreODonoDoJwtMesmoQueOModeloInformeOutroUsuario() {
        when(pedidoService.listarDoUsuario(eq(USUARIO_DO_JWT), isNull(), any())).thenReturn(Page.empty());

        FerramentasDoAssistente.Resultado resultado = ferramentas.executar("meus_pedidos",
                JsonValue.from(Map.of("status", "TODOS", "usuarioId", 999)), USUARIO_DO_JWT, acoes);

        assertThat(resultado.erro()).isFalse();
        verify(pedidoService).listarDoUsuario(eq(USUARIO_DO_JWT), isNull(), any());
    }

    @Test
    void pedidoDeOutroUsuarioVoltaComoErroDaFerramenta() {
        when(pedidoService.buscarPorIdDoUsuario(42L, USUARIO_DO_JWT))
                .thenThrow(new AcessoNegadoException("O pedido 42 pertence a outro usuário"));

        FerramentasDoAssistente.Resultado resultado = ferramentas.executar("preparar_pagamento",
                JsonValue.from(Map.of("pedidoId", 42)), USUARIO_DO_JWT, acoes);

        assertThat(resultado.erro()).isTrue();
        assertThat(acoes).isEmpty();
    }

    @Test
    void prepararPagamentoNuncaPagaSoCriaAAcaoParaConfirmar() {
        when(pedidoService.buscarPorIdDoUsuario(42L, USUARIO_DO_JWT)).thenReturn(
                new PedidoResponse(42L, null, LocalDateTime.now(), StatusPedido.PENDENTE, new BigDecimal("149.90"), List.of()));
        when(usuarioService.buscarEntidade(USUARIO_DO_JWT))
                .thenReturn(Usuario.builder().id(USUARIO_DO_JWT).saldo(new BigDecimal("200.00")).build());

        FerramentasDoAssistente.Resultado resultado = ferramentas.executar("preparar_pagamento",
                JsonValue.from(Map.of("pedidoId", 42)), USUARIO_DO_JWT, acoes);

        assertThat(resultado.erro()).isFalse();
        assertThat(acoes).singleElement().satisfies(acao -> {
            assertThat(acao.tipo()).isEqualTo(FerramentasDoAssistente.ACAO_PAGAR_PEDIDO);
            assertThat(acao.pedidoId()).isEqualTo(42L);
        });
        verify(pedidoService, org.mockito.Mockito.never()).pagar(any(), any());
    }

    @Test
    void pedidoJaPagoNaoGeraAcao() {
        when(pedidoService.buscarPorIdDoUsuario(42L, USUARIO_DO_JWT)).thenReturn(
                new PedidoResponse(42L, null, LocalDateTime.now(), StatusPedido.PAGO, BigDecimal.TEN, List.of()));

        FerramentasDoAssistente.Resultado resultado = ferramentas.executar("preparar_pagamento",
                JsonValue.from(Map.of("pedidoId", 42)), USUARIO_DO_JWT, acoes);

        assertThat(resultado.erro()).isTrue();
        assertThat(acoes).isEmpty();
    }

    @Test
    void ferramentaDesconhecidaViraErro() {
        assertThat(ferramentas.executar("apagar_tudo", JsonValue.from(Map.of()), USUARIO_DO_JWT, acoes).erro()).isTrue();
    }
}
