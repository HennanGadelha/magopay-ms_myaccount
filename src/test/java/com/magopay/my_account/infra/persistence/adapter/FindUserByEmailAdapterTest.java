package com.magopay.my_account.infra.persistence.adapter;

import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.infra.persistence.dto.UserRecord;
import com.magopay.my_account.infra.persistence.mapper.UserMapper;
import com.magopay.my_account.infra.persistence.query.UserPersistenceQueries;
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
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindUserByEmailAdapterTest")
class FindUserByEmailAdapterTest {

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
    private static final String HASH     = "$2a$hash";

    private User user;
    private UserRecord record;

    @BeforeEach
    void setUp() {
        MDC.clear();
        user   = User.reconstitute(ID, NAME, EMAIL, DOCUMENT, HASH, UserStatus.IN_ANALYZING);
        record = new UserRecord(ID, NAME, EMAIL, DOCUMENT, HASH, "IN_ANALYZING");
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("deve retornar Optional com User quando encontrado")
        void shouldReturnUserWhenFound() {
            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq(EMAIL)
            )).thenReturn(List.of(record));
            when(userMapper.toDomainUser(record)).thenReturn(user);

            Optional<User> result = adapter.findByEmail(EMAIL);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(ID);
            assertThat(result.get().getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando usuario nao for encontrado")
        void shouldReturnEmptyWhenNotFound() {
            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq(EMAIL)
            )).thenReturn(Collections.emptyList());

            Optional<User> result = adapter.findByEmail(EMAIL);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve lancar DomainException quando jdbcTemplate lancar excecao")
        void shouldThrowDomainExceptionOnJdbcError() {
            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq(EMAIL)
            )).thenThrow(new RuntimeException("SQL error"));

            assertThatThrownBy(() -> adapter.findByEmail(EMAIL))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Error finding user by email");
        }

        @Test
        @DisplayName("deve normalizar email (trim + toLowerCase) antes de consultar")
        void shouldNormalizeEmail() {
            String rawEmail = "  JOAO@EXEMPLO.COM  ";
            String normalized = rawEmail.trim().toLowerCase();

            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq(normalized)
            )).thenReturn(List.of(record));
            when(userMapper.toDomainUser(record)).thenReturn(user);

            Optional<User> result = adapter.findByEmail(rawEmail);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("deve retornar Optional vazio para email nulo apos normalizar")
        void shouldHandleNullEmail() {
            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq("")
            )).thenReturn(Collections.emptyList());

            Optional<User> result = adapter.findByEmail(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve funcionar sem correlationId no MDC")
        void shouldWorkWithoutCorrelationId() {
            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq(EMAIL)
            )).thenReturn(List.of(record));
            when(userMapper.toDomainUser(record)).thenReturn(user);

            Optional<User> result = adapter.findByEmail(EMAIL);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("deve funcionar com correlationId no MDC")
        void shouldWorkWithCorrelationId() {
            MDC.put("correlationId", "test-corr-id");
            when(jdbcTemplate.query(
                    eq(UserPersistenceQueries.SELECT_BY_EMAIL),
                    any(RowMapper.class),
                    eq(EMAIL)
            )).thenReturn(List.of(record));
            when(userMapper.toDomainUser(record)).thenReturn(user);

            Optional<User> result = adapter.findByEmail(EMAIL);

            assertThat(result).isPresent();
        }
    }
}

