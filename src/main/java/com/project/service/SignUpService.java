package com.project.service;

import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;
import com.project.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class SignUpService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;

    public SignUpService() {
        this(new UserRepositoryImpl());
    }

    public SignUpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String fullName, String username, String email,
                         String password, String confirmPassword, UserRole role) {
        String cleanedFullName = clean(fullName);
        String cleanedUsername = clean(username);
        String cleanedEmail = clean(email);
        String cleanedPassword = clean(password);
        String cleanedConfirmPassword = clean(confirmPassword);

        if (cleanedFullName == null
                || cleanedUsername == null
                || cleanedEmail == null
                || cleanedPassword == null
                || cleanedConfirmPassword == null
                || role == null) {
            throw new SignUpException(SignUpException.Reason.REQUIRED_FIELDS, "All fields are required.");
        }

        if (!cleanedPassword.equals(cleanedConfirmPassword)) {
            throw new SignUpException(SignUpException.Reason.PASSWORD_MISMATCH, "Passwords do not match.");
        }

        if (!EMAIL_PATTERN.matcher(cleanedEmail).matches()) {
            throw new SignUpException(SignUpException.Reason.INVALID_EMAIL, "Please enter a valid email address.");
        }

        if (cleanedPassword.length() < 8) {
            throw new SignUpException(SignUpException.Reason.PASSWORD_TOO_SHORT,
                    "Password must be at least 8 characters long.");
        }

        String passwordHash = PasswordUtil.sha256(cleanedPassword);

        try (Connection connection = DBConnection.getConnection()) {
            if (userRepository.usernameExists(connection, cleanedUsername)) {
                throw new SignUpException(SignUpException.Reason.USERNAME_IN_USE,
                        "Username is already in use. Please choose another.");
            }

            if (userRepository.emailExists(connection, cleanedEmail)) {
                throw new SignUpException(SignUpException.Reason.EMAIL_REGISTERED,
                        "Email is already registered. Use a different email or log in.");
            }

            return userRepository.create(connection, cleanedFullName, cleanedUsername, cleanedEmail, passwordHash, role);
        } catch (SQLException e) {
            if (isUsernameConstraintViolation(e)) {
                throw new SignUpException(SignUpException.Reason.USERNAME_IN_USE,
                        "Username is already in use. Please choose another.", e);
            }

            if (isEmailConstraintViolation(e)) {
                throw new SignUpException(SignUpException.Reason.EMAIL_REGISTERED,
                        "Email is already registered. Use a different email or log in.", e);
            }

            if (isMissingAuthSchema(e)) {
                throw new SignUpException(SignUpException.Reason.MISSING_SCHEMA,
                        "Sign-up schema is missing. Apply database migration 003_auth_and_dashboard_structure.sql.",
                        e);
            }

            throw new SignUpException(SignUpException.Reason.DATABASE_UNREACHABLE,
                    "Could not connect to the database.", e);
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
        return containsMessage(e, "Invalid column name 'Username'")
                || containsMessage(e, "Invalid column name 'Email'")
                || containsMessage(e, "Invalid column name 'PasswordHash'");
    }

    private boolean isUsernameConstraintViolation(SQLException e) {
        return containsMessage(e, "UX_Users_Username");
    }

    private boolean isEmailConstraintViolation(SQLException e) {
        return containsMessage(e, "UX_Users_Email");
    }

    private boolean containsMessage(Throwable throwable, String value) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(value)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }
}
