package com.magopay.my_account.core.application.ports.out;

import com.magopay.my_account.core.domain.User;

import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
}

