package com.magopay.my_account.core.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

