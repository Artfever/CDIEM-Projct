package com.project.model;

public class User {
    private final int userId;
    private final String name;
    private final UserRole role;

    public User(int userId, String name, UserRole role) {
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public String getDisplayLabel() {
        return name + " | " + role.getDisplayName() + " | ID " + userId;
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
