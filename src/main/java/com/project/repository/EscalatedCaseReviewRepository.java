package com.project.repository;

import com.project.model.EscalatedCaseReview;
import com.project.model.PriorityState;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data access operations for EscalatedCaseReview entities.
 * Handles saving and retrieving supervisor review decisions on escalated cases.
 */
public interface EscalatedCaseReviewRepository {
    int save(Connection connection, int caseId, String instructions, PriorityState previousPriorityState,
             PriorityState resultingPriorityState, int reviewedByUserId) throws SQLException;

    void deleteByCaseId(Connection connection, int caseId) throws SQLException;

    Optional<EscalatedCaseReview> findLatestByCaseId(Connection connection, int caseId) throws SQLException;
}
