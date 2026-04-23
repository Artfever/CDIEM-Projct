package com.project.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AuditRepository {
    void logAction(Connection connection, Integer caseId, String action, int userId) throws SQLException;

    Optional<Integer> findMostRecentActorByActionPrefixes(Connection connection, int caseId,
                                                          List<String> actionPrefixes) throws SQLException;
}
