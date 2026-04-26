package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.UserRepository;
import com.project.service.CaseReassignmentResult;
import com.project.service.ChainOfCustodyLog;
import com.project.service.NotificationDispatchResult;
import com.project.service.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction workflow for moving a case from one officer to another.
 * The change is logged and both officers are notified inside the same workflow.
 */
public class ReassignmentController extends AbstractCaseWorkflowController {
    private final ChainOfCustodyLog chainOfCustodyLog;
    private final NotificationService notificationService;

    public ReassignmentController() {
        this(new ChainOfCustodyLog(), new NotificationService());
    }

    public ReassignmentController(ChainOfCustodyLog chainOfCustodyLog, NotificationService notificationService) {
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.notificationService = notificationService;
    }

    public ReassignmentController(CaseRepository caseRepository, UserRepository userRepository,
                                  ChainOfCustodyLog chainOfCustodyLog, NotificationService notificationService) {
        super(caseRepository, userRepository);
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.notificationService = notificationService;
    }

    public CaseReassignmentResult reassignTo(int caseId, Integer officerId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can reassign an Investigating Officer."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateReassignableState();
                User newOfficer = validateAssignedOfficer(connection, officerId);
                User previousOfficer = existingCase.getAssignedOfficerId() == null
                        ? null
                        : requireUser(connection, existingCase.getAssignedOfficerId());

                if (officerId.equals(existingCase.getAssignedOfficerId())) {
                    throw new IllegalArgumentException("This case is already assigned to Investigating Officer " + officerId + ".");
                }

                // The case changes hands here, so the audit trail and notifications are updated together.
                chainOfCustodyLog.recordReassignment(connection, existingCase, previousOfficer, newOfficer, currentUser);
                existingCase.updateAssignedOfficer(newOfficer.getUserId(), newOfficer.getName());
                existingCase.moveToState(CaseState.CASE_REASSIGNED);
                caseRepository.updateAssignedOfficer(
                        connection,
                        caseId,
                        existingCase.getAssignedOfficerId(),
                        existingCase.getAssignedOfficerName(),
                        existingCase.getStatus()
                );

                NotificationDispatchResult notificationResult = notificationService.notifyBothOfficers(
                        connection,
                        caseId,
                        previousOfficer,
                        newOfficer,
                        currentUser
                );

                connection.commit();
                return new CaseReassignmentResult(
                        previousOfficer == null ? "Unassigned" : previousOfficer.getName(),
                        newOfficer.getName(),
                        notificationResult.outgoingOfficerNotified(),
                        notificationResult.incomingOfficerNotified()
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to reassign investigating officer.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }
}
