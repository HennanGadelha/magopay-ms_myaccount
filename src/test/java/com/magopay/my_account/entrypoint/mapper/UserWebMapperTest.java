package com.magopay.my_account.entrypoint.mapper;

import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;
import com.magopay.my_account.entrypoint.dto.RegisterUserRequest;
import com.magopay.my_account.entrypoint.dto.RegisterUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserWebMapper")
class UserWebMapperTest {

    private final UserWebMapper mapper = new UserWebMapper();

    @Test
    @DisplayName("toCommand deve mapear RegisterUserRequest para RegisterUserCommand")
    void shouldMapRequestToCommand() {
        RegisterUserRequest request = new RegisterUserRequest("João", "joao@ex.com", "12345678901", "senha");

        RegisterUserCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("João");
        assertThat(command.email()).isEqualTo("joao@ex.com");
        assertThat(command.document()).isEqualTo("12345678901");
        assertThat(command.password()).isEqualTo("senha");
    }

    @Test
    @DisplayName("toResponse deve mapear RegisterUserResult para RegisterUserResponse")
    void shouldMapResultToResponse() {
        UUID id = UUID.randomUUID();
        RegisterUserResult result = new RegisterUserResult(id, "João", "joao@ex.com", "12345678901");

        RegisterUserResponse response = mapper.toResponse(result);

        assertThat(response.getName()).isEqualTo("João");
        assertThat(response.getEmail()).isEqualTo("joao@ex.com");
        assertThat(response.getDocument()).isEqualTo("12345678901");
    }
}


