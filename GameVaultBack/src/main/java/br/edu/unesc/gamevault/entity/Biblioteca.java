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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "biblioteca",
        uniqueConstraints = @UniqueConstraint(name = "uk_biblioteca_usuario_jogo",
                columnNames = { "usuario_id", "jogo_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Biblioteca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogo_id", nullable = false)
    private Jogo jogo;

    @Column(name = "data_aquisicao", nullable = false)
    private LocalDateTime dataAquisicao;

    @Column(name = "horas_jogadas", nullable = false)
    private Integer horasJogadas;

    @PrePersist
    private void aoPersistir() {
        if (dataAquisicao == null) {
            dataAquisicao = LocalDateTime.now();
        }
        if (horasJogadas == null) {
            horasJogadas = 0;
        }
    }
}
