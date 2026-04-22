package com.project.repository;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.SeverityLevel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface CaseRepository {
    int save(Connection connection, Case c) throws SQLException;

    void updateSeverity(Connection connection, int caseId, SeverityLevel severity) throws SQLException;

    void updateAssignedOfficer(Connection connection, int caseId, Integer officerId, CaseState caseState) throws SQLException;

    Optional<Case> findById(Connection connection, int caseId) throws SQLException;
}
