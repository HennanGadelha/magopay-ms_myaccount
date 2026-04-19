package com.magopay.my_account.infra.persistence.mapper;

import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.infra.persistence.dto.UserRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper")
class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    private static final UUID   ID       = UUID.randomUUID();
    private static final String NAME     = "João Silva";
    private static final String EMAIL    = "joao@exemplo.com";
    private static final String DOCUMENT = "12345678901";
    private static final String PASSWORD = "$2a$hashed";

    @Test
    @DisplayName("toDatabaseRecord deve mapear todos os campos do User para UserRecord")
    void shouldMapUserToRecord() {
        User user = User.reconstitute(ID, NAME, EMAIL, DOCUMENT, PASSWORD, UserStatus.IN_ANALYZING);

        UserRecord record = mapper.toDatabaseRecord(user);

        assertThat(record.id()).isEqualTo(ID);
        assertThat(record.name()).isEqualTo(NAME);
        assertThat(record.email()).isEqualTo(EMAIL);
        assertThat(record.document()).isEqualTo(DOCUMENT);
        assertThat(record.password()).isEqualTo(PASSWORD);
        assertThat(record.status()).isEqualTo("IN_ANALYZING");
    }

    @Test
    @DisplayName("toDomainUser deve reconstituir User a partir de UserRecord")
    void shouldMapRecordToUser() {
        UserRecord record = new UserRecord(ID, NAME, EMAIL, DOCUMENT, PASSWORD, "ACTIVE");

        User user = mapper.toDomainUser(record);

        assertThat(user.getId()).isEqualTo(ID);
        assertThat(user.getName()).isEqualTo(NAME);
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getDocument()).isEqualTo(DOCUMENT);
        assertThat(user.getPassword()).isEqualTo(PASSWORD);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("toDomainUser deve reconstituir User com status INACTIVE")
    void shouldMapRecordWithInactiveStatus() {
        UserRecord record = new UserRecord(ID, NAME, EMAIL, DOCUMENT, PASSWORD, "INACTIVE");

        User user = mapper.toDomainUser(record);

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("toDomainUser deve reconstituir User com status IN_ANALYZING")
    void shouldMapRecordWithInAnalyzingStatus() {
        UserRecord record = new UserRecord(ID, NAME, EMAIL, DOCUMENT, PASSWORD, "IN_ANALYZING");

        User user = mapper.toDomainUser(record);

        assertThat(user.getStatus()).isEqualTo(UserStatus.IN_ANALYZING);
    }
}

