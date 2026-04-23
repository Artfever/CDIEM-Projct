package com.project.repository;

import com.project.model.CaseClosureDecision;
import com.project.model.CaseClosureDecisionType;
import com.project.model.CaseState;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface CaseClosureDecisionRepository {
    int save(Connection connection, int caseId, CaseClosureDecisionType decisionType, String reason,
             CaseState previousState, CaseState resultingState, int decidedByUserId) throws SQLException;

    Optional<CaseClosureDecision> findLatestByCaseId(Connection connection, int caseId) throws SQLException;
}
