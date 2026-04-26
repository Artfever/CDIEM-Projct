package com.project.controller;

import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.EvidenceStatus;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.EvidenceDecisionResult;
import com.project.service.EvidenceService;
import com.project.service.EvidenceSnapshot;
import com.project.service.EvidenceUploadResult;
import com.project.service.IntegrityVerificationResult;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main screen for the Manage Evidence use case.
 * Officers upload evidence here, and analysts review that evidence for integrity.
 */
public class EvidenceController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";
    private static final String VERIFICATION_NEUTRAL = "verification-neutral";
    private static final String VERIFICATION_MATCH = "verification-match";
    private static final String VERIFICATION_MISMATCH = "verification-mismatch";
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
    private VBox uploadPanel;

    @FXML
    private TextField selectedFileField;

    @FXML
    private Button browseFileButton;

    @FXML
    private Button uploadEvidenceButton;

    @FXML
    private VBox analystPanel;

    @FXML
    private Button verifyIntegrityButton;

    @FXML
    private Button markVerifiedButton;

    @FXML
    private Button markTamperedButton;

    @FXML
    private Label verificationOutcomeLabel;

    @FXML
    private Label caseStatusValueLabel;

    @FXML
    private Label evidenceStatusValueLabel;

    @FXML
    private Label evidenceIdValueLabel;

    @FXML
    private Label fileNameValueLabel;

    @FXML
    private Label storedPathValueLabel;

    @FXML
    private Label uploadedAtValueLabel;

    @FXML
    private Label storedHashValueLabel;

    @FXML
    private Label recalculatedHashValueLabel;

    @FXML
    private Label statusLabel;

    private final EvidenceService evidenceService = new EvidenceService();
    private User currentUser;
    private Path selectedFilePath;
    private EvidenceSnapshot currentSnapshot;

    @FXML
    public void initialize() {
        if (selectedFileField != null) {
            selectedFileField.setEditable(false);
        }

        resetSnapshot();
        setStatus("Manage Evidence is ready.", STATUS_NEUTRAL);
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
    public void loadEvidenceSnapshot() {
        try {
            // The screen always works from the latest saved evidence so the next action matches the current case state.
            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            refreshSnapshot(caseId);
            setStatus("Latest evidence snapshot loaded.", STATUS_NEUTRAL);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void chooseEvidenceFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Evidence File");
        File selectedFile = fileChooser.showOpenDialog(
                browseFileButton == null || browseFileButton.getScene() == null
                        ? null
                        : browseFileButton.getScene().getWindow()
        );

        if (selectedFile == null) {
            return;
        }

        selectedFilePath = selectedFile.toPath();
        selectedFileField.setText(selectedFile.getAbsolutePath());
        updateActionAvailability();
    }

    @FXML
    public void uploadEvidence() {
        try {
            ensureRole(UserRole.OFFICER, "Only an Investigating Officer can upload evidence.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            if (selectedFilePath == null) {
                throw new IllegalArgumentException("An evidence file must be selected.");
            }

            // Upload stores the file, hashes it, and moves the case into evidence intake.
            EvidenceUploadResult result = evidenceService.uploadEvidence(caseId, selectedFilePath, currentUser.getUserId());
            refreshSnapshot(caseId);
            selectedFilePath = null;
            selectedFileField.clear();
            updateActionAvailability();
            setStatus(buildUploadStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void verifyIntegrity() {
        try {
            ensureRole(UserRole.ANALYST, "Only a Digital Forensic Analyst can verify integrity.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // Verification recalculates the hash so the analyst can compare the live file with the recorded one.
            IntegrityVerificationResult result = evidenceService.verifyIntegrity(caseId, currentUser.getUserId());
            refreshSnapshot(caseId);
            setStatus(buildVerificationStatus(result), result.matched() ? STATUS_SUCCESS : STATUS_ERROR);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void markEvidenceVerified() {
        try {
            ensureRole(UserRole.ANALYST, "Only a Digital Forensic Analyst can mark evidence as verified.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // A verified decision keeps the case moving forward toward supervisor review.
            EvidenceDecisionResult result = evidenceService.markEvidenceVerified(caseId, currentUser.getUserId());
            refreshSnapshot(caseId);
            setStatus(buildDecisionStatus("verified", result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void markEvidenceTampered() {
        try {
            ensureRole(UserRole.ANALYST, "Only a Digital Forensic Analyst can mark evidence as tampered.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // A tampered decision stops the normal flow and freezes the case for controlled handling.
            EvidenceDecisionResult result = evidenceService.markEvidenceTampered(caseId, currentUser.getUserId());
            refreshSnapshot(caseId);
            setStatus(buildDecisionStatus("tampered", result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || roleLabel == null) {
            return;
        }

        boolean officerSignedIn = currentUser.getRole() == UserRole.OFFICER;
        boolean analystSignedIn = currentUser.getRole() == UserRole.ANALYST;
        String username = currentUser.getUsername() == null || currentUser.getUsername().isBlank()
                ? "n/a"
                : "@" + currentUser.getUsername();

        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(currentUser.getName() + " | " + username + " | ID " + currentUser.getUserId());
        subtitleLabel.setText(officerSignedIn
                ? "Upload evidence files for your assigned case records and move them into evidence intake."
                : "Verify uploaded evidence, compare hashes, and finalize the forensic integrity outcome.");
        accessSummaryLabel.setText(officerSignedIn
                ? "You can upload evidence for cases currently assigned to you. Each upload is stored locally, hashed with SHA-256, logged to chain-of-custody, and moves the case to EVIDENCE_UPLOADED."
                : "You can load the latest uploaded evidence for a case, compare stored and recalculated hashes, then mark the result as VERIFIED or TAMPERED. Verified evidence remains in FORENSIC_REVIEW until the assigned Investigating Officer submits the case for Supervisor Review, while tampered evidence freezes the case.");

        setNodeVisibility(uploadPanel, officerSignedIn);
        setNodeVisibility(analystPanel, analystSignedIn);

        if (officerSignedIn) {
            setStatus("Signed in as Investigating Officer. Evidence upload actions are available.", STATUS_NEUTRAL);
        } else if (analystSignedIn) {
            setStatus("Signed in as Digital Forensic Analyst. Integrity review actions are available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available to Investigating Officers and Digital Forensic Analysts only.", STATUS_ERROR);
        }

        updateActionAvailability();
    }

    private EvidenceSnapshot refreshSnapshot(int caseId) {
        currentSnapshot = evidenceService.getEvidenceSnapshot(caseId);
        renderSnapshot(currentSnapshot);
        updateActionAvailability();
        return currentSnapshot;
    }

    private void renderSnapshot(EvidenceSnapshot snapshot) {
        caseStatusValueLabel.setText(formatCaseState(snapshot.caseRecord().getStatus()));

        if (!snapshot.hasEvidence()) {
            evidenceStatusValueLabel.setText("No evidence uploaded");
            evidenceIdValueLabel.setText("Pending");
            fileNameValueLabel.setText("No stored evidence file");
            storedPathValueLabel.setText("No storage record available");
            uploadedAtValueLabel.setText("Not available");
            storedHashValueLabel.setText("Not available");
            recalculatedHashValueLabel.setText("Run integrity verification after upload");
            updateVerificationOutcome("No uploaded evidence found for this case.", VERIFICATION_NEUTRAL);
            return;
        }

        Evidence evidence = snapshot.evidence();
        evidenceStatusValueLabel.setText(evidence.getStatus().getDisplayName());
        evidenceIdValueLabel.setText(String.valueOf(evidence.getEvidenceId()));
        fileNameValueLabel.setText(evidence.getOriginalFileName());
        storedPathValueLabel.setText(evidence.getStoredFilePath());
        uploadedAtValueLabel.setText(formatTimestamp(evidence.getUploadedAt()));
        storedHashValueLabel.setText(defaultText(evidence.getOriginalSha256(), "Not available"));
        recalculatedHashValueLabel.setText(defaultText(evidence.getRecalculatedSha256(), "Not yet recalculated"));

        if (evidence.getStatus() == EvidenceStatus.VERIFIED) {
            updateVerificationOutcome("Evidence marked as verified. Awaiting Investigating Officer submission.", VERIFICATION_MATCH);
            return;
        }

        if (evidence.getStatus() == EvidenceStatus.TAMPERED) {
            updateVerificationOutcome("Evidence marked as tampered and the case is now frozen.", VERIFICATION_MISMATCH);
            return;
        }

        if (!evidence.hasVerificationSnapshot()) {
            updateVerificationOutcome("Awaiting integrity verification.", VERIFICATION_NEUTRAL);
            return;
        }

        updateVerificationOutcome(
                evidence.hashesMatch()
                        ? "Last integrity check: SHA-256 values match."
                        : "Last integrity check: hash mismatch detected.",
                evidence.hashesMatch() ? VERIFICATION_MATCH : VERIFICATION_MISMATCH
        );
    }

    private void resetSnapshot() {
        caseStatusValueLabel.setText("No case loaded");
        evidenceStatusValueLabel.setText("No evidence loaded");
        evidenceIdValueLabel.setText("Pending");
        fileNameValueLabel.setText("No file selected");
        storedPathValueLabel.setText("No storage record available");
        uploadedAtValueLabel.setText("Not available");
        storedHashValueLabel.setText("Not available");
        recalculatedHashValueLabel.setText("Not yet recalculated");
        updateVerificationOutcome("Awaiting integrity verification.", VERIFICATION_NEUTRAL);
    }

    private void updateVerificationOutcome(String message, String stateClass) {
        verificationOutcomeLabel.setText(message);
        verificationOutcomeLabel.getStyleClass().removeAll(VERIFICATION_NEUTRAL, VERIFICATION_MATCH, VERIFICATION_MISMATCH);
        verificationOutcomeLabel.getStyleClass().add(stateClass);
    }

    private void updateActionAvailability() {
        boolean officerSignedIn = currentUser != null && currentUser.getRole() == UserRole.OFFICER;
        boolean analystSignedIn = currentUser != null && currentUser.getRole() == UserRole.ANALYST;
        boolean snapshotLoaded = currentSnapshot != null;
        boolean evidenceLoaded = snapshotLoaded && currentSnapshot.hasEvidence();
        boolean inForensicReview = evidenceLoaded && currentSnapshot.caseRecord().getStatus() == CaseState.FORENSIC_REVIEW;
        boolean verificationReady = evidenceLoaded && currentSnapshot.evidence().hasVerificationSnapshot();
        boolean hashesMatch = verificationReady && currentSnapshot.evidence().hashesMatch();
        boolean decisionPending = evidenceLoaded && currentSnapshot.evidence().getStatus() == EvidenceStatus.UPLOADED;

        if (browseFileButton != null) {
            browseFileButton.setDisable(!officerSignedIn);
        }
        if (uploadEvidenceButton != null) {
            uploadEvidenceButton.setDisable(!officerSignedIn || selectedFilePath == null);
        }
        if (loadSnapshotButton != null) {
            loadSnapshotButton.setDisable(currentUser == null);
        }
        if (verifyIntegrityButton != null) {
            verifyIntegrityButton.setDisable(!analystSignedIn || !evidenceLoaded);
        }
        if (markVerifiedButton != null) {
            markVerifiedButton.setDisable(!analystSignedIn || !inForensicReview || !verificationReady || !hashesMatch || !decisionPending);
        }
        if (markTamperedButton != null) {
            markTamperedButton.setDisable(!analystSignedIn || !inForensicReview || !verificationReady || hashesMatch || !decisionPending);
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

    private void ensureRole(UserRole expectedRole, String message) {
        if (currentUser == null || currentUser.getRole() != expectedRole) {
            throw new IllegalStateException(message);
        }
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

    private String buildUploadStatus(EvidenceUploadResult result) {
        return "Evidence uploaded as file " + result.originalFileName()
                + ". SHA-256: " + result.sha256Hash()
                + ". Case state moved to " + formatCaseState(result.caseState()) + ".";
    }

    private String buildVerificationStatus(IntegrityVerificationResult result) {
        if (result.matched()) {
            return "Integrity verification completed. Hashes match and the case is now in "
                    + formatCaseState(result.caseState()) + ".";
        }

        return "Integrity verification completed. A hash mismatch was detected and the case remains ready for analyst action.";
    }

    private String buildDecisionStatus(String decisionLabel, EvidenceDecisionResult result) {
        if ("verified".equalsIgnoreCase(decisionLabel)) {
            return "Evidence marked as verified. Case state remains " + formatCaseState(result.caseState())
                    + " until the assigned Investigating Officer submits it for Supervisor Review.";
        }

        return "Evidence marked as " + decisionLabel
                + ". Evidence status is now " + result.evidenceStatus().getDisplayName()
                + " and case state is " + formatCaseState(result.caseState()) + ".";
    }

    private String formatCaseState(CaseState caseState) {
        if (caseState == null) {
            return "Unknown";
        }

        return formatEnumName(caseState.name());
    }

    private String formatEnumName(String value) {
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

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatTimestamp(LocalDateTime value) {
        return value == null ? "Not available" : value.format(TIMESTAMP_FORMATTER);
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
