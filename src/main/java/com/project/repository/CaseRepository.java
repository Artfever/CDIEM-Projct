package com.project.repository;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface CaseRepository {
    int save(Connection connection, Case c) throws SQLException;

    void updateSeverityProfile(Connection connection, int caseId, SeverityLevel severity, int slaHours,
                               PriorityState priorityState) throws SQLException;

    void updateAssignedOfficer(Connection connection, int caseId, Integer officerId, String officerName,
                               CaseState caseState) throws SQLException;

    void updatePriorityState(Connection connection, int caseId, PriorityState priorityState) throws SQLException;

    void updateState(Connection connection, int caseId, CaseState caseState) throws SQLException;

    Optional<Case> findById(Connection connection, int caseId) throws SQLException;
}
