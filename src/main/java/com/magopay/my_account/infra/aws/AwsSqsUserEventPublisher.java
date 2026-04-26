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
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    private final int maxAttempts;
    private final long initialBackoffMs;

    public AwsSqsUserEventPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.queue.user-events:http://localhost:4566/000000000000/user-events-queue}") String queueUrl,
            @Value("${aws.sqs.publish.retry.max-attempts:3}") int maxAttempts,
            @Value("${aws.sqs.publish.retry.initial-backoff-ms:500}") long initialBackoffMs
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = Math.max(1L, initialBackoffMs);
    }

    @Override
    public void publish(UserCreatedEvent event) {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);

        try {
            String messageBody = objectMapper.writeValueAsString(event);
            publishWithRetry(event, messageBody, correlationId);
        } catch (Exception exception) {
            LOGGER.error(
                    "event=user.event.publish.failed correlationId={} userId={} reason={}",
                    correlationId,
                    event.getUserId(),
                    exception.getMessage(),
                    exception
            );
            throw new RuntimeException("Erro ao publicar evento de usuario criado", exception);
        }
    }

    private void publishWithRetry(UserCreatedEvent event, String messageBody, String correlationId) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                SendMessageRequest request = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .build();

                sqsClient.sendMessage(request);

                LOGGER.info(
                        "event=user.event.published.sqs correlationId={} userId={} queueUrl={} attempt={}",
                        correlationId,
                        event.getUserId(),
                        queueUrl,
                        attempt
                );
                return;
            } catch (Exception exception) {
                lastException = new RuntimeException("Erro ao publicar evento de usuario criado", exception);

                LOGGER.warn(
                        "event=user.event.publish.retry correlationId={} userId={} attempt={} maxAttempts={} reason={}",
                        correlationId,
                        event.getUserId(),
                        attempt,
                        maxAttempts,
                        exception.getMessage()
                );

                if (attempt < maxAttempts) {
                    sleepWithBackoff(attempt, correlationId, event);
                }
            }
        }

        LOGGER.error(
                "event=user.event.publish.failed correlationId={} userId={} reason=exhausted_retries maxAttempts={}",
                correlationId,
                event.getUserId(),
                maxAttempts,
                lastException
        );
        throw lastException == null ? new RuntimeException("Erro ao publicar evento de usuario criado") : lastException;
    }

    private void sleepWithBackoff(int attempt, String correlationId, UserCreatedEvent event) {
        long delay = initialBackoffMs * (1L << (attempt - 1));

        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    String.format(
                            "Thread interrompida durante backoff de publicacao SQS correlationId=%s userId=%s",
                            correlationId,
                            event.getUserId()
                    ),
                    interruptedException
            );
        }
    }
}
