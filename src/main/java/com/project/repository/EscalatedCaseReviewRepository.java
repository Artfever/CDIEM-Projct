package com.project.repository;

import com.project.model.EscalatedCaseReview;
import com.project.model.PriorityState;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface EscalatedCaseReviewRepository {
    int save(Connection connection, int caseId, String instructions, PriorityState previousPriorityState,
             PriorityState resultingPriorityState, int reviewedByUserId) throws SQLException;

    Optional<EscalatedCaseReview> findLatestByCaseId(Connection connection, int caseId) throws SQLException;
}
