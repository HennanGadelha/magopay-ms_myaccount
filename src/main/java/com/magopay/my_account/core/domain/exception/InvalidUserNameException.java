package com.magopay.my_account.core.domain.exception;

public class InvalidUserNameException extends DomainException {
    public InvalidUserNameException(String message) {
        super(message);
    }
}
