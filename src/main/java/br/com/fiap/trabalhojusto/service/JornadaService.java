package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.dto.JornadaDto;
import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.exception.RecursoNaoEncontradoException;
import br.com.fiap.trabalhojusto.exception.RegraNegocioException;
import br.com.fiap.trabalhojusto.model.RegistroJornada;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import br.com.fiap.trabalhojusto.repository.JornadaRepository;
import br.com.fiap.trabalhojusto.util.Paginacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JornadaService {

    private final JornadaRepository jornadas;
    private final VinculoService vinculoService;

    public JornadaService(JornadaRepository jornadas, VinculoService vinculoService) {
        this.jornadas = jornadas;
        this.vinculoService = vinculoService;
    }

    @Transactional
    public JornadaDto.Response criar(String email, Long vinculoId, JornadaDto.Request req) {
        VinculoTrabalhista vinculo = vinculoService.obter(email, vinculoId);
        validarHorarios(req);
        if (jornadas.existsByVinculoIdAndData(vinculoId, req.data())) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe jornada registrada nesta data para o vínculo");
        }
        RegistroJornada j = new RegistroJornada();
        j.setVinculo(vinculo);
        aplicar(j, req);
        return JornadaDto.Response.de(jornadas.save(j));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<JornadaDto.Response> listar(String email, Long vinculoId, int pagina, int tamanho) {
        vinculoService.obter(email, vinculoId);
        Pageable pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.DESC, "data"));
        return PaginaResponse.de(jornadas.findByVinculoId(vinculoId, pageable), JornadaDto.Response::de);
    }

    @Transactional(readOnly = true)
    public JornadaDto.Response buscar(String email, Long vinculoId, Long id) {
        vinculoService.obter(email, vinculoId);
        return JornadaDto.Response.de(obterJornada(vinculoId, id));
    }

    @Transactional
    public JornadaDto.Response atualizar(String email, Long vinculoId, Long id, JornadaDto.Request req) {
        vinculoService.obter(email, vinculoId);
        validarHorarios(req);
        RegistroJornada j = obterJornada(vinculoId, id);
        if (jornadas.existsByVinculoIdAndDataAndIdNot(vinculoId, req.data(), id)) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe jornada registrada nesta data para o vínculo");
        }
        aplicar(j, req);
        return JornadaDto.Response.de(jornadas.save(j));
    }

    @Transactional
    public void excluir(String email, Long vinculoId, Long id) {
        vinculoService.obter(email, vinculoId);
        jornadas.delete(obterJornada(vinculoId, id));
    }

    private RegistroJornada obterJornada(Long vinculoId, Long id) {
        return jornadas.findByIdAndVinculoId(id, vinculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jornada não encontrada"));
    }

    private void validarHorarios(JornadaDto.Request req) {
        if (req.horaEntrada().equals(req.horaSaida())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Hora de entrada e de saída não podem ser iguais");
        }
    }

    private void aplicar(RegistroJornada j, JornadaDto.Request req) {
        j.setData(req.data());
        j.setHoraEntrada(req.horaEntrada());
        j.setHoraSaida(req.horaSaida());
        j.setIntervaloMinutos(req.intervaloMinutos());
    }
}
