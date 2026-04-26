package com.project.service;

import com.project.model.User;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;
import com.project.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles user authentication.
 * Validates credentials and returns the authenticated user.
 */
public class AuthService {
    private final UserRepository userRepository;

    public AuthService() {
        this(new UserRepositoryImpl());
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticate(String identifier, String password) {
        String cleanedIdentifier = clean(identifier);
        String cleanedPassword = clean(password);

        if (cleanedIdentifier == null) {
            throw new IllegalArgumentException("Username or email is required.");
        }

        if (cleanedPassword == null) {
            throw new IllegalArgumentException("Password is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            // Login accepts either username or email, then compares the stored password hash.
            return userRepository.authenticate(connection, cleanedIdentifier, PasswordUtil.sha256(cleanedPassword))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid username/email or password."));
        } catch (SQLException e) {
            if (isMissingAuthSchema(e)) {
                throw new IllegalStateException(
                        "Login schema is missing. Apply database migration 003_auth_and_dashboard_structure.sql.",
                        e
                );
            }

            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isMissingAuthSchema(SQLException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Invalid column name 'Username'")
                || message.contains("Invalid column name 'Email'")
                || message.contains("Invalid column name 'PasswordHash'");
    }
}
