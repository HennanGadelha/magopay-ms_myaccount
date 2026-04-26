package com.magopay.my_account.infra.persistence.query;

public class UserPersistenceQueries {
    public static final String INSERT_USER =
            "INSERT INTO users (id, name, email, document, password, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    public static final String SELECT_BY_EMAIL =
            "SELECT id, name, email, document, password, status FROM users WHERE email = ?";

    public static final String UPDATE_STATUS =
            "UPDATE users SET status = ? WHERE id = ?";

    public static final String SELECT_BY_STATUS =
            "SELECT id, name, email, document, password, status FROM users WHERE status = ?";

    private UserPersistenceQueries() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
