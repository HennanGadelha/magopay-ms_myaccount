package com.magopay.my_account.infra.messaging.adapter;

import com.magopay.my_account.core.domain.event.UserCreatedEvent;
import com.magopay.my_account.infra.aws.AwsSqsUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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
        publisher = new AwsSqsUserEventPublisher(sqsClient, objectMapper, queueUrl);
        MDC.clear();
    }

    @Nested
    @DisplayName("publish()")
    class Publish {

        @Test
        @DisplayName("deve enviar mensagem para SQS com evento serializado")
        void shouldSendMessageToSqs() throws Exception {
            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");
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
        @DisplayName("deve serializar evento para JSON antes de enviar")
        void shouldSerializeEventToJson() throws Exception {
            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");
            String messageBody = "{\"userId\":\"" + userId + "\",\"name\":\"João Silva\"}";

            when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

            publisher.publish(event);

            verify(objectMapper).writeValueAsString(event);
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando falhar a serialização")
        void shouldThrowRuntimeExceptionOnSerializationError() throws Exception {
            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");

            when(objectMapper.writeValueAsString(event))
                    .thenThrow(new IllegalArgumentException("Serialization failed"));

            assertThatThrownBy(() -> publisher.publish(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao publicar evento de usuário criado");
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando falhar ao enviar para SQS")
        void shouldThrowRuntimeExceptionOnSqsError() throws Exception {
            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");
            String messageBody = "{\"userId\":\"" + userId + "\"}";

            when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenThrow(new RuntimeException("SQS connection error"));

            assertThatThrownBy(() -> publisher.publish(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao publicar evento de usuário criado");
        }

        @Test
        @DisplayName("deve enviar mensagem mesmo com correlationId ausente no MDC")
        void shouldSendMessageWithoutCorrelationId() throws Exception {
            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");
            String messageBody = "{\"userId\":\"" + userId + "\"}";

            when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

            publisher.publish(event);

            verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("deve enviar mensagem com correlationId presente no MDC")
        void shouldSendMessageWithCorrelationId() throws Exception {
            MDC.put("correlationId", "test-correlation-123");

            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");
            String messageBody = "{\"userId\":\"" + userId + "\"}";

            when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

            publisher.publish(event);

            verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        }

        @Test
        @DisplayName("deve usar URL da fila correta ao enviar mensagem")
        void shouldUseCorrectQueueUrl() throws Exception {
            UUID userId = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(userId, "João Silva", "12345678901");
            String messageBody = "{\"userId\":\"" + userId + "\"}";

            when(objectMapper.writeValueAsString(event)).thenReturn(messageBody);
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                    .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

            publisher.publish(event);

            ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
            verify(sqsClient).sendMessage(captor.capture());

            assertThat(captor.getValue().queueUrl()).isEqualTo(queueUrl);
        }
    }
}

