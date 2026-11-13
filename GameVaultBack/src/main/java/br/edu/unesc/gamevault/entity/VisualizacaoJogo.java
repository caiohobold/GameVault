package br.edu.unesc.gamevault.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Última vez que um usuário logado abriu a página de um jogo. */
@Entity
@Table(name = "visualizacao_jogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisualizacaoJogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "jogo_id", nullable = false)
    private Long jogoId;

    @Column(name = "visualizado_em", nullable = false)
    private LocalDateTime visualizadoEm;
}
