package com.magopay.my_account.core.application.ports.in.result;

import com.magopay.my_account.core.domain.UserStatus;

import java.util.UUID;

public record FindUserByEmailResult(
        UUID id,
        String name,
        String email,
        String document,
        UserStatus status
) {
}

