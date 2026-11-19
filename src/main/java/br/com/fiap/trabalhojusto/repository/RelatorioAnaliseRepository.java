package br.com.fiap.trabalhojusto.repository;

import br.com.fiap.trabalhojusto.model.RelatorioAnalise;
import br.com.fiap.trabalhojusto.model.StatusAnalise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RelatorioAnaliseRepository extends JpaRepository<RelatorioAnalise, Long> {

    Page<RelatorioAnalise> findByVinculoId(Long vinculoId, Pageable pageable);

    Optional<RelatorioAnalise> findByIdAndVinculo_Usuario_Email(Long id, String email);

    /** Usado pelo consumidor da fila: já traz o vínculo para não depender de lazy loading. */
    @EntityGraph(attributePaths = "vinculo")
    Optional<RelatorioAnalise> findWithVinculoById(Long id);

    long countByStatus(StatusAnalise status);
}
