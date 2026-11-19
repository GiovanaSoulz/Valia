# VALIA — backend (Global Solution · Java Advanced · CitizenTech)

API REST em **Java 21 + Spring Boot** que ajuda trabalhadores (domésticos, informais e CLT) a identificar
**possíveis** irregularidades nas próprias condições de trabalho e a estimar **valores de referência**
que deixaram de receber. A mesma API é consumida pelo app Mobile.

> Os resultados são sinais e valores de referência, **não** parecer jurídico. Toda resposta de relatório traz esse aviso.

## Sumário

1. [Como funciona](#como-funciona)
2. [Opção escolhida no requisito 5](#opção-escolhida-no-requisito-5-a-mensageria-com-rabbitmq)
3. [Tecnologias](#tecnologias)
4. [Tutorial: do zero até a API rodando](#tutorial-do-zero-até-a-api-rodando)
5. [Tutorial: IA com Ollama](#tutorial-ia-com-ollama)
6. [Tutorial: usando o Swagger](#tutorial-usando-o-swagger)
7. [Usuários de teste](#usuários-de-teste)
8. [Perfis e autorização](#perfis-e-autorização)
9. [Principais endpoints](#principais-endpoints)
10. [Problemas comuns](#problemas-comuns)
11. [Modelagem, motor de regras, compliance e DevOps](#modelagem)

## Como funciona

1. A pessoa descreve a rotina em texto livre → **Spring AI** (modelo local via Ollama) extrai os campos (`POST /ia/interpretar-relato`) e o app pré-preenche vínculo e jornada.
2. Ela cadastra o **vínculo**, as **jornadas** e os **pagamentos** recebidos.
3. Solicita uma **análise** de um período (`POST /vinculos/{id}/analises`). A API responde `202` e publica um evento no **RabbitMQ**.
4. O consumidor roda o **motor de regras determinístico** (horas extras, intervalos, descanso entre jornadas, salário mínimo proporcional, diferença de pagamento) e usa a IA só para **explicar o resultado em linguagem simples**.
5. O app consulta `GET /analises/{id}` até o status ser `CONCLUIDO`.

A IA **nunca calcula nem decide**: o cálculo jurídico/financeiro é código determinístico, versionado e testado.

## Opção escolhida no requisito 5: A) Mensageria com RabbitMQ

Evento `análise solicitada` publicado por `AnaliseService` e consumido de forma assíncrona por `AnaliseSolicitadaListener`
(fila `valia.analise.solicitada`). A mensagem contém só o id do relatório; nenhum dado pessoal trafega pela fila.

## Tecnologias

Java 21, Spring Boot, Spring Data JPA (PostgreSQL), Spring Security + JWT (jjwt), **Spring AI (Ollama, modelo local)**,
Spring AMQP (RabbitMQ), Bean Validation, springdoc-openapi (Swagger), Lombok, JUnit 5, Docker, GitHub Actions.

---

## Tutorial: do zero até a API rodando

### O que instalar antes

| Programa | Para quê | Onde baixar |
|---|---|---|
| **JDK 21** | rodar o Java | https://adoptium.net |
| **Docker Desktop** | PostgreSQL e RabbitMQ | https://www.docker.com/products/docker-desktop |
| **Ollama** | IA local (veja o tutorial abaixo) | https://ollama.com/download |
| **IntelliJ IDEA** (opcional) | abrir
