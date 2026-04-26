package com.project.service;

import com.project.controller.CaseRegistrationController;
import com.project.controller.CaseDeletionController;
import com.project.controller.ReassignmentController;
import com.project.controller.SeverityUpdateController;
import com.project.model.Case;
import com.project.model.SeverityLevel;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.CaseClosureDecisionRepository;
import com.project.repository.CaseClosureDecisionRepositoryImpl;
import com.project.repository.EscalatedCaseReviewRepository;
import com.project.repository.EscalatedCaseReviewRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.NotificationRepository;
import com.project.repository.NotificationRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;
import com.project.util.IdGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Orchestrates case management operations.
 * Coordinates between registration, severity updates, reassignment, and deletion.
 */
public class CaseService {
    private final UserRepository userRepository;
    private final CaseRegistrationController caseRegistrationController;
    private final SeverityUpdateController severityUpdateController;
    private final ReassignmentController reassignmentController;
    private final CaseDeletionController caseDeletionController;

    public CaseService() {
        this(new CaseRepositoryImpl(), new AuditRepositoryImpl(), new UserRepositoryImpl(),
                new NotificationRepositoryImpl(), new EvidenceRepositoryImpl(),
                new CaseClosureDecisionRepositoryImpl(), new EscalatedCaseReviewRepositoryImpl());
    }

    public CaseService(CaseRepository caseRepository, AuditRepository auditRepository, UserRepository userRepository) {
        this(caseRepository, auditRepository, userRepository, new NotificationRepositoryImpl(),
                new EvidenceRepositoryImpl(), new CaseClosureDecisionRepositoryImpl(),
                new EscalatedCaseReviewRepositoryImpl());
    }

    public CaseService(CaseRepository caseRepository, AuditRepository auditRepository, UserRepository userRepository,
                       NotificationRepository notificationRepository) {
        this(caseRepository, auditRepository, userRepository, notificationRepository, new EvidenceRepositoryImpl(),
                new CaseClosureDecisionRepositoryImpl(), new EscalatedCaseReviewRepositoryImpl());
    }

    public CaseService(CaseRepository caseRepository, AuditRepository auditRepository, UserRepository userRepository,
                       NotificationRepository notificationRepository, EvidenceRepository evidenceRepository) {
        this(caseRepository, auditRepository, userRepository, notificationRepository, evidenceRepository,
                new CaseClosureDecisionRepositoryImpl(), new EscalatedCaseReviewRepositoryImpl());
    }

    public CaseService(CaseRepository caseRepository, AuditRepository auditRepository, UserRepository userRepository,
                       NotificationRepository notificationRepository, EvidenceRepository evidenceRepository,
                       CaseClosureDecisionRepository caseClosureDecisionRepository,
                       EscalatedCaseReviewRepository escalatedCaseReviewRepository) {
        this.userRepository = userRepository;

        // The screen-level service wires together the smaller workflow controllers used by Manage Case.
        AuditLogService auditLogService = new AuditLogService(auditRepository);
        SLAManager slaManager = new SLAManager();
        ChainOfCustodyLog chainOfCustodyLog = new ChainOfCustodyLog(auditLogService);
        NotificationService notificationService = new NotificationService(notificationRepository);

        this.caseRegistrationController = new CaseRegistrationController(
                caseRepository,
                userRepository,
                auditLogService,
                slaManager,
                new IdGenerator()
        );
        this.severityUpdateController = new SeverityUpdateController(
                caseRepository,
                userRepository,
                chainOfCustodyLog,
                slaManager
        );
        this.reassignmentController = new ReassignmentController(
                caseRepository,
                userRepository,
                chainOfCustodyLog,
                notificationService
        );
        this.caseDeletionController = new CaseDeletionController(
                caseRepository,
                evidenceRepository,
                caseClosureDecisionRepository,
                escalatedCaseReviewRepository,
                userRepository,
                auditLogService,
                notificationService,
                new SecureFileStorage()
        );
    }

    public int registerCase(Case caseRecord, int userId) {
        return caseRegistrationController.submitCaseDetails(caseRecord, userId);
    }

    public CaseSeverityUpdateResult updateSeverityLevel(int caseId, SeverityLevel severityLevel, int userId) {
        return severityUpdateController.updateSeverity(caseId, severityLevel, userId);
    }

    public CaseReassignmentResult reassignOfficer(int caseId, Integer officerId, int userId) {
        return reassignmentController.reassignTo(caseId, officerId, userId);
    }

    public CaseDeletionResult deleteCase(int caseId, int userId) {
        return caseDeletionController.deleteCase(caseId, userId);
    }

    public List<User> getCaseActors() {
        try (Connection connection = DBConnection.getConnection()) {
            return userRepository.findCaseActors(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load case actors from the database.", e);
        }
    }

    public List<User> getInvestigatingOfficers() {
        try (Connection connection = DBConnection.getConnection()) {
            return userRepository.findByRole(connection, UserRole.OFFICER);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load investigating officers from the database.", e);
        }
    }
}
