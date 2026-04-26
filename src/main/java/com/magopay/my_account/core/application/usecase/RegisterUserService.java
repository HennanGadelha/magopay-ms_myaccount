package com.magopay.my_account.core.application.usecase;

import com.magopay.my_account.core.application.ports.in.RegisterUserUseCase;
import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;
import com.magopay.my_account.core.application.ports.out.PasswordEncoderPort;
import com.magopay.my_account.core.application.ports.out.UserEventPublisherPort;
import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.event.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterUserService.class);
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final UserEventPublisherPort userEventPublisher;

    public RegisterUserService(UserRepositoryPort userRepository, PasswordEncoderPort passwordEncoder, UserEventPublisherPort userEventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userEventPublisher = userEventPublisher;
    }

    @Override
    public RegisterUserResult execute(RegisterUserCommand command) {
        String correlationId = currentCorrelationId();
        String maskedEmail = maskEmail(command.email());
        String maskedDocument = maskDocument(command.document());

        LOGGER.info(
                "event=user.register.usecase.started correlationId={} emailMasked={} documentMasked={}",
                correlationId,
                maskedEmail,
                maskedDocument
        );

        try {
            User user = User.create(
                    UUID.randomUUID(),
                    command.name(),
                    command.email(),
                    command.document(),
                    passwordEncoder.encode(command.password())
            );

            LOGGER.debug(
                    "event=user.register.domain.created correlationId={} userId={} status={}",
                    correlationId,
                    user.getId(),
                    user.getStatus()
            );

            User savedUser = userRepository.save(user);
            publishUserCreatedEvent(savedUser, correlationId);

            LOGGER.info(
                    "event=user.register.usecase.completed correlationId={} userId={} status={}",
                    correlationId,
                    savedUser.getId(),
                    savedUser.getStatus()
            );

            return new RegisterUserResult(
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getDocument()
            );
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "event=user.register.usecase.failed correlationId={} emailMasked={} documentMasked={} reason={}",
                    correlationId,
                    maskedEmail,
                    maskedDocument,
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private void publishUserCreatedEvent(User savedUser, String correlationId) {
        try {
            userEventPublisher.publish(new UserCreatedEvent(
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getDocument()
            ));
        } catch (RuntimeException publishError) {
            LOGGER.error(
                    "event=user.register.sqs.publish.failed correlationId={} userId={} reason={}",
                    correlationId,
                    savedUser.getId(),
                    publishError.getMessage(),
                    publishError
            );

            try {
                savedUser.markAsAnalysisPending();
                userRepository.updateStatus(savedUser.getId(), savedUser.getStatus());
                LOGGER.warn(
                        "event=user.register.status.fallback.applied correlationId={} userId={} status={}",
                        correlationId,
                        savedUser.getId(),
                        savedUser.getStatus()
                );
            } catch (RuntimeException statusError) {
                LOGGER.error(
                        "event=user.register.status.fallback.failed correlationId={} userId={} reason={}",
                        correlationId,
                        savedUser.getId(),
                        statusError.getMessage(),
                        statusError
                );
            }
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

    private String maskDocument(String document) {
        if (document == null || document.isBlank()) {
            return "<empty>";
        }

        String normalized = document.trim();
        int visible = Math.min(4, normalized.length());
        return "***" + normalized.substring(normalized.length() - visible);
    }
}
