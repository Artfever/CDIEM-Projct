package com.project.controller;

import com.project.model.Case;
import com.project.model.PriorityState;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EscalatedCaseReviewRepository;
import com.project.repository.EscalatedCaseReviewRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.ChainOfCustodyLog;
import com.project.service.EscalatedCaseReviewResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class EscalatedCaseController extends AbstractCaseWorkflowController {
    private static final int MAX_INSTRUCTION_LENGTH = 500;

    private final ChainOfCustodyLog chainOfCustodyLog;
    private final EscalatedCaseReviewRepository escalatedCaseReviewRepository;

    public EscalatedCaseController() {
        this(new CaseRepositoryImpl(), new UserRepositoryImpl(), new ChainOfCustodyLog(),
                new EscalatedCaseReviewRepositoryImpl());
    }

    public EscalatedCaseController(CaseRepository caseRepository, UserRepository userRepository,
                                   ChainOfCustodyLog chainOfCustodyLog,
                                   EscalatedCaseReviewRepository escalatedCaseReviewRepository) {
        super(caseRepository, userRepository);
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.escalatedCaseReviewRepository = escalatedCaseReviewRepository;
    }

    public EscalatedCaseReviewResult recordReviewDecision(int caseId, String instructions, int userId) {
        String cleanedInstructions = validateInstructions(instructions);

        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can review escalated cases."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateEscalatedReviewState(LocalDateTime.now());

                PriorityState previousPriorityState = existingCase.getPriorityState();
                chainOfCustodyLog.recordEscalatedReview(connection, existingCase, currentUser, cleanedInstructions);
                existingCase.transitionPriorityState(PriorityState.UNDER_ACTIVE_REVIEW);
                caseRepository.updatePriorityState(connection, caseId, existingCase.getPriorityState());
                escalatedCaseReviewRepository.save(
                        connection,
                        caseId,
                        cleanedInstructions,
                        previousPriorityState,
                        existingCase.getPriorityState(),
                        currentUser.getUserId()
                );

                connection.commit();
                return new EscalatedCaseReviewResult(existingCase.getPriorityState(), cleanedInstructions);
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to review the escalated case.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private String validateInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("Corrective instructions are required.");
        }

        String cleanedInstructions = instructions.trim();
        if (cleanedInstructions.length() > MAX_INSTRUCTION_LENGTH) {
            throw new IllegalArgumentException("Corrective instructions must not exceed " + MAX_INSTRUCTION_LENGTH + " characters.");
        }

        return cleanedInstructions;
    }
}
