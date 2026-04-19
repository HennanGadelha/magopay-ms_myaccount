package com.magopay.my_account.infra.persistence.mapper;

import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.infra.persistence.dto.UserRecord;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserRecord toDatabaseRecord(User user) {
        return new UserRecord(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDocument(),
                user.getPassword(),
                user.getStatus().name()
        );
    }

    public User toDomainUser(UserRecord record) {
        return User.create(
                record.id(),
                record.name(),
                record.email(),
                record.document(),
                record.password(),
                UserStatus.valueOf(record.status())
        );
    }
}

