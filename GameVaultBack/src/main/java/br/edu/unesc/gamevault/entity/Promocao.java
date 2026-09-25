package br.edu.unesc.gamevault.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promocao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogo_id", nullable = false)
    private Jogo jogo;

    @Column(name = "percentual_desconto", nullable = false)
    private Integer percentualDesconto;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDateTime dataFim;

    @Column(name = "ativa", nullable = false)
    private Boolean ativa;

    @PrePersist
    private void aoPersistir() {
        if (ativa == null) {
            ativa = Boolean.TRUE;
        }
    }

    public boolean estaVigenteEm(LocalDateTime momento) {
        return Boolean.TRUE.equals(ativa)
                && !momento.isBefore(dataInicio)
                && momento.isBefore(dataFim);
    }
}
