package com.magopay.my_account.infra.aws;

import com.magopay.my_account.core.application.ports.out.UserEventPublisherPort;
import com.magopay.my_account.core.domain.event.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

@Component
public class AwsSqsUserEventPublisher implements UserEventPublisherPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSqsUserEventPublisher.class);
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final SqsClient sqsClient;
    private final tools.jackson.databind.ObjectMapper objectMapper;
    private final String queueUrl;

    public AwsSqsUserEventPublisher(SqsClient sqsClient, ObjectMapper objectMapper,
                                    @Value("${aws.sqs.queue.user-events:http://localhost:4566/000000000000/user-events-queue}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publish(UserCreatedEvent event) {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);

        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(request);

            LOGGER.info(
                    "event=user.event.published.sqs correlationId={} userId={} queueUrl={}",
                    correlationId,
                    event.getUserId(),
                    queueUrl
            );
        } catch (Exception e) {
            LOGGER.error(
                    "event=user.event.publish.failed correlationId={} userId={} reason={}",
                    correlationId,
                    event.getUserId(),
                    e.getMessage(),
                    e
            );
            throw new RuntimeException("Erro ao publicar evento de usuário criado", e);
        }
    }
}

