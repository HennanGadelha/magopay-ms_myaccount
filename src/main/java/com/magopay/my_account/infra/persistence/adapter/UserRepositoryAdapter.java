package com.magopay.my_account.infra.persistence.adapter;

import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.infra.persistence.dto.UserRecord;
import com.magopay.my_account.infra.persistence.mapper.UserMapper;
import com.magopay.my_account.infra.persistence.query.UserPersistenceQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositoryAdapter.class);
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(JdbcTemplate jdbcTemplate, UserMapper userMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        String correlationId = currentCorrelationId();

        LOGGER.debug(
                "event=user.register.persistence.save.started correlationId={} userId={} status={}",
                correlationId,
                user.getId(),
                user.getStatus()
        );

        try {
            UserRecord record = userMapper.toDatabaseRecord(user);
            int rowsAffected = jdbcTemplate.update(
                    UserPersistenceQueries.INSERT_USER,
                    record.id(),
                    record.name(),
                    record.email(),
                    record.document(),
                    record.password(),
                    record.status()
            );

            if (rowsAffected == 0) {
                LOGGER.warn(
                        "event=user.register.persistence.save.no_rows correlationId={} userId={}",
                        correlationId,
                        user.getId()
                );
                throw new DomainException("Failed to save user");
            }

            LOGGER.info(
                    "event=user.register.persistence.save.completed correlationId={} userId={} rowsAffected={}",
                    correlationId,
                    user.getId(),
                    rowsAffected
            );
            return user;
        } catch (Exception ex) {
            LOGGER.error(
                    "event=user.register.persistence.save.failed correlationId={} userId={} reason={}",
                    correlationId,
                    user.getId(),
                    ex.getMessage(),
                    ex
            );
            throw new DomainException("Error saving user");
        }
    }

    private String currentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "<missing>" : correlationId;
    }
}
