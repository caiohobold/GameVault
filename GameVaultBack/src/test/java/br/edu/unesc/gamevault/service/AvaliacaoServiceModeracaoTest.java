package br.edu.unesc.gamevault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.dto.request.AvaliacaoRequest;
import br.edu.unesc.gamevault.entity.Avaliacao;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import br.edu.unesc.gamevault.evento.AvaliacaoAprovadaEvento;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.moderacao.ModeracaoService;
import br.edu.unesc.gamevault.ia.moderacao.ResultadoModeracao;
import br.edu.unesc.gamevault.mapper.AvaliacaoMapper;
import br.edu.unesc.gamevault.repository.AvaliacaoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvaliacaoServiceModeracaoTest {
    @Mock
    private AvaliacaoRepository avaliacaoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private JogoService jogoService;
    @Mock
    private BibliotecaService bibliotecaService;
    @Mock
    private AvaliacaoMapper avaliacaoMapper;
    @Mock
    private ModeracaoService moderacaoService;
    @Mock
    private TransactionTemplate transacao;
    @Mock
    private ApplicationEventPublisher publicadorDeEventos;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    private final AvaliacaoRequest requisicao = new AvaliacaoRequest(10L, 4, "Gostei bastante");

    @BeforeEach
    void preparar() {
        Usuario usuario = Usuario.builder().id(1L).nome("Ana").role(Role.USUARIO).saldo(BigDecimal.ZERO).build();
        Jogo jogo = Jogo.builder().id(10L).titulo("Fenda Profunda").preco(BigDecimal.TEN).build();

        when(usuarioService.buscarEntidade(1L)).thenReturn(usuario);
        when(jogoService.buscarEntidade(10L)).thenReturn(jogo);
        when(bibliotecaService.usuarioPossui(1L, 10L)).thenReturn(true);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transacao.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    void comentarioBloqueadoNaoEhGravadoEDevolve422() {
        when(moderacaoService.moderarComentario(any(), any(), any(), anyLong()))
                .thenReturn(new ResultadoModeracao(StatusModeracao.BLOQUEADA, false, "Ofensa a uma pessoa"));

        assertThatThrownBy(() -> avaliacaoService.criar(requisicao, 1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Ofensa a uma pessoa");

        verify(avaliacaoRepository, never()).save(any());
        verify(transacao, never()).execute(any());
    }

    @Test
    void emRevisaoEhGravadaMasNaoDisparaOResumo() {
        when(moderacaoService.moderarComentario(any(), any(), any(), anyLong()))
                .thenReturn(ResultadoModeracao.paraRevisaoHumana());

        avaliacaoService.criar(requisicao, 1L);

        ArgumentCaptor<Avaliacao> gravada = ArgumentCaptor.forClass(Avaliacao.class);
        verify(avaliacaoRepository).save(gravada.capture());
        assertThat(gravada.getValue().getStatusModeracao()).isEqualTo(StatusModeracao.EM_REVISAO);
        verify(publicadorDeEventos, never()).publishEvent(any());
    }

    @Test
    void aprovadaComSpoilerEhGravadaEAvisaOResumo() {
        when(moderacaoService.moderarComentario(any(), any(), any(), anyLong()))
                .thenReturn(new ResultadoModeracao(StatusModeracao.APROVADA, true, "Contém spoiler"));

        avaliacaoService.criar(requisicao, 1L);

        ArgumentCaptor<Avaliacao> gravada = ArgumentCaptor.forClass(Avaliacao.class);
        verify(avaliacaoRepository).save(gravada.capture());
        assertThat(gravada.getValue().getStatusModeracao()).isEqualTo(StatusModeracao.APROVADA);
        assertThat(gravada.getValue().getContemSpoiler()).isTrue();
        verify(publicadorDeEventos).publishEvent(new AvaliacaoAprovadaEvento(10L));
    }

    @Test
    void notaMediaSoContaAvaliacoesAprovadas() {
        when(moderacaoService.moderarComentario(any(), any(), any(), anyLong()))
                .thenReturn(ResultadoModeracao.aprovadaSemComentario());

        avaliacaoService.criar(requisicao, 1L);

        verify(avaliacaoRepository).calcularMediaDoJogo(eq(10L), eq(StatusModeracao.APROVADA));
        verify(avaliacaoRepository).countByJogo_IdAndStatusModeracao(eq(10L), eq(StatusModeracao.APROVADA));
    }
}
