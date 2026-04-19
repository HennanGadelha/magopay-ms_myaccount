package com.magopay.my_account.infra.persistence.query;

public class UserPersistenceQueries {
    public static final String INSERT_USER =
            "INSERT INTO users (id, name, email, document, password, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private UserPersistenceQueries() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
