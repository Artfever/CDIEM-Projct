package com.project.repository;

import com.project.model.User;
import com.project.model.UserRole;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Connection connection, int userId) throws SQLException;

    Optional<User> authenticate(Connection connection, String identifier, String passwordHash) throws SQLException;

    boolean usernameExists(Connection connection, String username) throws SQLException;

    boolean emailExists(Connection connection, String email) throws SQLException;

    User create(Connection connection, String name, String username, String email,
                String passwordHash, UserRole role) throws SQLException;

    List<User> findByRole(Connection connection, UserRole role) throws SQLException;

    List<User> findCaseActors(Connection connection) throws SQLException;
}
