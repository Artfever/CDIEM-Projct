package com.project.controller;

import com.project.model.CaseClosureDecision;
import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.EvidenceStatus;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.CaseClosureApprovalResult;
import com.project.service.CaseClosureRejectionResult;
import com.project.service.CaseClosureService;
import com.project.service.CaseClosureSnapshot;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main screen for the closure decision stage.
 * Supervisors either close an eligible case or return it for more forensic work.
 */
public class ManageCaseClosureController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm a");

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label identityLabel;

    @FXML
    private Label accessSummaryLabel;

    @FXML
    private TextField caseIdField;

    @FXML
    private Button loadSnapshotButton;

    @FXML
    private VBox approvalPanel;

    @FXML
    private Button approveClosureButton;

    @FXML
    private VBox rejectionPanel;

    @FXML
    private TextArea rejectionReasonArea;

    @FXML
    private Button rejectClosureButton;

    @FXML
    private Label caseStatusValueLabel;

    @FXML
    private Label assignedOfficerValueLabel;

    @FXML
    private Label severityValueLabel;

    @FXML
    private Label priorityValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private Label evidenceStatusValueLabel;

    @FXML
    private Label fileNameValueLabel;

    @FXML
    private Label integritySummaryValueLabel;

    @FXML
    private Label latestDecisionValueLabel;

    @FXML
    private Label decisionByValueLabel;

    @FXML
    private Label decisionTimeValueLabel;

    @FXML
    private Label decisionReasonValueLabel;

    @FXML
    private Label statusLabel;

    private final CaseClosureService closureService = new CaseClosureService();
    private User currentUser;
    private CaseClosureSnapshot currentSnapshot;

    @FXML
    public void initialize() {
        if (rejectionReasonArea != null) {
            rejectionReasonArea.textProperty().addListener((observable, oldValue, newValue) -> updateActionAvailability());
        }

        resetSnapshot();
        setStatus("Manage Case Closure is ready.", STATUS_NEUTRAL);
        updateActionAvailability();
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        applyCurrentUserContext();
    }

    @FXML
    public void goBackToDashboard() {
        if (currentUser == null) {
            setStatus("No authenticated session was found for dashboard navigation.", STATUS_ERROR);
            return;
        }

        try {
            AppNavigator.showDashboard(currentUser);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void loadCaseSnapshot() {
        try {
            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            currentSnapshot = closureService.getClosureSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateActionAvailability();
            setStatus("Case closure snapshot loaded.", STATUS_NEUTRAL);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void approveClosure() {
        try {
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can approve case closure.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // Approval is the clean finish: verified evidence, supervisor sign-off, then the case closes.
            CaseClosureApprovalResult result = closureService.approveClosure(caseId, currentUser.getUserId());
            currentSnapshot = closureService.getClosureSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            rejectionReasonArea.clear();
            updateActionAvailability();
            setStatus(buildApprovalStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void rejectClosure() {
        try {
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can reject case closure.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // Rejection sends the case back for more investigation instead of ending it.
            CaseClosureRejectionResult result = closureService.rejectClosure(
                    caseId,
                    rejectionReasonArea.getText(),
                    currentUser.getUserId()
            );
            currentSnapshot = closureService.getClosureSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            rejectionReasonArea.clear();
            updateActionAvailability();
            setStatus(buildRejectionStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || roleLabel == null) {
            return;
        }

        boolean supervisorSignedIn = currentUser.getRole() == UserRole.SUPERVISOR;

        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(currentUser.getName() + " | " + buildUsername(currentUser) + " | ID " + currentUser.getUserId());
        subtitleLabel.setText(supervisorSignedIn
                ? "Approve eligible supervisor-review cases for closure or return them to forensic review."
                : "This module is reserved for the Supervisory Authority.");
        accessSummaryLabel.setText(supervisorSignedIn
                ? "Supervisory Authority users can close only cases that are still in SUPERVISOR_REVIEW and whose latest evidence is VERIFIED. Rejections require a reason, record a closure decision trail, and send the case back to FORENSIC_REVIEW."
                : "You can inspect case context here, but only Supervisory Authority users can approve or reject case closure.");

        setNodeVisibility(approvalPanel, supervisorSignedIn);
        setNodeVisibility(rejectionPanel, supervisorSignedIn);

        if (supervisorSignedIn) {
            setStatus("Signed in as Supervisory Authority. Closure approval and rejection actions are available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available only to Supervisory Authority users.", STATUS_ERROR);
        }

        updateActionAvailability();
    }

    private void renderSnapshot(CaseClosureSnapshot snapshot) {
        // The closure screen always shows the latest case state, evidence state, and the last closure decision together.
        caseStatusValueLabel.setText(formatCaseState(snapshot.caseRecord().getStatus()));
        assignedOfficerValueLabel.setText(defaultText(snapshot.caseRecord().getAssignedOfficerName(), "Unassigned"));
        severityValueLabel.setText(formatEnumName(snapshot.caseRecord().getSeverity() == null
                ? null
                : snapshot.caseRecord().getSeverity().name()));
        priorityValueLabel.setText(formatEnumName(snapshot.caseRecord().getPriorityState() == null
                ? null
                : snapshot.caseRecord().getPriorityState().name()));
        createdAtValueLabel.setText(formatTimestamp(snapshot.caseRecord().getCreatedAt()));

        Evidence evidence = snapshot.evidence();
        if (evidence == null) {
            evidenceStatusValueLabel.setText("No evidence uploaded");
            fileNameValueLabel.setText("No evidence file recorded");
            integritySummaryValueLabel.setText("Closure approval is blocked until verified evidence exists.");
        } else {
            evidenceStatusValueLabel.setText(evidence.getStatus().getDisplayName());
            fileNameValueLabel.setText(defaultText(evidence.getOriginalFileName(), "Unnamed evidence"));
            integritySummaryValueLabel.setText(buildIntegritySummary(evidence));
        }

        renderLatestDecision(snapshot.latestDecision());
    }

    private void renderLatestDecision(CaseClosureDecision decision) {
        if (decision == null) {
            latestDecisionValueLabel.setText("No closure decision recorded");
            decisionByValueLabel.setText("Unavailable");
            decisionTimeValueLabel.setText("Unavailable");
            decisionReasonValueLabel.setText("No approval or rejection has been recorded for this case yet.");
            return;
        }

        latestDecisionValueLabel.setText(decision.getDecisionType().getDisplayName());
        decisionByValueLabel.setText(defaultText(decision.getDecidedByName(), "User ID " + decision.getDecidedByUserId()));
        decisionTimeValueLabel.setText(formatTimestamp(decision.getDecidedAt()));
        decisionReasonValueLabel.setText(defaultText(
                decision.getReason(),
                "Approval recorded without a rejection reason."
        ));
    }

    private void resetSnapshot() {
        caseStatusValueLabel.setText("No case loaded");
        assignedOfficerValueLabel.setText("Unavailable");
        severityValueLabel.setText("Unavailable");
        priorityValueLabel.setText("Unavailable");
        createdAtValueLabel.setText("Unavailable");
        evidenceStatusValueLabel.setText("No evidence loaded");
        fileNameValueLabel.setText("No evidence file recorded");
        integritySummaryValueLabel.setText("Closure approval is blocked until verified evidence exists.");
        renderLatestDecision(null);
    }

    private String buildIntegritySummary(Evidence evidence) {
        if (evidence.getStatus() == EvidenceStatus.VERIFIED) {
            return "Evidence is verified and eligible for closure approval.";
        }

        if (evidence.getStatus() == EvidenceStatus.TAMPERED) {
            return "Evidence marked as tampered. Closure approval is blocked.";
        }

        if (!evidence.hasVerificationSnapshot()) {
            return "Integrity verification pending.";
        }

        return evidence.hashesMatch()
                ? "Hashes match, but evidence still has not been finalized as VERIFIED."
                : "Hash mismatch detected. Closure approval is blocked.";
    }

    private void updateActionAvailability() {
        boolean supervisorSignedIn = currentUser != null && currentUser.getRole() == UserRole.SUPERVISOR;
        boolean snapshotLoaded = currentSnapshot != null;
        boolean caseUnderSupervisorReview = snapshotLoaded
                && currentSnapshot.caseRecord().getStatus() == CaseState.SUPERVISOR_REVIEW;
        boolean closureEligible = caseUnderSupervisorReview
                && currentSnapshot.hasEvidence()
                && currentSnapshot.evidence().getStatus() == EvidenceStatus.VERIFIED;
        boolean rejectionReasonProvided = rejectionReasonArea != null && clean(rejectionReasonArea.getText()) != null;

        if (loadSnapshotButton != null) {
            loadSnapshotButton.setDisable(currentUser == null);
        }
        if (approveClosureButton != null) {
            approveClosureButton.setDisable(!supervisorSignedIn || !closureEligible);
        }
        if (rejectClosureButton != null) {
            rejectClosureButton.setDisable(!supervisorSignedIn || !caseUnderSupervisorReview || !rejectionReasonProvided);
        }
    }

    private String buildApprovalStatus(CaseClosureApprovalResult result) {
        if (!result.investigatingOfficerNotified()) {
            return "Case closure approved. State is now " + formatCaseState(result.caseState())
                    + ", but no assigned investigating officer was available to notify.";
        }

        return "Case closure approved. State is now " + formatCaseState(result.caseState())
                + ". " + result.investigatingOfficerName() + " was notified.";
    }

    private String buildRejectionStatus(CaseClosureRejectionResult result) {
        return "Case closure rejected. State is now " + formatCaseState(result.caseState())
                + ". The rejection reason has been recorded.";
    }

    private void ensureRole(UserRole expectedRole, String message) {
        if (currentUser == null || currentUser.getRole() != expectedRole) {
            throw new IllegalStateException(message);
        }
    }

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_SUCCESS, STATUS_ERROR);
        statusLabel.getStyleClass().add(stateClass);
    }

    private void setNodeVisibility(Node node, boolean visible) {
        if (node == null) {
            return;
        }

        node.setVisible(visible);
        node.setManaged(visible);
    }

    private int parseRequiredInteger(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private String buildUsername(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return "n/a";
        }

        return "@" + user.getUsername();
    }

    private String formatCaseState(CaseState caseState) {
        return formatEnumName(caseState == null ? null : caseState.name());
    }

    private String formatEnumName(String value) {
        if (value == null || value.isBlank()) {
            return "Unavailable";
        }

        String[] tokens = value.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private String formatTimestamp(LocalDateTime value) {
        return value == null ? "Unavailable" : value.format(TIMESTAMP_FORMATTER);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getRootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage() == null ? "Unknown error." : current.getMessage();
    }
}
