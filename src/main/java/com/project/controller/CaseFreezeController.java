package com.project.controller;

import com.project.model.Case;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.CaseFreezeResult;
import com.project.service.ChainOfCustodyLog;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction workflow for freezing a case.
 * Freezing preserves the case but stops normal work on it until a supervisor reopens it.
 */
public class CaseFreezeController extends AbstractCaseWorkflowController {
    private final ChainOfCustodyLog chainOfCustodyLog;

    public CaseFreezeController() {
        this(new CaseRepositoryImpl(), new UserRepositoryImpl(), new ChainOfCustodyLog());
    }

    public CaseFreezeController(CaseRepository caseRepository, UserRepository userRepository,
                                ChainOfCustodyLog chainOfCustodyLog) {
        super(caseRepository, userRepository);
        this.chainOfCustodyLog = chainOfCustodyLog;
    }

    public CaseFreezeResult initiateFreeze(int caseId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.ANALYST,
                        "Only a Digital Forensic Analyst can freeze a case."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateFreezableState();

                // The audit record is written before the state change so the pause is traceable.
                chainOfCustodyLog.recordFreeze(connection, existingCase, currentUser);
                existingCase.triggerFreezeWorkflow();
                caseRepository.updateState(connection, caseId, existingCase.getStatus());

                connection.commit();
                return new CaseFreezeResult(existingCase.getStatus());
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to freeze the case.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }
}
