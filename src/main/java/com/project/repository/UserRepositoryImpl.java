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
            SELECT UserID, Name, Role
            FROM Users
            WHERE UserID = ?
            """;
    private static final String FIND_BY_ROLE_SQL = """
            SELECT UserID, Name, Role
            FROM Users
            WHERE Role = ?
            ORDER BY Name, UserID
            """;
    private static final String FIND_CASE_ACTORS_SQL = """
            SELECT UserID, Name, Role
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

                return Optional.of(new User(
                        resultSet.getInt("UserID"),
                        resultSet.getString("Name"),
                        UserRole.valueOf(resultSet.getString("Role"))
                ));
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

    private List<User> mapUsers(ResultSet resultSet) throws SQLException {
        List<User> users = new ArrayList<>();
        while (resultSet.next()) {
            users.add(new User(
                    resultSet.getInt("UserID"),
                    resultSet.getString("Name"),
                    UserRole.valueOf(resultSet.getString("Role"))
            ));
        }
        return users;
    }
}
