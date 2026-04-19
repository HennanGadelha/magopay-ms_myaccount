package com.magopay.my_account.core.application.ports.out;

import com.magopay.my_account.core.domain.User;

public interface UserRepositoryPort {
    User save(User user);
}

