package com.magopay.my_account.entrypoint.dto;

import com.magopay.my_account.core.domain.UserStatus;
import org.springframework.hateoas.RepresentationModel;

import java.util.UUID;

public class FindUserByEmailResponse extends RepresentationModel<FindUserByEmailResponse> {

    private UUID id;
    private String name;
    private String email;
    private String document;
    private UserStatus status;

    public FindUserByEmailResponse(UUID id, String name, String email, String document, UserStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDocument() { return document; }
    public UserStatus getStatus() { return status; }
}

