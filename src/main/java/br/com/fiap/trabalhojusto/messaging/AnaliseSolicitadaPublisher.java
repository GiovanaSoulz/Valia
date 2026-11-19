package br.com.fiap.trabalhojusto.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica o evento "análise solicitada". A mensagem é só o id do relatório (texto simples):
 * dados do trabalhador nunca trafegam pela fila.
 */
@Component
public class AnaliseSolicitadaPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AnaliseSolicitadaPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(Long relatorioId) {
        // exchange padrão ("") roteia direto para a fila de mesmo nome da routing key
        rabbitTemplate.convertAndSend("", RabbitConfig.FILA_ANALISE_SOLICITADA, String.valueOf(relatorioId));
    }
}
