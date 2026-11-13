package br.edu.unesc.gamevault.ia.fraude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.dto.response.DeteccaoFraudeResponse;
import br.edu.unesc.gamevault.entity.AlertaFraude;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.StatusAlertaFraude;
import br.edu.unesc.gamevault.ia.ml.ClienteML;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.DeteccaoAnomaliasResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.UsuarioSuspeitoMl;
import br.edu.unesc.gamevault.mapper.UsuarioMapper;
import br.edu.unesc.gamevault.repository.AlertaFraudeRepository;
import br.edu.unesc.gamevault.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeteccaoFraudeServiceTest {
    @Mock
    private ClienteML clienteML;
    @Mock
    private AlertaFraudeRepository alertaRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private TransactionTemplate transacao;

    @InjectMocks
    private DeteccaoFraudeService deteccao;

    @BeforeEach
    void preparar() {
        when(transacao.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        when(usuarioService.buscarEntidade(anyLong())).thenAnswer(inv -> Usuario.builder().id(inv.getArgument(0)).build());
        when(clienteML.detectarAnomalias()).thenReturn(new DeteccaoAnomaliasResposta(List.of(
                new UsuarioSuspeitoMl(1L, 1.3, List.of("12 pedidos cancelados nos últimos 30 dias")),
                new UsuarioSuspeitoMl(2L, 0.7, List.of("Conta criada há 1 dias"))), 100, false));
    }

    @Test
    void criaAlertaAbertoComPontuacaoEntreZeroEUm() {
        DeteccaoFraudeResponse resposta = deteccao.detectar();

        ArgumentCaptor<AlertaFraude> alertas = ArgumentCaptor.forClass(AlertaFraude.class);
        verify(alertaRepository, org.mockito.Mockito.times(2)).save(alertas.capture());
        assertThat(resposta.alertasCriados()).isEqualTo(2);
        assertThat(alertas.getAllValues()).allSatisfy(alerta -> {
            assertThat(alerta.getStatus()).isEqualTo(StatusAlertaFraude.ABERTO);
            assertThat(alerta.getPontuacao()).isBetween(0.0, 1.0);
        });
    }

    @Test
    void naoRepeteAlertaDeQuemJaEstaAbertoOuConfirmado() {
        when(alertaRepository.existsByUsuario_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(true);

        DeteccaoFraudeResponse resposta = deteccao.detectar();

        assertThat(resposta.alertasCriados()).isEqualTo(1);
    }

    @Test
    void descartadoRecentementeNaoVoltaAoPainel() {
        when(alertaRepository.existsByUsuario_IdAndStatusAndRevisadoEmAfter(anyLong(),
                eq(StatusAlertaFraude.DESCARTADO), any())).thenReturn(true);

        assertThat(deteccao.detectar().alertasCriados()).isZero();
        verify(alertaRepository, never()).save(any());
    }
}
