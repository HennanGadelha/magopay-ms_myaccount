package com.magopay.my_account.core.domain;

import com.magopay.my_account.core.domain.exception.DomainException;
import com.magopay.my_account.core.domain.exception.InvalidDocumentException;
import com.magopay.my_account.core.domain.exception.InvalidEmailException;
import com.magopay.my_account.core.domain.exception.InvalidPasswordException;
import com.magopay.my_account.core.domain.exception.InvalidUserNameException;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class User {

    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private String name;
    private String email;
    private String document;
    private String password;
    private UserStatus status;

    private User(UUID id, String name, String email, String document, String password, UserStatus status) {
        this.id = validateId(id);
        this.name = normalizeAndValidateName(name);
        this.email = normalizeAndValidateEmail(email);
        this.document = normalizeAndValidateDocument(document);
        this.password = normalizeAndValidatePassword(password);
        this.status = validateStatus(status);
        validateState();
    }

    public static User create(UUID id, String name, String email, String document, String password) {
        return new User(id, name, email, document, password, UserStatus.IN_ANALYZING);
    }

    public static User reconstitute(UUID id, String name, String email, String document, String password, UserStatus status) {
        return new User(id, name, email, document, password, status);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDocument() {
        return document;
    }

    public String getPassword() {
        return password;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        validateState();
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        validateState();
    }

    public void startAnalyzing() {
        this.status = UserStatus.IN_ANALYZING;
        validateState();
    }

    public void changeName(String newName) {
        this.name = normalizeAndValidateName(newName);
        validateState();
    }

    public void changeEmail(String newEmail) {
        this.email = normalizeAndValidateEmail(newEmail);
        validateState();
    }

    public void changeDocument(String newDocument) {
        this.document = normalizeAndValidateDocument(newDocument);
        validateState();
    }

    public void changePassword(String newPassword) {
        this.password = normalizeAndValidatePassword(newPassword);
        validateState();
    }

    private void validateState() {
        validateId(this.id);
        normalizeAndValidateName(this.name);
        normalizeAndValidateEmail(this.email);
        normalizeAndValidateDocument(this.document);
        normalizeAndValidatePassword(this.password);
        validateStatus(this.status);
    }

    private static UUID validateId(UUID value) {
        if (value == null) {
            throw new DomainException("User id cannot be null");
        }
        return value;
    }

    private static UserStatus validateStatus(UserStatus value) {
        if (value == null) {
            throw new DomainException("User status cannot be null");
        }
        return value;
    }

    private static String normalizeAndValidateName(String value) {
        String normalized = normalizeRequired(value);
        if (normalized.isEmpty()) {
            throw new InvalidUserNameException("User name cannot be empty");
        }
        return normalized;
    }

    private static String normalizeAndValidateEmail(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new InvalidEmailException("User email cannot be empty");
        }
        if (!SIMPLE_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailException("User email format is invalid");
        }
        return normalized;
    }

    private static String normalizeAndValidateDocument(String value) {
        String normalized = normalizeRequired(value).replaceAll("[.\\-]", "");
        if (normalized.isEmpty()) {
            throw new InvalidDocumentException("User document cannot be empty");
        }
        return normalized;
    }

    private static String normalizeAndValidatePassword(String value) {
        String normalized = normalizeRequired(value);
        if (normalized.isEmpty()) {
            throw new InvalidPasswordException("User password cannot be empty");
        }
        return normalized;
    }

    private static String normalizeRequired(String value) {
        if (value == null) {
            throw new DomainException("Field value cannot be null");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", document='" + document + '\'' +
                ", status=" + status +
                '}';
    }
}
