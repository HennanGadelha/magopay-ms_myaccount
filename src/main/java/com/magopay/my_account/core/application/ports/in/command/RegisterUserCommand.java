package com.magopay.my_account.core.application.ports.in.command;

public record RegisterUserCommand(
        String name,
        String email,
        String document,
        String password
) {
}

