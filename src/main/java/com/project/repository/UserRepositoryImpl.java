package com.project.repository;

import com.project.model.User;
import com.project.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    private static final String FIND_BY_ID_SQL = """
            SELECT UserID, Name, Username, Email, Role
            FROM Users
            WHERE UserID = ?
            """;
    private static final String AUTHENTICATE_SQL = """
            SELECT UserID, Name, Username, Email, Role
            FROM Users
            WHERE (LOWER(Username) = LOWER(?) OR LOWER(Email) = LOWER(?))
              AND PasswordHash = ?
            """;
    private static final String USERNAME_EXISTS_SQL = """
            SELECT TOP 1 1
            FROM Users
            WHERE LOWER(Username) = LOWER(?)
            """;
    private static final String EMAIL_EXISTS_SQL = """
            SELECT TOP 1 1
            FROM Users
            WHERE LOWER(Email) = LOWER(?)
            """;
    private static final String CREATE_USER_SQL = """
            INSERT INTO Users (Name, Username, Email, PasswordHash, Role)
            OUTPUT INSERTED.UserID AS UserID
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String FIND_BY_ROLE_SQL = """
            SELECT UserID, Name, Username, Email, Role
            FROM Users
            WHERE Role = ?
            ORDER BY Name, UserID
            """;
    private static final String FIND_CASE_ACTORS_SQL = """
            SELECT UserID, Name, Username, Email, Role
            FROM Users
            WHERE Role IN ('OFFICER', 'SUPERVISOR')
            ORDER BY
                CASE Role WHEN 'SUPERVISOR' THEN 0 ELSE 1 END,
                Name,
                UserID
            """;

    @Override
    public Optional<User> findById(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUser(resultSet));
            }
        }
    }

    @Override
    public Optional<User> authenticate(Connection connection, String identifier, String passwordHash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(AUTHENTICATE_SQL)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            statement.setString(3, passwordHash);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapUser(resultSet));
            }
        }
    }

    @Override
    public boolean usernameExists(Connection connection, String username) throws SQLException {
        return exists(connection, USERNAME_EXISTS_SQL, username);
    }

    @Override
    public boolean emailExists(Connection connection, String email) throws SQLException {
        return exists(connection, EMAIL_EXISTS_SQL, email);
    }

    @Override
    public User create(Connection connection, String name, String username, String email,
                       String passwordHash, UserRole role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_USER_SQL)) {
            statement.setString(1, name);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setString(5, role.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Failed to retrieve the generated UserID.");
                }

                return new User(resultSet.getInt("UserID"), name, username, email, role);
            }
        }
    }

    @Override
    public List<User> findByRole(Connection connection, UserRole role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ROLE_SQL)) {
            statement.setString(1, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapUsers(resultSet);
            }
        }
    }

    @Override
    public List<User> findCaseActors(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_CASE_ACTORS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            return mapUsers(resultSet);
        }
    }

    private boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private List<User> mapUsers(ResultSet resultSet) throws SQLException {
        List<User> users = new ArrayList<>();
        while (resultSet.next()) {
            users.add(mapUser(resultSet));
        }
        return users;
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("UserID"),
                resultSet.getString("Name"),
                resultSet.getString("Username"),
                resultSet.getString("Email"),
                UserRole.valueOf(resultSet.getString("Role"))
        );
    }
}
