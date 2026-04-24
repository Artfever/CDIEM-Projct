package com.project.controller;

import com.project.model.CaseState;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.SummaryReportCaseFilter;
import com.project.model.SummaryReportCaseRecord;
import com.project.model.SummaryReportRequest;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.ReportService;
import com.project.service.SummaryReportResult;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReportController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";
    private static final String ANY_PRIORITY_LABEL = "Any Priority";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm a");
    private static final DateTimeFormatter EXPORT_FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label identityLabel;

    @FXML
    private Label accessSummaryLabel;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private ComboBox<SummaryReportCaseFilter> caseFilterBox;

    @FXML
    private ComboBox<String> priorityFilterBox;

    @FXML
    private Button generateReportButton;

    @FXML
    private Button resetFiltersButton;

    @FXML
    private FlowPane overviewMetricsPane;

    @FXML
    private FlowPane statusDistributionPane;

    @FXML
    private FlowPane severityDistributionPane;

    @FXML
    private VBox matchingCasesContainer;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private Label generatedByValueLabel;

    @FXML
    private Label generatedAtValueLabel;

    @FXML
    private Label dateRangeValueLabel;

    @FXML
    private Label caseFilterValueLabel;

    @FXML
    private Label priorityFilterValueLabel;

    @FXML
    private Label matchedCasesValueLabel;

    @FXML
    private Label slaSummaryValueLabel;

    @FXML
    private Label frozenSummaryValueLabel;

    @FXML
    private Label tamperedSummaryValueLabel;

    @FXML
    private Button exportCsvButton;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Label statusLabel;

    private final ReportService reportService = new ReportService();
    private User currentUser;
    private SummaryReportResult currentReport;

    @FXML
    public void initialize() {
        configureFilters();
        resetReportView();
        setStatus("Generate a summary report to review case trends and export the result.", STATUS_NEUTRAL);
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
    public void generateReport() {
        try {
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can generate summary reports.");

            SummaryReportRequest request = new SummaryReportRequest(
                    fromDatePicker.getValue(),
                    toDatePicker.getValue(),
                    caseFilterBox.getValue(),
                    parsePriorityFilter(priorityFilterBox.getValue())
            );
            currentReport = reportService.generateSummaryReport(request, currentUser.getUserId());
            renderReport(currentReport);
            updateActionAvailability();

            if (currentReport.matchedCaseCount() == 0) {
                setStatus("Summary report generated. No cases matched the selected parameters.", STATUS_NEUTRAL);
            } else {
                setStatus("Summary report generated for " + currentReport.matchedCaseCount() + " matching case(s).",
                        STATUS_SUCCESS);
            }
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void clearFilters() {
        if (fromDatePicker != null) {
            fromDatePicker.setValue(null);
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(null);
        }
        if (caseFilterBox != null) {
            caseFilterBox.getSelectionModel().select(SummaryReportCaseFilter.ALL);
        }
        if (priorityFilterBox != null) {
            priorityFilterBox.getSelectionModel().select(ANY_PRIORITY_LABEL);
        }

        currentReport = null;
        resetReportView();
        updateActionAvailability();
        setStatus("Report filters have been reset.", STATUS_NEUTRAL);
    }

    @FXML
    public void exportCsv() {
        try {
            ensureReportReadyForExport();

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Summary Report as CSV");
            chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            chooser.setInitialFileName(buildDefaultExportFileName("csv"));

            File file = chooser.showSaveDialog(exportCsvButton.getScene().getWindow());
            if (file == null) {
                return;
            }

            reportService.exportCsv(currentReport, file.toPath());
            setStatus("Summary report exported as CSV.", STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void exportPdf() {
        try {
            ensureReportReadyForExport();

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Summary Report as PDF");
            chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName(buildDefaultExportFileName("pdf"));

            File file = chooser.showSaveDialog(exportPdfButton.getScene().getWindow());
            if (file == null) {
                return;
            }

            reportService.exportPdf(currentReport, file.toPath());
            setStatus("Summary report exported as PDF.", STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void configureFilters() {
        caseFilterBox.getItems().setAll(SummaryReportCaseFilter.values());
        caseFilterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SummaryReportCaseFilter value) {
                return value == null ? SummaryReportCaseFilter.ALL.getDisplayName() : value.getDisplayName();
            }

            @Override
            public SummaryReportCaseFilter fromString(String string) {
                return SummaryReportCaseFilter.ALL;
            }
        });
        caseFilterBox.getSelectionModel().select(SummaryReportCaseFilter.ALL);

        priorityFilterBox.getItems().setAll(ANY_PRIORITY_LABEL);
        for (PriorityState priorityState : PriorityState.values()) {
            priorityFilterBox.getItems().add(priorityState.getDisplayName());
        }
        priorityFilterBox.getSelectionModel().select(ANY_PRIORITY_LABEL);
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || roleLabel == null) {
            return;
        }

        boolean supervisorSignedIn = currentUser.getRole() == UserRole.SUPERVISOR;
        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(currentUser.getName() + " | " + buildUsername(currentUser) + " | ID " + currentUser.getUserId());
        subtitleLabel.setText(supervisorSignedIn
                ? "Generate aggregate case intelligence across status, priority, SLA, and tamper outcomes."
                : "This module is reserved for the Supervisory Authority.");
        accessSummaryLabel.setText(supervisorSignedIn
                ? "Supervisory Authority users can filter by date range, report case filter, and priority state. Each report records an audit entry and can be exported as CSV or PDF."
                : "Only Supervisory Authority users can generate or export summary reports.");

        if (supervisorSignedIn) {
            setStatus("Signed in as Supervisory Authority. Summary report generation is available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available only to Supervisory Authority users.", STATUS_ERROR);
        }

        updateActionAvailability();
    }

    private void renderReport(SummaryReportResult result) {
        generatedByValueLabel.setText(result.generatedByName() + " | User ID " + result.generatedByUserId());
        generatedAtValueLabel.setText(formatTimestamp(result.generatedAt()));
        dateRangeValueLabel.setText(buildDateRangeLabel(result.request().fromDate(), result.request().toDate()));
        caseFilterValueLabel.setText(result.request().caseFilter().getDisplayName());
        priorityFilterValueLabel.setText(result.request().priorityState() == null
                ? ANY_PRIORITY_LABEL
                : result.request().priorityState().getDisplayName());
        matchedCasesValueLabel.setText(String.valueOf(result.matchedCaseCount()));
        slaSummaryValueLabel.setText(result.slaCompliantCount() + " compliant | " + result.slaBreachedCount() + " breached");
        frozenSummaryValueLabel.setText(String.valueOf(result.frozenCaseCount()));
        tamperedSummaryValueLabel.setText(String.valueOf(result.tamperedCaseCount()));

        renderOverviewMetrics(result);
        renderStatusDistribution(result);
        renderSeverityDistribution(result);
        renderMatchingCases(result);
    }

    private void renderOverviewMetrics(SummaryReportResult result) {
        overviewMetricsPane.getChildren().setAll(
                buildMetricCard("Matched Cases", String.valueOf(result.matchedCaseCount()), "metric-card-strong"),
                buildMetricCard("SLA Compliant", String.valueOf(result.slaCompliantCount())),
                buildMetricCard("SLA Breached", String.valueOf(result.slaBreachedCount()), "metric-card-alert"),
                buildMetricCard("Frozen Cases", String.valueOf(result.frozenCaseCount())),
                buildMetricCard("Tampered Cases", String.valueOf(result.tamperedCaseCount()), "metric-card-alert")
        );
    }

    private void renderStatusDistribution(SummaryReportResult result) {
        statusDistributionPane.getChildren().clear();
        for (CaseState caseState : CaseState.values()) {
            statusDistributionPane.getChildren().add(
                    buildMetricCard(formatEnumName(caseState.name()),
                            String.valueOf(result.statusCounts().get(caseState)))
            );
        }
    }

    private void renderSeverityDistribution(SummaryReportResult result) {
        severityDistributionPane.getChildren().clear();
        for (SeverityLevel severityLevel : SeverityLevel.values()) {
            severityDistributionPane.getChildren().add(
                    buildMetricCard(formatEnumName(severityLevel.name()),
                            String.valueOf(result.severityCounts().get(severityLevel)))
            );
        }
    }

    private void renderMatchingCases(SummaryReportResult result) {
        matchingCasesContainer.getChildren().clear();

        boolean empty = result.matchingCases().isEmpty();
        emptyStateLabel.setVisible(empty);
        emptyStateLabel.setManaged(empty);

        if (empty) {
            emptyStateLabel.setText("No cases matched the selected parameters.");
            return;
        }

        for (SummaryReportCaseRecord caseRecord : result.matchingCases()) {
            matchingCasesContainer.getChildren().add(buildCaseCard(caseRecord, result.generatedAt()));
        }
    }

    private VBox buildMetricCard(String labelText, String valueText, String... additionalStyles) {
        VBox card = new VBox(6.0);
        card.getStyleClass().add("metric-card");
        card.getStyleClass().addAll(additionalStyles);
        card.setPadding(new Insets(16.0));
        card.setPrefWidth(160.0);

        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        label.setWrapText(true);

        Label value = new Label(valueText);
        value.getStyleClass().add("metric-value");

        card.getChildren().addAll(label, value);
        return card;
    }

    private VBox buildCaseCard(SummaryReportCaseRecord caseRecord, LocalDateTime reportGeneratedAt) {
        VBox card = new VBox(10.0);
        card.getStyleClass().add("case-card");
        card.setPadding(new Insets(16.0));

        HBox headerRow = new HBox(10.0);
        Label caseIdLabel = new Label("Case #" + caseRecord.caseId());
        caseIdLabel.getStyleClass().add("case-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stateChip = buildChip(formatEnumName(caseRecord.caseState().name()), "chip-status");
        Label evidenceChip = buildChip(formatEvidenceStatus(caseRecord), "chip-evidence");
        headerRow.getChildren().addAll(caseIdLabel, spacer, stateChip, evidenceChip);

        Label titleLabel = new Label(defaultText(caseRecord.title(), "Untitled Case"));
        titleLabel.getStyleClass().add("case-summary");
        titleLabel.setWrapText(true);

        Label contextLabel = new Label(
                "Assigned Officer: " + defaultText(caseRecord.assignedOfficerName(), "Unassigned")
                        + " | Severity: " + formatEnumName(caseRecord.severity() == null ? null : caseRecord.severity().name())
                        + " | Priority: " + formatPriority(caseRecord.priorityState())
        );
        contextLabel.getStyleClass().add("case-meta");
        contextLabel.setWrapText(true);

        Label lifecycleLabel = new Label(
                "Created: " + formatTimestamp(caseRecord.createdAt())
                        + " | Closed: " + formatTimestamp(caseRecord.closedAt())
                        + " | SLA Due: " + formatTimestamp(caseRecord.dueAt())
                        + " | SLA: " + formatSlaOutcome(caseRecord, reportGeneratedAt)
        );
        lifecycleLabel.getStyleClass().add("case-meta");
        lifecycleLabel.setWrapText(true);

        card.getChildren().addAll(headerRow, titleLabel, contextLabel, lifecycleLabel);
        return card;
    }

    private Label buildChip(String text, String styleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("chip", styleClass);
        return chip;
    }

    private void resetReportView() {
        generatedByValueLabel.setText("No report generated");
        generatedAtValueLabel.setText("Unavailable");
        dateRangeValueLabel.setText("Any");
        caseFilterValueLabel.setText(SummaryReportCaseFilter.ALL.getDisplayName());
        priorityFilterValueLabel.setText(ANY_PRIORITY_LABEL);
        matchedCasesValueLabel.setText("0");
        slaSummaryValueLabel.setText("Awaiting report");
        frozenSummaryValueLabel.setText("0");
        tamperedSummaryValueLabel.setText("0");

        overviewMetricsPane.getChildren().setAll(buildMetricCard("Awaiting Report", "Generate"));
        statusDistributionPane.getChildren().setAll(buildMetricCard("No Status Data", "--"));
        severityDistributionPane.getChildren().setAll(buildMetricCard("No Severity Data", "--"));
        matchingCasesContainer.getChildren().clear();
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setManaged(true);
        emptyStateLabel.setText("Generate a report to review matching cases.");
    }

    private void updateActionAvailability() {
        boolean supervisorSignedIn = currentUser != null && currentUser.getRole() == UserRole.SUPERVISOR;
        boolean reportAvailable = currentReport != null;

        setNodeDisabled(generateReportButton, !supervisorSignedIn);
        setNodeDisabled(resetFiltersButton, currentUser == null);
        setNodeDisabled(exportCsvButton, !supervisorSignedIn || !reportAvailable);
        setNodeDisabled(exportPdfButton, !supervisorSignedIn || !reportAvailable);
        setNodeDisabled(fromDatePicker, !supervisorSignedIn);
        setNodeDisabled(toDatePicker, !supervisorSignedIn);
        setNodeDisabled(caseFilterBox, !supervisorSignedIn);
        setNodeDisabled(priorityFilterBox, !supervisorSignedIn);
    }

    private void setNodeDisabled(Node node, boolean disabled) {
        if (node == null) {
            return;
        }

        node.setDisable(disabled);
    }

    private void ensureRole(UserRole expectedRole, String message) {
        if (currentUser == null || currentUser.getRole() != expectedRole) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureReportReadyForExport() {
        ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can export summary reports.");
        if (currentReport == null) {
            throw new IllegalStateException("Generate a summary report before exporting it.");
        }
    }

    private PriorityState parsePriorityFilter(String selectedPriority) {
        if (selectedPriority == null || ANY_PRIORITY_LABEL.equals(selectedPriority)) {
            return null;
        }

        for (PriorityState priorityState : PriorityState.values()) {
            if (priorityState.getDisplayName().equals(selectedPriority)) {
                return priorityState;
            }
        }

        throw new IllegalArgumentException("Selected priority filter is invalid.");
    }

    private String buildDefaultExportFileName(String extension) {
        LocalDateTime stamp = currentReport == null ? LocalDateTime.now() : currentReport.generatedAt();
        return "summary-report-" + EXPORT_FILE_STAMP.format(stamp) + "." + extension;
    }

    private String buildDateRangeLabel(LocalDate fromDate, LocalDate toDate) {
        String from = fromDate == null ? "Any" : fromDate.toString();
        String to = toDate == null ? "Any" : toDate.toString();
        return from + " to " + to;
    }

    private String formatEvidenceStatus(SummaryReportCaseRecord caseRecord) {
        return caseRecord.latestEvidenceStatus() == null
                ? "No Evidence"
                : formatEnumName(caseRecord.latestEvidenceStatus().name());
    }

    private String formatSlaOutcome(SummaryReportCaseRecord caseRecord, LocalDateTime reportGeneratedAt) {
        if (!caseRecord.hasSlaData()) {
            return "Unavailable";
        }

        return caseRecord.isSlaCompliant(reportGeneratedAt) ? "Compliant" : "Breached";
    }

    private String formatPriority(PriorityState priorityState) {
        return priorityState == null ? ANY_PRIORITY_LABEL : priorityState.getDisplayName();
    }

    private String formatTimestamp(LocalDateTime value) {
        return value == null ? "Unavailable" : value.format(TIMESTAMP_FORMATTER);
    }

    private String formatEnumName(String value) {
        if (value == null || value.isBlank()) {
            return "Unavailable";
        }

        String[] tokens = value.toLowerCase(Locale.ROOT).split("_");
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

    private String buildUsername(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return "n/a";
        }

        return "@" + user.getUsername();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_SUCCESS, STATUS_ERROR);
        statusLabel.getStyleClass().add(stateClass);
    }

    private String getRootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage() == null ? "Unknown error." : current.getMessage();
    }
}
