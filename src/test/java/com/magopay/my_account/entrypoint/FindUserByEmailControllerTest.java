package com.magopay.my_account.entrypoint;

import com.magopay.my_account.core.application.ports.in.FindUserByEmailUseCase;
import com.magopay.my_account.core.application.ports.in.RegisterUserUseCase;
import com.magopay.my_account.core.application.ports.in.query.FindUserByEmailQuery;
import com.magopay.my_account.core.application.ports.in.result.FindUserByEmailResult;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.core.domain.exception.UserNotFoundException;
import com.magopay.my_account.entrypoint.dto.FindUserByEmailResponse;
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
@DisplayName("UserController - findByEmail()")
class FindUserByEmailControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private FindUserByEmailUseCase findUserByEmailUseCase;

    @Mock
    private UserWebMapper userWebMapper;

    @InjectMocks
    private UserController controller;

    private static final UUID   ID       = UUID.randomUUID();
    private static final String NAME     = "João Silva";
    private static final String EMAIL    = "joao@exemplo.com";
    private static final String DOCUMENT = "12345678901";

    private FindUserByEmailResult result;
    private FindUserByEmailResponse response;

    @BeforeEach
    void setUp() {
        result   = new FindUserByEmailResult(ID, NAME, EMAIL, DOCUMENT, UserStatus.IN_ANALYZING);
        response = new FindUserByEmailResponse(ID, NAME, EMAIL, DOCUMENT, UserStatus.IN_ANALYZING);
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("deve retornar 200 com body e header de correlationId quando usuario encontrado")
        void shouldReturn200OnSuccess() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class))).thenReturn(result);
            when(userWebMapper.toFindByEmailResponse(result)).thenReturn(response);

            ResponseEntity<FindUserByEmailResponse> resp = controller.findByEmail(EMAIL, "corr-id-123");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-id-123");
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("deve gerar correlationId quando header for nulo")
        void shouldGenerateCorrelationIdWhenNull() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class))).thenReturn(result);
            when(userWebMapper.toFindByEmailResponse(result)).thenReturn(response);

            ResponseEntity<FindUserByEmailResponse> resp = controller.findByEmail(EMAIL, null);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isNotNull();
        }

        @Test
        @DisplayName("deve gerar correlationId quando header for vazio")
        void shouldGenerateCorrelationIdWhenBlank() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class))).thenReturn(result);
            when(userWebMapper.toFindByEmailResponse(result)).thenReturn(response);

            ResponseEntity<FindUserByEmailResponse> resp = controller.findByEmail(EMAIL, "   ");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
        }

        @Test
        @DisplayName("deve propagar UserNotFoundException e limpar MDC")
        void shouldPropagateUserNotFoundException() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class)))
                    .thenThrow(new UserNotFoundException("User not found with given email"));

            assertThatThrownBy(() -> controller.findByEmail(EMAIL, "corr-id"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("deve propagar DomainException em caso de erro de infra")
        void shouldPropagateDomainException() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class)))
                    .thenThrow(new DomainException("infra error"));

            assertThatThrownBy(() -> controller.findByEmail(EMAIL, "corr-id"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("infra error");
        }

        @Test
        @DisplayName("branch: email nulo — mascara como <empty> e executa normalmente")
        void shouldHandleNullEmailMask() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class))).thenReturn(result);
            when(userWebMapper.toFindByEmailResponse(result)).thenReturn(response);

            ResponseEntity<FindUserByEmailResponse> resp = controller.findByEmail(null, "corr-id");
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("branch: email com @ na posicao 1 — mascara como ***")
        void shouldHandleEmailAtIndexOne() {
            FindUserByEmailResult shortResult = new FindUserByEmailResult(ID, NAME, "a@exemplo.com", DOCUMENT, UserStatus.IN_ANALYZING);
            FindUserByEmailResponse shortResponse = new FindUserByEmailResponse(ID, NAME, "a@exemplo.com", DOCUMENT, UserStatus.IN_ANALYZING);
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class))).thenReturn(shortResult);
            when(userWebMapper.toFindByEmailResponse(shortResult)).thenReturn(shortResponse);

            ResponseEntity<FindUserByEmailResponse> resp = controller.findByEmail("a@exemplo.com", "corr-id");
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("branch: email com @ na ultima posicao — mascara como ***")
        void shouldHandleEmailAtLastPosition() {
            when(findUserByEmailUseCase.execute(any(FindUserByEmailQuery.class)))
                    .thenThrow(new UserNotFoundException("User not found with given email"));

            assertThatThrownBy(() -> controller.findByEmail("joao@", "corr-id"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}

