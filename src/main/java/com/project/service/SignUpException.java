package com.project.service;

public class SignUpException extends RuntimeException {
    public enum Reason {
        REQUIRED_FIELDS,
        PASSWORD_MISMATCH,
        INVALID_EMAIL,
        PASSWORD_TOO_SHORT,
        USERNAME_IN_USE,
        EMAIL_REGISTERED,
        DATABASE_UNREACHABLE,
        MISSING_SCHEMA
    }

    private final Reason reason;

    public SignUpException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public SignUpException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
