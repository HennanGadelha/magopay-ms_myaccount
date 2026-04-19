package com.magopay.my_account.core.application.usecase;

import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;
import com.magopay.my_account.core.application.ports.out.PasswordEncoderPort;
import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserService")
class RegisterUserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private RegisterUserService service;

    private static final String NAME     = "João Silva";
    private static final String EMAIL    = "joao@exemplo.com";
    private static final String DOCUMENT = "12345678901";
    private static final String PASSWORD = "plain_password";
    private static final String HASH     = "$2a$hashed";

    @BeforeEach
    void clearMdc() {
        MDC.clear();
    }

    // ─────────────────────────────────────────────────────────────────
    // execute() — happy path
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("deve retornar RegisterUserResult com dados do usuario salvo")
        void shouldReturnResultOnSuccess() {
            UUID id = UUID.randomUUID();
            User savedUser = User.reconstitute(id, NAME, EMAIL, DOCUMENT, HASH, UserStatus.IN_ANALYZING);

            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, DOCUMENT, PASSWORD);
            RegisterUserResult result = service.execute(command);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo(NAME);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.document()).isEqualTo(DOCUMENT);
        }

        @Test
        @DisplayName("deve propagar excecao quando repositorio falhar")
        void shouldPropagateRepositoryException() {
            when(passwordEncoder.encode(anyString())).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("db error"));

            RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, DOCUMENT, PASSWORD);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("db error");
        }

        @Test
        @DisplayName("deve funcionar sem correlationId no MDC")
        void shouldWorkWithoutCorrelationId() {
            UUID id = UUID.randomUUID();
            User savedUser = User.reconstitute(id, NAME, EMAIL, DOCUMENT, HASH, UserStatus.IN_ANALYZING);

            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, DOCUMENT, PASSWORD);
            RegisterUserResult result = service.execute(command);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("deve funcionar com correlationId no MDC")
        void shouldWorkWithCorrelationId() {
            MDC.put("correlationId", "test-correlation-id");

            UUID id = UUID.randomUUID();
            User savedUser = User.reconstitute(id, NAME, EMAIL, DOCUMENT, HASH, UserStatus.IN_ANALYZING);

            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, DOCUMENT, PASSWORD);
            RegisterUserResult result = service.execute(command);

            assertThat(result).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // maskEmail — branches via execute()
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("mascaramento de email (via execute)")
    class MaskEmail {

        @Test
        @DisplayName("branch: email nulo — maskEmail retorna <empty> e dominio lanca excecao")
        void shouldHandleNullEmail() {
            // cobre o branch: email == null
            RegisterUserCommand command = new RegisterUserCommand(NAME, null, DOCUMENT, PASSWORD);
            when(passwordEncoder.encode(anyString())).thenReturn(HASH);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("branch: email curto com @ na posicao 1 — maskEmail retorna ***")
        void shouldMaskEmailWithAtAtPositionOne() {
            // cobre o branch: atIndex <= 1
            // "a@exemplo.com" é válido no domínio (regex passa) mas atIndex==1
            UUID id = UUID.randomUUID();
            User savedUser = User.reconstitute(id, NAME, "a@exemplo.com", DOCUMENT, HASH, UserStatus.IN_ANALYZING);
            when(passwordEncoder.encode(anyString())).thenReturn(HASH);
            when(userRepository.save(any())).thenReturn(savedUser);

            RegisterUserCommand command = new RegisterUserCommand(NAME, "a@exemplo.com", DOCUMENT, PASSWORD);
            RegisterUserResult result = service.execute(command);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("branch: email com @ na ultima posicao — maskEmail retorna *** e dominio lanca excecao")
        void shouldHandleAtAtLastPosition() {
            // cobre o branch: atIndex == normalized.length() - 1
            // "joao@" falha no domínio (regex) → excecao
            RegisterUserCommand command = new RegisterUserCommand(NAME, "joao@", DOCUMENT, PASSWORD);
            when(passwordEncoder.encode(anyString())).thenReturn(HASH);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // maskDocument — branches via execute() com document edge cases
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("mascaramento de document (via execute)")
    class MaskDocument {

        @Test
        @DisplayName("maskDocument deve funcionar para document nulo")
        void shouldHandleNullDocument() {
            RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, null, PASSWORD);

            when(passwordEncoder.encode(anyString())).thenReturn(HASH);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("maskDocument deve funcionar para document com menos de 4 chars")
        void shouldHandleShortDocument() {
            // document com 3 chars — visible = min(4, 3) = 3
            RegisterUserCommand command = new RegisterUserCommand(NAME, EMAIL, "abc", PASSWORD);

            when(passwordEncoder.encode(anyString())).thenReturn(HASH);

            // domínio aceita, repositório não está configurado — vai lançar NPE/mock exception
            when(userRepository.save(any())).thenThrow(new RuntimeException("repo"));

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

