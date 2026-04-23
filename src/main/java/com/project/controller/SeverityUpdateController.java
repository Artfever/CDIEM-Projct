package com.project.controller;

import com.project.model.Case;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.UserRepository;
import com.project.service.CaseSeverityUpdateResult;
import com.project.service.ChainOfCustodyLog;
import com.project.service.SLAManager;

import java.sql.Connection;
import java.sql.SQLException;

public class SeverityUpdateController extends AbstractCaseWorkflowController {
    private final ChainOfCustodyLog chainOfCustodyLog;
    private final SLAManager slaManager;

    public SeverityUpdateController() {
        this(new ChainOfCustodyLog(), new SLAManager());
    }

    public SeverityUpdateController(ChainOfCustodyLog chainOfCustodyLog, SLAManager slaManager) {
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.slaManager = slaManager;
    }

    public SeverityUpdateController(CaseRepository caseRepository, UserRepository userRepository,
                                    ChainOfCustodyLog chainOfCustodyLog, SLAManager slaManager) {
        super(caseRepository, userRepository);
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.slaManager = slaManager;
    }

    public CaseSeverityUpdateResult updateSeverity(int caseId, SeverityLevel severityLevel, int userId) {
        if (severityLevel == null) {
            throw new IllegalArgumentException("Severity is required.");
        }

        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can update case severity."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateActiveState();

                if (existingCase.getSeverity() == severityLevel) {
                    throw new IllegalArgumentException("Case is already marked with severity " + severityLevel.name() + ".");
                }

                int recalculatedSlaHours = slaManager.reEvaluateThreshold(severityLevel);
                PriorityState recalculatedPriorityState = slaManager.transitionPriorityState(severityLevel);
                chainOfCustodyLog.recordSeverityChange(
                        connection,
                        existingCase,
                        severityLevel,
                        recalculatedSlaHours,
                        recalculatedPriorityState,
                        currentUser
                );

                existingCase.setSeverity(severityLevel);
                existingCase.setSlaHours(recalculatedSlaHours);
                existingCase.transitionPriorityState(recalculatedPriorityState);
                caseRepository.updateSeverityProfile(
                        connection,
                        caseId,
                        severityLevel,
                        recalculatedSlaHours,
                        recalculatedPriorityState
                );

                connection.commit();
                return new CaseSeverityUpdateResult(severityLevel, recalculatedSlaHours, recalculatedPriorityState);
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to update case severity.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }
}
