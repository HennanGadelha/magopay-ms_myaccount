package com.magopay.my_account.infra.persistence.dto;

import java.util.UUID;

public record UserRecord(
        UUID id,
        String name,
        String email,
        String document,
        String password,
        String status
) {
}

