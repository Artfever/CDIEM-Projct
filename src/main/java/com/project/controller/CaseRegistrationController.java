package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.PriorityState;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.UserRepository;
import com.project.service.AuditLogService;
import com.project.service.SLAManager;
import com.project.util.IdGenerator;

import java.sql.Connection;
import java.sql.SQLException;

public class CaseRegistrationController extends AbstractCaseWorkflowController {
    private final AuditLogService auditLogService;
    private final SLAManager slaManager;
    private final IdGenerator idGenerator;

    public CaseRegistrationController() {
        this(AuditLogService.getInstance(), new SLAManager(), new IdGenerator());
    }

    public CaseRegistrationController(AuditLogService auditLogService, SLAManager slaManager, IdGenerator idGenerator) {
        this.auditLogService = auditLogService;
        this.slaManager = slaManager;
        this.idGenerator = idGenerator;
    }

    public CaseRegistrationController(CaseRepository caseRepository, UserRepository userRepository,
                                      AuditLogService auditLogService, SLAManager slaManager,
                                      IdGenerator idGenerator) {
        super(caseRepository, userRepository);
        this.auditLogService = auditLogService;
        this.slaManager = slaManager;
        this.idGenerator = idGenerator;
    }

    public int submitCaseDetails(Case caseRecord, int userId) {
        validateCaseRegistration(caseRecord);

        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.OFFICER,
                        "Only an Investigating Officer can create a case."
                );

                int newSlaHours = slaManager.reEvaluateThreshold(caseRecord.getSeverity());
                PriorityState newPriorityState = slaManager.transitionPriorityState(caseRecord.getSeverity());
                caseRecord.setSlaHours(newSlaHours);
                caseRecord.transitionPriorityState(newPriorityState);
                caseRecord.updateAssignedOfficer(currentUser.getUserId(), currentUser.getName());
                caseRecord.moveToState(CaseState.CASE_CREATED);

                int storedCaseId = validateAndStoreCase(connection, caseRecord);
                int caseId = generateUniqueCaseID(storedCaseId);
                auditLogService.recordAudit(
                        connection,
                        caseId,
                        "CASE_REGISTERED with initial state CASE_CREATED, severity " + caseRecord.getSeverity().name()
                                + ", SLA " + caseRecord.getSlaHours() + "h, priority "
                                + caseRecord.getPriorityState().name()
                                + " by " + currentUser.getRole().getDisplayName(),
                        currentUser.getUserId()
                );

                connection.commit();
                return caseId;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to create case.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private int validateAndStoreCase(Connection connection, Case caseRecord) throws SQLException {
        return caseRepository.save(connection, caseRecord);
    }

    private int generateUniqueCaseID(int storedCaseId) {
        return idGenerator.generateUniqueCaseID(storedCaseId);
    }
}
