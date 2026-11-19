package br.com.fiap.trabalhojusto.repository;

import br.com.fiap.trabalhojusto.model.IrregularidadeDetectada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IrregularidadeRepository extends JpaRepository<IrregularidadeDetectada, Long> {

    /** Agregado anonimizado para o gestor: [TipoIrregularidade, quantidade, soma dos valores]. */
    @Query("select i.tipo, count(i), sum(i.valorEstimado) from IrregularidadeDetectada i group by i.tipo")
    List<Object[]> resumoPorTipo();
}
