package br.com.fiap.trabalhojusto.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String FILA_ANALISE_SOLICITADA = "trabalhojusto.analise.solicitada";

    @Bean
    public Queue filaAnaliseSolicitada() {
        return QueueBuilder.durable(FILA_ANALISE_SOLICITADA).build();
    }
}
