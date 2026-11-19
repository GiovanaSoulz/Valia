package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.dto.RelatorioDto;
import br.com.fiap.trabalhojusto.exception.RecursoNaoEncontradoException;
import br.com.fiap.trabalhojusto.exception.RegraNegocioException;
import br.com.fiap.trabalhojusto.exception.ServicoIndisponivelException;
import br.com.fiap.trabalhojusto.messaging.AnaliseSolicitadaPublisher;
import br.com.fiap.trabalhojusto.model.RelatorioAnalise;
import br.com.fiap.trabalhojusto.model.StatusAnalise;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import br.com.fiap.trabalhojusto.repository.RelatorioAnaliseRepository;
import br.com.fiap.trabalhojusto.util.Paginacao;
import br.com.fiap.trabalhojusto.util.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

/**
 * Solicitação e consulta de relatórios. A análise em si roda de forma assíncrona:
 * o relatório nasce PENDENTE, um evento é publicado no RabbitMQ e o consumidor
 * ({@link AnaliseProcessor}) calcula, gera a explicação e marca como CONCLUIDO.
 */
@Service
public class AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);
    private static final long PERIODO_MAXIMO_DIAS = 366;

    private final RelatorioAnaliseRepository relatorios;
    private final VinculoService vinculoService;
    private final AnaliseSolicitadaPublisher publisher;

    public AnaliseService(RelatorioAnaliseRepository relatorios, VinculoService vinculoService,
                          AnaliseSolicitadaPublisher publisher) {
        this.relatorios = relatorios;
        this.vinculoService = vinculoService;
        this.publisher = publisher;
    }

    /**
     * Intencionalmente sem @Transactional: o relatório precisa estar commitado no banco
     * antes de o evento ser publicado, senão o consumidor poderia não encontrá-lo.
     */
    public RelatorioDto.Resumo solicitar(String email, Long vinculoId, RelatorioDto.Request req) {
        VinculoTrabalhista vinculo = vinculoService.obter(email, vinculoId);
        if (req.periodoFim().isBefore(req.periodoInicio())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "O fim do período não pode ser anterior ao início");
        }
        if (req.periodoFim().isAfter(Tempo.hoje())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "O período não pode terminar no futuro");
        }
        if (ChronoUnit.DAYS.between(req.periodoInicio(), req.periodoFim()) > PERIODO_MAXIMO_DIAS) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "O período máximo de análise é de 12 meses");
        }

        RelatorioAnalise relatorio = new RelatorioAnalise();
        relatorio.setVinculo(vinculo);
        relatorio.setPeriodoInicio(req.periodoInicio());
        relatorio.setPeriodoFim(req.periodoFim());
        relatorio.setStatus(StatusAnalise.PENDENTE);
        relatorio = relatorios.save(relatorio);

        try {
            publisher.publicar(relatorio.getId());
        } catch (RuntimeException e) {
            log.error("Falha ao publicar evento de análise {}", relatorio.getId(), e);
            relatorio.setStatus(StatusAnalise.ERRO);
            relatorio.setMensagemErro("Fila de processamento indisponível no momento");
            relatorios.save(relatorio);
            throw new ServicoIndisponivelException("Fila de processamento indisponível. Tente novamente em instantes.", e);
        }
        return RelatorioDto.Resumo.de(relatorio);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RelatorioDto.Resumo> listar(String email, Long vinculoId, int pagina, int tamanho) {
        vinculoService.obter(email, vinculoId);
        Pageable pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.DESC, "solicitadoEm"));
        return PaginaResponse.de(relatorios.findByVinculoId(vinculoId, pageable), RelatorioDto.Resumo::de);
    }

    @Transactional(readOnly = true)
    public RelatorioDto.Detalhe buscar(String email, Long relatorioId) {
        RelatorioAnalise r = relatorios.findByIdAndVinculo_Usuario_Email(relatorioId, email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório não encontrado"));
        return RelatorioDto.Detalhe.de(r);
    }
}
