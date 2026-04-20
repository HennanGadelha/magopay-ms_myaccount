package com.magopay.my_account.core.application.usecase;

import com.magopay.my_account.core.application.ports.in.FindUserByEmailUseCase;
import com.magopay.my_account.core.application.ports.in.query.FindUserByEmailQuery;
import com.magopay.my_account.core.application.ports.in.result.FindUserByEmailResult;
import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class FindUserByEmailService implements FindUserByEmailUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FindUserByEmailService.class);
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final UserRepositoryPort userRepository;

    public FindUserByEmailService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public FindUserByEmailResult execute(FindUserByEmailQuery query) {
        String correlationId = currentCorrelationId();
        String maskedEmail = maskEmail(query.email());

        LOGGER.info(
                "event=user.findByEmail.usecase.started correlationId={} emailMasked={}",
                correlationId,
                maskedEmail
        );

        try {
            User user = userRepository.findByEmail(query.email())
                    .orElseThrow(() -> new UserNotFoundException("User not found with given email"));

            LOGGER.info(
                    "event=user.findByEmail.usecase.completed correlationId={} userId={} status={}",
                    correlationId,
                    user.getId(),
                    user.getStatus()
            );

            return new FindUserByEmailResult(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getDocument(),
                    user.getStatus()
            );
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "event=user.findByEmail.usecase.failed correlationId={} emailMasked={} reason={}",
                    correlationId,
                    maskedEmail,
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private String currentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "<missing>" : correlationId;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<empty>";
        }
        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 1 || atIndex == normalized.length() - 1) {
            return "***";
        }
        return normalized.substring(0, 2) + "***" + normalized.substring(atIndex);
    }
}

