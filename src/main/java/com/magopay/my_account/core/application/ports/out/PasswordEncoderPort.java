package com.magopay.my_account.core.application.ports.out;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
}

