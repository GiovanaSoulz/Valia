package br.com.fiap.trabalhojusto.model;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "irregularidades_detectadas")
@Getter
@Setter
@NoArgsConstructor
public class IrregularidadeDetectada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relatorio_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RelatorioAnalise relatorio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoIrregularidade tipo;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "valor_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorEstimado = BigDecimal.ZERO;
}
