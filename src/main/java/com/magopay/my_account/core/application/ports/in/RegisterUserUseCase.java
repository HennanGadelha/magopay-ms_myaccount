package com.magopay.my_account.core.application.ports.in;

import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;

public interface RegisterUserUseCase {
    RegisterUserResult execute(RegisterUserCommand command);
}
