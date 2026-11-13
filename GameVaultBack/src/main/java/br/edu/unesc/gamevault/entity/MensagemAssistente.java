package br.edu.unesc.gamevault.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.edu.unesc.gamevault.entity.enums.PapelMensagem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma mensagem da conversa, guardada exatamente no formato da API do Claude
 * (blocos de texto, raciocínio, chamadas e resultados de ferramenta). O histórico só
 * recebe acréscimos: as mensagens são reenviadas sem alteração.
 */
@Entity
@Table(name = "mensagem_assistente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemAssistente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversa_id", nullable = false)
    private Long conversaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    private PapelMensagem papel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conteudo", nullable = false)
    private String conteudo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;
}
