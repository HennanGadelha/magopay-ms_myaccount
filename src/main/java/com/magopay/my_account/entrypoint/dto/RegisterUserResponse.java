package com.magopay.my_account.entrypoint.dto;

import java.util.UUID;
import org.springframework.hateoas.RepresentationModel;

public class RegisterUserResponse extends RepresentationModel<RegisterUserResponse> {
    private UUID id;
    private String name;
    private String email;
    private String document;

    public RegisterUserResponse(UUID id, String name, String email, String document) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}


