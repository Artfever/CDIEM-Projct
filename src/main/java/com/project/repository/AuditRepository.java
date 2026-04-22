package com.project.repository;

import java.sql.Connection;
import java.sql.SQLException;

public interface AuditRepository {
    void logAction(Connection connection, Integer caseId, String action, int userId) throws SQLException;
}
