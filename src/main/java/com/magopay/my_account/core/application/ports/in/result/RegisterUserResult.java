package com.magopay.my_account.core.application.ports.in.result;

import java.util.UUID;

public record RegisterUserResult(
        UUID id,
        String name,
        String email,
        String document
) {
}

