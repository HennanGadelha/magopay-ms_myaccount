package com.magopay.my_account.entrypoint.mapper;

import com.magopay.my_account.entrypoint.dto.RegisterUserRequest;
import com.magopay.my_account.entrypoint.dto.RegisterUserResponse;
import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;
import org.springframework.stereotype.Component;

@Component
public class UserWebMapper {

    public RegisterUserCommand toCommand(RegisterUserRequest request) {
        return new RegisterUserCommand(
                request.name(),
                request.email(),
                request.document(),
                request.password()
        );
    }

    public RegisterUserResponse toResponse(RegisterUserResult result) {
        return new RegisterUserResponse(
                result.id(),
                result.name(),
                result.email(),
                result.document()
        );
    }
}


