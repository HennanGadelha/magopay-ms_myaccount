package com.magopay.my_account.core.domain.event;

import java.time.Instant;
import java.util.UUID;

public class UserCreatedEvent {
    private final UUID userId;
    private final String name;
    private final String document;
    private final Instant occurredAt;

    public UserCreatedEvent(UUID userId, String name, String document) {
        this.userId = userId;
        this.name = name;
        this.document = document;
        this.occurredAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

