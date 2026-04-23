package com.project.repository;

import com.project.model.Evidence;
import com.project.model.EvidenceStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EvidenceRepository {
    int save(Connection connection, Evidence evidence) throws SQLException;

    Optional<Evidence> findLatestByCaseId(Connection connection, int caseId) throws SQLException;

    void updateVerificationSnapshot(Connection connection, int evidenceId, String recalculatedSha256,
                                    Integer verifiedByUserId, LocalDateTime verifiedAt) throws SQLException;

    void updateStatus(Connection connection, int evidenceId, EvidenceStatus status, String recalculatedSha256,
                      Integer verifiedByUserId, LocalDateTime verifiedAt) throws SQLException;
}
