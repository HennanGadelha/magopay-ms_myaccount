package com.magopay.my_account.infra.persistence.adapter;

import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.infra.persistence.dto.UserRecord;
import com.magopay.my_account.infra.persistence.mapper.UserMapper;
import com.magopay.my_account.infra.persistence.query.UserPersistenceQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<User> findByEmail(String email) {
        String correlationId = currentCorrelationId();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        LOGGER.debug(
                "event=user.findByEmail.persistence.started correlationId={}",
                correlationId
        );

        try {
            List<UserRecord> records = jdbcTemplate.query(
                    UserPersistenceQueries.SELECT_BY_EMAIL,
                    (rs, rowNum) -> new UserRecord(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("document"),
                            rs.getString("password"),
                            rs.getString("status")
                    ),
                    normalizedEmail
            );

            if (records.isEmpty()) {
                LOGGER.info(
                        "event=user.findByEmail.persistence.not_found correlationId={}",
                        correlationId
                );
                return Optional.empty();
            }

            User user = userMapper.toDomainUser(records.get(0));
            LOGGER.info(
                    "event=user.findByEmail.persistence.found correlationId={} userId={}",
                    correlationId,
                    user.getId()
            );
            return Optional.of(user);
        } catch (Exception ex) {
            LOGGER.error(
                    "event=user.findByEmail.persistence.failed correlationId={} reason={}",
                    correlationId,
                    ex.getMessage(),
                    ex
            );
            throw new DomainException("Error finding user by email");
        }
    }

    @Override
    public void updateStatus(UUID userId, UserStatus status) {
        String correlationId = currentCorrelationId();

        LOGGER.debug(
                "event=user.persistence.updateStatus.started correlationId={} userId={} newStatus={}",
                correlationId,
                userId,
                status
        );

        try {
            int rowsAffected = jdbcTemplate.update(
                    UserPersistenceQueries.UPDATE_STATUS,
                    status.name(),
                    userId
            );

            if (rowsAffected == 0) {
                LOGGER.warn(
                        "event=user.persistence.updateStatus.no_rows correlationId={} userId={}",
                        correlationId,
                        userId
                );
            } else {
                LOGGER.info(
                        "event=user.persistence.updateStatus.completed correlationId={} userId={} newStatus={}",
                        correlationId,
                        userId,
                        status
                );
            }
        } catch (Exception ex) {
            LOGGER.error(
                    "event=user.persistence.updateStatus.failed correlationId={} userId={} reason={}",
                    correlationId,
                    userId,
                    ex.getMessage(),
                    ex
            );
            throw new DomainException("Error updating user status");
        }
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        String correlationId = currentCorrelationId();

        LOGGER.debug(
                "event=user.persistence.findByStatus.started correlationId={} status={}",
                correlationId,
                status
        );

        try {
            List<UserRecord> records = jdbcTemplate.query(
                    UserPersistenceQueries.SELECT_BY_STATUS,
                    (rs, rowNum) -> new UserRecord(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("document"),
                            rs.getString("password"),
                            rs.getString("status")
                    ),
                    status.name()
            );

            List<User> users = records.stream()
                    .map(userMapper::toDomainUser)
                    .toList();

            LOGGER.info(
                    "event=user.persistence.findByStatus.completed correlationId={} status={} count={}",
                    correlationId,
                    status,
                    users.size()
            );

            return users;
        } catch (Exception ex) {
            LOGGER.error(
                    "event=user.persistence.findByStatus.failed correlationId={} status={} reason={}",
                    correlationId,
                    status,
                    ex.getMessage(),
                    ex
            );
            throw new DomainException("Error finding users by status");
        }
    }

    private String currentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "<missing>" : correlationId;
    }
}
