package com.magopay.my_account.infra.job;

import com.magopay.my_account.core.application.ports.out.UserEventPublisherPort;
import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import com.magopay.my_account.core.domain.event.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PendingAnalysisUserEventJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingAnalysisUserEventJob.class);
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final UserRepositoryPort userRepository;
    private final UserEventPublisherPort userEventPublisher;

    public PendingAnalysisUserEventJob(UserRepositoryPort userRepository, UserEventPublisherPort userEventPublisher) {
        this.userRepository = userRepository;
        this.userEventPublisher = userEventPublisher;
    }

    @Scheduled(fixedDelayString = "${jobs.pending-analysis.fixed-delay-ms:3600000}")
    public void processPendingAnalysisUsers() {
        String correlationId = currentCorrelationId();

        List<User> pendingUsers = userRepository.findByStatus(UserStatus.ANALYSIS_PENDING);
        LOGGER.info(
                "event=user.pending-analysis.job.started correlationId={} count={}",
                correlationId,
                pendingUsers.size()
        );

        for (User user : pendingUsers) {
            try {
                userEventPublisher.publish(new UserCreatedEvent(
                        user.getId(),
                        user.getName(),
                        user.getDocument()
                ));

                userRepository.updateStatus(user.getId(), UserStatus.IN_ANALYZING);
                LOGGER.info(
                        "event=user.pending-analysis.job.completed correlationId={} userId={} status={}",
                        correlationId,
                        user.getId(),
                        UserStatus.IN_ANALYZING
                );
            } catch (RuntimeException ex) {
                LOGGER.error(
                        "event=user.pending-analysis.job.failed correlationId={} userId={} reason={}",
                        correlationId,
                        user.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }

    private String currentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "<missing>" : correlationId;
    }
}

