package br.edu.unesc.gamevault.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "avaliacao",
        uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_usuario_jogo",
                columnNames = { "usuario_id", "jogo_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogo_id", nullable = false)
    private Jogo jogo;

    @Column(name = "nota", nullable = false)
    private Integer nota;

    @Column(name = "comentario", length = 1000)
    private String comentario;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao;

    /** Só APROVADA aparece na página do jogo e entra na nota média (RN08). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_moderacao", nullable = false, length = 20)
    private StatusModeracao statusModeracao;

    @Column(name = "motivo_moderacao", length = 500)
    private String motivoModeracao;

    @Column(name = "contem_spoiler", nullable = false)
    private Boolean contemSpoiler;

    @Column(name = "moderada_em")
    private LocalDateTime moderadaEm;

    /** Administrador que decidiu; vazio quando a decisão foi da IA. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderada_por_id")
    private Usuario moderadaPor;

    @PrePersist
    private void aoPersistir() {
        if (dataAvaliacao == null) {
            dataAvaliacao = LocalDateTime.now();
        }
        if (statusModeracao == null) {
            statusModeracao = StatusModeracao.APROVADA;
        }
        if (contemSpoiler == null) {
            contemSpoiler = Boolean.FALSE;
        }
    }
}
