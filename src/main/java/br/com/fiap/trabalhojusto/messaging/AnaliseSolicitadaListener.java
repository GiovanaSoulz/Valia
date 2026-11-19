package br.com.fiap.trabalhojusto.messaging;

import br.com.fiap.trabalhojusto.service.AnaliseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumidor assíncrono: recebe o id do relatório e dispara o processamento. */
@Component
public class AnaliseSolicitadaListener {

    private static final Logger log = LoggerFactory.getLogger(AnaliseSolicitadaListener.class);

    private final AnaliseProcessor processor;

    public AnaliseSolicitadaListener(AnaliseProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = RabbitConfig.FILA_ANALISE_SOLICITADA)
    public void receber(String mensagem) {
        Long id;
        try {
            id = Long.valueOf(mensagem.trim());
        } catch (NumberFormatException e) {
            // mensagem inválida é descartada (não volta para a fila)
            log.warn("Mensagem inválida na fila de análises: {}", mensagem);
            return;
        }
        processor.processar(id);
    }
}
