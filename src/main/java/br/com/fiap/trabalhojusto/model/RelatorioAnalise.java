package br.com.fiap.trabalhojusto.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "relatorios_analise")
@Getter
@Setter
@NoArgsConstructor
public class RelatorioAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private VinculoTrabalhista vinculo;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAnalise status = StatusAnalise.PENDENTE;

    @Column(name = "versao_regras", length = 20)
    private String versaoRegras;

    @Column(name = "diferenca_total_estimada", precision = 12, scale = 2)
    private BigDecimal diferencaTotalEstimada;

    @Column(name = "explicacao_simples", length = 4000)
    private String explicacaoSimples;

    @Column(name = "mensagem_erro", length = 500)
    private String mensagemErro;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm = LocalDateTime.now();

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @OneToMany(mappedBy = "relatorio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IrregularidadeDetectada> irregularidades = new ArrayList<>();

    public void adicionar(IrregularidadeDetectada irregularidade) {
        irregularidade.setRelatorio(this);
        irregularidades.add(irregularidade);
    }
}
