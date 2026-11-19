package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.dto.PagamentoDto;
import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.exception.RecursoNaoEncontradoException;
import br.com.fiap.trabalhojusto.exception.RegraNegocioException;
import br.com.fiap.trabalhojusto.model.Pagamento;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import br.com.fiap.trabalhojusto.repository.PagamentoRepository;
import br.com.fiap.trabalhojusto.util.Paginacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentos;
    private final VinculoService vinculoService;

    public PagamentoService(PagamentoRepository pagamentos, VinculoService vinculoService) {
        this.pagamentos = pagamentos;
        this.vinculoService = vinculoService;
    }

    @Transactional
    public PagamentoDto.Response criar(String email, Long vinculoId, PagamentoDto.Request req) {
        VinculoTrabalhista vinculo = vinculoService.obter(email, vinculoId);
        LocalDate competencia = req.competencia().withDayOfMonth(1);
        if (pagamentos.existsByVinculoIdAndCompetencia(vinculoId, competencia)) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe pagamento registrado para esta competência");
        }
        Pagamento p = new Pagamento();
        p.setVinculo(vinculo);
        aplicar(p, req, competencia);
        return PagamentoDto.Response.de(pagamentos.save(p));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PagamentoDto.Response> listar(String email, Long vinculoId, int pagina, int tamanho) {
        vinculoService.obter(email, vinculoId);
        Pageable pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.DESC, "competencia"));
        return PaginaResponse.de(pagamentos.findByVinculoId(vinculoId, pageable), PagamentoDto.Response::de);
    }

    @Transactional(readOnly = true)
    public PagamentoDto.Response buscar(String email, Long vinculoId, Long id) {
        vinculoService.obter(email, vinculoId);
        return PagamentoDto.Response.de(obterPagamento(vinculoId, id));
    }

    @Transactional
    public PagamentoDto.Response atualizar(String email, Long vinculoId, Long id, PagamentoDto.Request req) {
        vinculoService.obter(email, vinculoId);
        Pagamento p = obterPagamento(vinculoId, id);
        LocalDate competencia = req.competencia().withDayOfMonth(1);
        if (pagamentos.existsByVinculoIdAndCompetenciaAndIdNot(vinculoId, competencia, id)) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe pagamento registrado para esta competência");
        }
        aplicar(p, req, competencia);
        return PagamentoDto.Response.de(pagamentos.save(p));
    }

    @Transactional
    public void excluir(String email, Long vinculoId, Long id) {
        vinculoService.obter(email, vinculoId);
        pagamentos.delete(obterPagamento(vinculoId, id));
    }

    private Pagamento obterPagamento(Long vinculoId, Long id) {
        return pagamentos.findByIdAndVinculoId(id, vinculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado"));
    }

    private void aplicar(Pagamento p, PagamentoDto.Request req, LocalDate competencia) {
        p.setCompetencia(competencia);
        p.setValorPago(req.valorPago());
        p.setDataPagamento(req.dataPagamento());
    }
}
