package com.magopay.my_account.core.application.ports.out;

import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
    void updateStatus(UUID userId, UserStatus status);
    List<User> findByStatus(UserStatus status);
}

