package com.magopay.my_account.core.application.ports.out;

import com.magopay.my_account.core.domain.event.UserCreatedEvent;

public interface UserEventPublisherPort {
    void publish(UserCreatedEvent event);
}

