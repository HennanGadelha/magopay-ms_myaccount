package com.magopay.my_account.core.application.usecase;

import com.magopay.my_account.core.application.ports.in.query.FindUserByEmailQuery;
import com.magopay.my_account.core.application.ports.in.result.FindUserByEmailResult;
import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.core.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindUserByEmailService")
class FindUserByEmailServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private FindUserByEmailService service;

    private static final UUID   ID       = UUID.randomUUID();
    private static final String NAME     = "João Silva";
    private static final String EMAIL    = "joao@exemplo.com";
    private static final String DOCUMENT = "12345678901";
    private static final String HASH     = "$2a$hashed";

    @BeforeEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("deve retornar FindUserByEmailResult quando usuario for encontrado")
        void shouldReturnResultWhenUserFound() {
            User user = User.reconstitute(ID, NAME, EMAIL, DOCUMENT, HASH, UserStatus.IN_ANALYZING);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            FindUserByEmailResult result = service.execute(new FindUserByEmailQuery(EMAIL));

            assertThat(result.id()).isEqualTo(ID);
            assertThat(result.name()).isEqualTo(NAME);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.document()).isEqualTo(DOCUMENT);
            assertThat(result.status()).isEqualTo(UserStatus.IN_ANALYZING);
        }

        @Test
        @DisplayName("deve lancar UserNotFoundException quando usuario nao for encontrado")
        void shouldThrowUserNotFoundWhenEmpty() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new FindUserByEmailQuery(EMAIL)))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with given email");
        }

        @Test
        @DisplayName("deve propagar excecao quando repositorio lancar RuntimeException")
        void shouldPropagateRepositoryException() {
            when(userRepository.findByEmail(EMAIL)).thenThrow(new RuntimeException("db down"));

            assertThatThrownBy(() -> service.execute(new FindUserByEmailQuery(EMAIL)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("db down");
        }

        @Test
        @DisplayName("deve funcionar sem correlationId no MDC")
        void shouldWorkWithoutCorrelationId() {
            User user = User.reconstitute(ID, NAME, EMAIL, DOCUMENT, HASH, UserStatus.ACTIVE);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            FindUserByEmailResult result = service.execute(new FindUserByEmailQuery(EMAIL));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("deve funcionar com correlationId no MDC")
        void shouldWorkWithCorrelationId() {
            MDC.put("correlationId", "test-corr-id");
            User user = User.reconstitute(ID, NAME, EMAIL, DOCUMENT, HASH, UserStatus.INACTIVE);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            FindUserByEmailResult result = service.execute(new FindUserByEmailQuery(EMAIL));

            assertThat(result.status()).isEqualTo(UserStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("mascaramento de email (via execute)")
    class MaskEmail {

        @Test
        @DisplayName("branch: email nulo — maskEmail retorna <empty>")
        void shouldHandleNullEmail() {
            when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new FindUserByEmailQuery(null)))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("branch: email vazio — maskEmail retorna <empty>")
        void shouldHandleBlankEmail() {
            when(userRepository.findByEmail("   ")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new FindUserByEmailQuery("   ")))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("branch: email com @ na posicao 1 — maskEmail retorna ***")
        void shouldMaskEmailWithAtAtPositionOne() {
            User user = User.reconstitute(ID, NAME, "a@exemplo.com", DOCUMENT, HASH, UserStatus.IN_ANALYZING);
            when(userRepository.findByEmail("a@exemplo.com")).thenReturn(Optional.of(user));

            FindUserByEmailResult result = service.execute(new FindUserByEmailQuery("a@exemplo.com"));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("branch: email sem @ valido (atIndex == -1 ou < 0) — maskEmail retorna ***")
        void shouldMaskEmailWithNoAtSign() {
            when(userRepository.findByEmail("semArroba")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new FindUserByEmailQuery("semArroba")))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("branch: email com @ na ultima posicao — maskEmail retorna ***")
        void shouldMaskEmailWithAtAtLastPosition() {
            when(userRepository.findByEmail("joao@")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new FindUserByEmailQuery("joao@")))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}

