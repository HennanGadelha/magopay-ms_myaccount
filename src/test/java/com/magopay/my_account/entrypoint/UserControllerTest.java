package com.magopay.my_account.entrypoint;

import com.magopay.my_account.core.application.ports.in.RegisterUserUseCase;
import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;
import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.entrypoint.dto.RegisterUserRequest;
import com.magopay.my_account.entrypoint.dto.RegisterUserResponse;
import com.magopay.my_account.entrypoint.mapper.UserWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private UserWebMapper userWebMapper;

    @InjectMocks
    private UserController controller;

    private static final UUID   ID       = UUID.randomUUID();
    private static final String NAME     = "João Silva";
    private static final String EMAIL    = "joao@exemplo.com";
    private static final String DOCUMENT = "12345678901";
    private static final String PASSWORD = "senha";

    private RegisterUserRequest request;
    private RegisterUserCommand command;
    private RegisterUserResult result;
    private RegisterUserResponse response;

    @BeforeEach
    void setUp() {
        request  = new RegisterUserRequest(NAME, EMAIL, DOCUMENT, PASSWORD);
        command  = new RegisterUserCommand(NAME, EMAIL, DOCUMENT, PASSWORD);
        result   = new RegisterUserResult(ID, NAME, EMAIL, DOCUMENT);
        response = new RegisterUserResponse(ID, NAME, EMAIL, DOCUMENT);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("deve retornar 201 com body e header de correlationId quando bem-sucedido")
        void shouldReturn201OnSuccess() {
            when(userWebMapper.toCommand(request)).thenReturn(command);
            when(registerUserUseCase.execute(command)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(request, "corr-id-123");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-id-123");
            assertThat(resp.getBody()).isNotNull();
        }

        @Test
        @DisplayName("deve gerar correlationId quando header nao for enviado (null)")
        void shouldGenerateCorrelationIdWhenNull() {
            when(userWebMapper.toCommand(request)).thenReturn(command);
            when(registerUserUseCase.execute(command)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(request, null);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isNotNull();
        }

        @Test
        @DisplayName("deve gerar correlationId quando header for vazio")
        void shouldGenerateCorrelationIdWhenBlank() {
            when(userWebMapper.toCommand(request)).thenReturn(command);
            when(registerUserUseCase.execute(command)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(request, "   ");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isNotNull();
        }

        @Test
        @DisplayName("deve propagar excecao e limpar MDC quando usecase lancar excecao")
        void shouldPropagateExceptionAndClearMdc() {
            when(userWebMapper.toCommand(any())).thenReturn(command);
            when(registerUserUseCase.execute(any())).thenThrow(new DomainException("erro"));

            assertThatThrownBy(() -> controller.register(request, "corr-id-123"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("erro");
        }

        @Test
        @DisplayName("branch: email nulo — mascara como <empty> e executa normalmente")
        void shouldHandleNullEmailMask() {
            RegisterUserRequest reqNullEmail = new RegisterUserRequest(NAME, null, DOCUMENT, PASSWORD);
            RegisterUserCommand cmd = new RegisterUserCommand(NAME, null, DOCUMENT, PASSWORD);
            when(userWebMapper.toCommand(reqNullEmail)).thenReturn(cmd);
            when(registerUserUseCase.execute(cmd)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(reqNullEmail, "corr-id");
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("branch: email com @ na posicao 1 — mascara como ***")
        void shouldHandleEmailAtIndexOne() {
            RegisterUserRequest reqShortEmail = new RegisterUserRequest(NAME, "a@exemplo.com", DOCUMENT, PASSWORD);
            RegisterUserCommand cmd = new RegisterUserCommand(NAME, "a@exemplo.com", DOCUMENT, PASSWORD);
            when(userWebMapper.toCommand(reqShortEmail)).thenReturn(cmd);
            when(registerUserUseCase.execute(cmd)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(reqShortEmail, "corr-id");
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("branch: document nulo — mascara como <empty>")
        void shouldHandleNullDocumentMask() {
            RegisterUserRequest reqNullDoc = new RegisterUserRequest(NAME, EMAIL, null, PASSWORD);
            RegisterUserCommand cmd = new RegisterUserCommand(NAME, EMAIL, null, PASSWORD);
            when(userWebMapper.toCommand(reqNullDoc)).thenReturn(cmd);
            when(registerUserUseCase.execute(cmd)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(reqNullDoc, "corr-id");
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("branch: document com menos de 4 chars — visible = length")
        void shouldHandleShortDocumentMask() {
            RegisterUserRequest reqShortDoc = new RegisterUserRequest(NAME, EMAIL, "abc", PASSWORD);
            RegisterUserCommand cmd = new RegisterUserCommand(NAME, EMAIL, "abc", PASSWORD);
            when(userWebMapper.toCommand(reqShortDoc)).thenReturn(cmd);
            when(registerUserUseCase.execute(cmd)).thenReturn(result);
            when(userWebMapper.toResponse(result)).thenReturn(response);

            ResponseEntity<RegisterUserResponse> resp = controller.register(reqShortDoc, "corr-id");
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }
}

