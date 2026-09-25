package br.edu.unesc.gamevault.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descricao", length = 2000)
    private String descricao;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "data_lancamento")
    private LocalDate dataLancamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicadora_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_jogo_publicadora"))
    private Usuario publicadora;

    @Column(name = "nota_media", nullable = false, precision = 3, scale = 2)
    private BigDecimal notaMedia;

    @Column(name = "total_avaliacoes", nullable = false)
    private Integer totalAvaliacoes;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "jogo_categoria",
            joinColumns = @JoinColumn(name = "jogo_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    @Builder.Default
    private Set<Categoria> categorias = new LinkedHashSet<>();

    @PrePersist
    private void aoPersistir() {
        if (notaMedia == null) {
            notaMedia = BigDecimal.ZERO;
        }
        if (totalAvaliacoes == null) {
            totalAvaliacoes = 0;
        }
        if (ativo == null) {
            ativo = Boolean.TRUE;
        }
    }
}
