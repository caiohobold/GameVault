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
@Table(name = "lista_desejos",
        uniqueConstraints = @UniqueConstraint(name = "uk_lista_desejos_usuario_jogo",
                columnNames = { "usuario_id", "jogo_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaDesejos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogo_id", nullable = false)
    private Jogo jogo;

    @Column(name = "data_adicao", nullable = false)
    private LocalDateTime dataAdicao;

    @PrePersist
    private void aoPersistir() {
        if (dataAdicao == null) {
            dataAdicao = LocalDateTime.now();
        }
    }
}
