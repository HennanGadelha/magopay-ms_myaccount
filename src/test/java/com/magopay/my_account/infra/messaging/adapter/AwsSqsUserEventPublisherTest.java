package com.magopay.my_account.infra.messaging.adapter;

import com.magopay.my_account.core.domain.event.UserCreatedEvent;
import com.magopay.my_account.infra.aws.AwsSqsUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsSqsUserEventPublisher")
class AwsSqsUserEventPublisherTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    private AwsSqsUserEventPublisher publisher;
    private final String queueUrl = "http://localhost:4566/000000000000/user-events-queue";

    @BeforeEach
    void setUp() {
        publisher = new AwsSqsUserEventPublisher(sqsClient, objectMapper, queueUrl, 3, 1);
        MDC.clear();
    }

    @Test
    @DisplayName("deve enviar mensagem para SQS com evento serializado")
    void shouldSendMessageToSqs() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(userId, "Joao Silva", "12345678901");
        String messageBody = "{\"userId\":\"" + userId + "\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        publisher.publish(event);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.queueUrl()).isEqualTo(queueUrl);
        assertThat(request.messageBody()).isEqualTo(messageBody);
    }

    @Test
    @DisplayName("deve tentar novamente quando envio ao SQS falhar")
    void shouldRetryWhenSqsFails() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(userId, "Joao Silva", "12345678901");
        String messageBody = "{\"userId\":\"" + userId + "\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("first failure"))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        publisher.publish(event);

        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("deve lancar RuntimeException quando retries se esgotarem")
    void shouldThrowRuntimeExceptionWhenRetriesExhausted() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(userId, "Joao Silva", "12345678901");
        String messageBody = "{\"userId\":\"" + userId + "\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS connection error"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao publicar evento de usuario criado");

        verify(sqsClient, times(3)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("deve lancar RuntimeException quando falhar a serializacao")
    void shouldThrowRuntimeExceptionOnSerializationError() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(userId, "Joao Silva", "12345678901");

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new IllegalArgumentException("Serialization failed"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao publicar evento de usuario criado");
    }
}
