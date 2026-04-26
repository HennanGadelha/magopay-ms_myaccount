package com.magopay.my_account.entrypoint.dto;

public record RegisterUserRequest(
        String name,
        String email,
        String document,
        String password
) {
}


