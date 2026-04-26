package com.magopay.my_account.infra.persistence.adapter;

import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.infra.persistence.dto.UserRecord;
import com.magopay.my_account.infra.persistence.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter")
class UserRepositoryAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    private static final UUID   ID       = UUID.randomUUID();
    private static final String NAME     = "João Silva";
    private static final String EMAIL    = "joao@exemplo.com";
    private static final String DOCUMENT = "12345678901";
    private static final String PASSWORD = "$2a$hash";

    private User user;
    private UserRecord record;

    @BeforeEach
    void setUp() {
        MDC.clear();
        user = User.reconstitute(ID, NAME, EMAIL, DOCUMENT, PASSWORD, UserStatus.IN_ANALYZING);
        record = new UserRecord(ID, NAME, EMAIL, DOCUMENT, PASSWORD, "IN_ANALYZING");
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("deve retornar o User quando insert for bem-sucedido")
        void shouldReturnUserOnSuccess() {
            when(userMapper.toDatabaseRecord(user)).thenReturn(record);
            when(jdbcTemplate.update(eq("INSERT INTO users (id, name, email, document, password, status) VALUES (?, ?, ?, ?, ?, ?)"),
                    any(), any(), any(), any(), any(), any())).thenReturn(1);

            User result = adapter.save(user);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("deve lancar DomainException quando rowsAffected for zero")
        void shouldThrowWhenNoRowsAffected() {
            when(userMapper.toDatabaseRecord(user)).thenReturn(record);
            when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> adapter.save(user))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Error saving user");
        }

        @Test
        @DisplayName("deve lancar DomainException quando jdbcTemplate lancar excecao")
        void shouldThrowDomainExceptionOnJdbcError() {
            when(userMapper.toDatabaseRecord(user)).thenReturn(record);
            when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("SQL error"));

            assertThatThrownBy(() -> adapter.save(user))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Error saving user");
        }

        @Test
        @DisplayName("deve funcionar sem correlationId no MDC")
        void shouldWorkWithoutCorrelationId() {
            when(userMapper.toDatabaseRecord(user)).thenReturn(record);
            when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any(), any())).thenReturn(1);

            User result = adapter.save(user);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("deve funcionar com correlationId no MDC")
        void shouldWorkWithCorrelationId() {
            MDC.put("correlationId", "test-id");
            when(userMapper.toDatabaseRecord(user)).thenReturn(record);
            when(jdbcTemplate.update(any(String.class), any(), any(), any(), any(), any(), any())).thenReturn(1);

            User result = adapter.save(user);

            assertThat(result).isNotNull();
        }
    }
}

