package com.project.model;

/**
 * Represents a user in the system (Investigating Officer, Analyst, or Supervisor).
 * Contains basic identity information and role-based access control.
 */
public class User {
    private final int userId;
    private final String name;
    private final String username;
    private final String email;
    private final UserRole role;

    public User(int userId, String name, UserRole role) {
        this(userId, name, null, null, role);
    }

    public User(int userId, String name, String username, String email, UserRole role) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public String getDisplayLabel() {
        if (username == null || username.isBlank()) {
            return name + " | " + role.getDisplayName() + " | ID " + userId;
        }

        return name + " | @" + username + " | " + role.getDisplayName() + " | ID " + userId;
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
