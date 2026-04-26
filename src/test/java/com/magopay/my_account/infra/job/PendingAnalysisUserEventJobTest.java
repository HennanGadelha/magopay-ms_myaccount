package com.magopay.my_account.infra.job;

import com.magopay.my_account.core.application.ports.out.UserEventPublisherPort;
import com.magopay.my_account.core.application.ports.out.UserRepositoryPort;
import com.magopay.my_account.core.domain.User;
import com.magopay.my_account.core.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingAnalysisUserEventJob")
class PendingAnalysisUserEventJobTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private UserEventPublisherPort userEventPublisher;

    @InjectMocks
    private PendingAnalysisUserEventJob job;

    @Test
    @DisplayName("deve publicar e mover para IN_ANALYZING quando envio funcionar")
    void shouldPublishAndMoveToInAnalyzingWhenSuccess() {
        User pendingUser = User.reconstitute(
                UUID.randomUUID(),
                "Joao Silva",
                "joao@exemplo.com",
                "12345678901",
                "$2a$hash",
                UserStatus.ANALYSIS_PENDING
        );

        when(userRepository.findByStatus(UserStatus.ANALYSIS_PENDING)).thenReturn(List.of(pendingUser));

        job.processPendingAnalysisUsers();

        verify(userEventPublisher).publish(any());
        verify(userRepository).updateStatus(pendingUser.getId(), UserStatus.IN_ANALYZING);
    }

    @Test
    @DisplayName("deve manter ANALYSIS_PENDING quando publish falhar")
    void shouldKeepStatusWhenPublishFails() {
        User pendingUser = User.reconstitute(
                UUID.randomUUID(),
                "Joao Silva",
                "joao@exemplo.com",
                "12345678901",
                "$2a$hash",
                UserStatus.ANALYSIS_PENDING
        );

        when(userRepository.findByStatus(UserStatus.ANALYSIS_PENDING)).thenReturn(List.of(pendingUser));
        doThrow(new RuntimeException("sqs error")).when(userEventPublisher).publish(any());

        job.processPendingAnalysisUsers();

        verify(userRepository, never()).updateStatus(pendingUser.getId(), UserStatus.IN_ANALYZING);
    }
}

