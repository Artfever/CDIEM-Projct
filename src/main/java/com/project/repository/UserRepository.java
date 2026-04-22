package com.project.repository;

import com.project.model.User;
import com.project.model.UserRole;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Connection connection, int userId) throws SQLException;

    List<User> findByRole(Connection connection, UserRole role) throws SQLException;

    List<User> findCaseActors(Connection connection) throws SQLException;
}
