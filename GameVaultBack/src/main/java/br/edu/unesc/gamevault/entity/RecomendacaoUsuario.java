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

/** Jogo recomendado a um usuário no último treino do modelo de recomendação. */
@Entity
@Table(name = "recomendacao_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecomendacaoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "jogo_id", nullable = false)
    private Long jogoId;

    @Column(name = "pontuacao", nullable = false)
    private Double pontuacao;

    @Column(name = "motivo_jogo_id")
    private Long motivoJogoId;

    @Column(name = "gerada_em", nullable = false)
    private LocalDateTime geradaEm;
}
