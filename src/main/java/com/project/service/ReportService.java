package com.project.service;

import com.project.model.CaseState;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.SummaryReportCaseFilter;
import com.project.model.SummaryReportCaseRecord;
import com.project.model.SummaryReportRequest;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public class ReportService {
    private static final DateTimeFormatter EXPORT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String PDF_HEADER = "%PDF-1.4\n";
    private static final String PDF_LINE_BREAK = "\n";
    private static final int PDF_LINES_PER_PAGE = 48;

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ReportService() {
        this(new CaseRepositoryImpl(), new UserRepositoryImpl(), new AuditRepositoryImpl());
    }

    public ReportService(CaseRepository caseRepository, UserRepository userRepository, AuditRepository auditRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.auditLogService = new AuditLogService(auditRepository);
    }

    public SummaryReportResult generateSummaryReport(SummaryReportRequest request, int userId) {
        SummaryReportRequest validatedRequest = validateRequest(request);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User currentUser = userRepository.findById(connection, userId)
                        .orElseThrow(() -> new IllegalArgumentException("User " + userId + " does not exist."));
                if (currentUser.getRole() != UserRole.SUPERVISOR) {
                    throw new IllegalStateException("Only the Supervisory Authority can generate summary reports.");
                }

                LocalDateTime generatedAt = LocalDateTime.now();
                List<SummaryReportCaseRecord> matchingCases =
                        caseRepository.findCasesForSummaryReport(connection, validatedRequest);
                SummaryReportResult result = buildResult(validatedRequest, matchingCases, currentUser, generatedAt);

                auditLogService.recordAudit(
                        connection,
                        null,
                        buildAuditMessage(validatedRequest, result),
                        currentUser.getUserId()
                );

                connection.commit();
                return result;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapQueryException(e);
            }
        } catch (SQLException e) {
            throw wrapQueryException(e);
        }
    }

    public void exportCsv(SummaryReportResult result, Path targetPath) {
        ensureReportAvailable(result);
        ensureTargetPath(targetPath);

        try {
            Files.writeString(targetPath, buildCsv(result), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not export the summary report as CSV.", e);
        }
    }

    public void exportPdf(SummaryReportResult result, Path targetPath) {
        ensureReportAvailable(result);
        ensureTargetPath(targetPath);

        try {
            Files.write(targetPath, buildPdf(result));
        } catch (IOException e) {
            throw new IllegalStateException("Could not export the summary report as PDF.", e);
        }
    }

    private SummaryReportRequest validateRequest(SummaryReportRequest request) {
        SummaryReportRequest safeRequest = request == null
                ? new SummaryReportRequest(null, null, SummaryReportCaseFilter.ALL, null)
                : request;

        LocalDate fromDate = safeRequest.fromDate();
        LocalDate toDate = safeRequest.toDate();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date must be on or before the To date.");
        }

        return safeRequest;
    }

    private SummaryReportResult buildResult(SummaryReportRequest request, List<SummaryReportCaseRecord> matchingCases,
                                            User currentUser, LocalDateTime generatedAt) {
        EnumMap<CaseState, Long> statusCounts = new EnumMap<>(CaseState.class);
        for (CaseState caseState : CaseState.values()) {
            statusCounts.put(caseState, 0L);
        }

        EnumMap<SeverityLevel, Long> severityCounts = new EnumMap<>(SeverityLevel.class);
        for (SeverityLevel severityLevel : SeverityLevel.values()) {
            severityCounts.put(severityLevel, 0L);
        }

        long slaCompliantCount = 0;
        long slaBreachedCount = 0;
        long frozenCaseCount = 0;
        long tamperedCaseCount = 0;

        for (SummaryReportCaseRecord caseRecord : matchingCases) {
            statusCounts.merge(caseRecord.caseState(), 1L, Long::sum);
            if (caseRecord.severity() != null) {
                severityCounts.merge(caseRecord.severity(), 1L, Long::sum);
            }

            if (caseRecord.hasSlaData()) {
                if (caseRecord.isSlaCompliant(generatedAt)) {
                    slaCompliantCount++;
                } else {
                    slaBreachedCount++;
                }
            }

            if (caseRecord.isFrozenCase()) {
                frozenCaseCount++;
            }
            if (caseRecord.hasTamperedEvidence()) {
                tamperedCaseCount++;
            }
        }

        return new SummaryReportResult(
                request,
                currentUser.getName(),
                currentUser.getUserId(),
                generatedAt,
                matchingCases,
                statusCounts,
                severityCounts,
                matchingCases.size(),
                slaCompliantCount,
                slaBreachedCount,
                frozenCaseCount,
                tamperedCaseCount
        );
    }

    private String buildAuditMessage(SummaryReportRequest request, SummaryReportResult result) {
        String fromDate = request.fromDate() == null ? "unbounded" : request.fromDate().toString();
        String toDate = request.toDate() == null ? "unbounded" : request.toDate().toString();
        String priority = request.priorityState() == null ? "ANY" : request.priorityState().name();

        return "SUMMARY_REPORT_GENERATED dateRange=" + fromDate + ".." + toDate
                + ", caseFilter=" + request.caseFilter().name()
                + ", priority=" + priority
                + ", matched=" + result.matchedCaseCount()
                + ", breached=" + result.slaBreachedCount()
                + ", tampered=" + result.tamperedCaseCount();
    }

    private String buildCsv(SummaryReportResult result) {
        StringBuilder builder = new StringBuilder();

        appendCsvRow(builder, "Summary Report Metadata");
        appendCsvRow(builder, "Generated By", result.generatedByName());
        appendCsvRow(builder, "Generated At", EXPORT_TIMESTAMP.format(result.generatedAt()));
        appendCsvRow(builder, "From Date", formatDate(result.request().fromDate()));
        appendCsvRow(builder, "To Date", formatDate(result.request().toDate()));
        appendCsvRow(builder, "Case Filter", result.request().caseFilter().getDisplayName());
        appendCsvRow(builder, "Priority Filter", formatPriority(result.request().priorityState()));
        builder.append(PDF_LINE_BREAK);

        appendCsvRow(builder, "Summary Metrics");
        appendCsvRow(builder, "Matched Cases", String.valueOf(result.matchedCaseCount()));
        appendCsvRow(builder, "SLA Compliant", String.valueOf(result.slaCompliantCount()));
        appendCsvRow(builder, "SLA Breached", String.valueOf(result.slaBreachedCount()));
        appendCsvRow(builder, "Frozen Cases", String.valueOf(result.frozenCaseCount()));
        appendCsvRow(builder, "Tampered Evidence Cases", String.valueOf(result.tamperedCaseCount()));
        builder.append(PDF_LINE_BREAK);

        appendCsvRow(builder, "Status Distribution");
        appendCsvRow(builder, "Status", "Count");
        for (CaseState caseState : CaseState.values()) {
            appendCsvRow(builder, formatEnumName(caseState.name()), String.valueOf(result.statusCounts().get(caseState)));
        }
        builder.append(PDF_LINE_BREAK);

        appendCsvRow(builder, "Severity Distribution");
        appendCsvRow(builder, "Severity", "Count");
        for (SeverityLevel severityLevel : SeverityLevel.values()) {
            appendCsvRow(builder, formatEnumName(severityLevel.name()),
                    String.valueOf(result.severityCounts().get(severityLevel)));
        }
        builder.append(PDF_LINE_BREAK);

        appendCsvRow(builder, "Matching Cases");
        appendCsvRow(builder, "Case ID", "Title", "State", "Severity", "Priority", "Assigned Officer",
                "Created At", "Closed At", "Evidence Status", "SLA Due At", "SLA Outcome");
        for (SummaryReportCaseRecord caseRecord : result.matchingCases()) {
            appendCsvRow(
                    builder,
                    String.valueOf(caseRecord.caseId()),
                    defaultText(caseRecord.title(), "Untitled Case"),
                    formatEnumName(caseRecord.caseState().name()),
                    formatEnumName(caseRecord.severity() == null ? null : caseRecord.severity().name()),
                    formatPriority(caseRecord.priorityState()),
                    defaultText(caseRecord.assignedOfficerName(), "Unassigned"),
                    formatDateTime(caseRecord.createdAt()),
                    formatDateTime(caseRecord.closedAt()),
                    formatEvidenceStatus(caseRecord),
                    formatDateTime(caseRecord.dueAt()),
                    formatSlaOutcome(caseRecord, result.generatedAt())
            );
        }

        return builder.toString();
    }

    private byte[] buildPdf(SummaryReportResult result) {
        List<String> wrappedLines = new ArrayList<>();
        for (String line : buildPdfLines(result)) {
            wrappedLines.addAll(wrapLine(line, 92));
        }

        List<String> pagedContent = new ArrayList<>();
        for (int start = 0; start < wrappedLines.size(); start += PDF_LINES_PER_PAGE) {
            int end = Math.min(start + PDF_LINES_PER_PAGE, wrappedLines.size());
            pagedContent.add(buildPdfPageContent(wrappedLines.subList(start, end)));
        }

        StringBuilder body = new StringBuilder(PDF_HEADER);
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        int pageCount = pagedContent.size();
        int pagesObjectNumber = 2;
        int fontObjectNumber = 3;

        appendPdfObject(body, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            if (i > 0) {
                kids.append(' ');
            }
            kids.append(4 + (i * 2)).append(" 0 R");
        }
        appendPdfObject(body, offsets, pagesObjectNumber,
                "<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
        appendPdfObject(body, offsets, fontObjectNumber,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        for (int i = 0; i < pageCount; i++) {
            int pageObjectNumber = 4 + (i * 2);
            int contentObjectNumber = pageObjectNumber + 1;
            String content = pagedContent.get(i);

            appendPdfObject(body, offsets, pageObjectNumber,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                            + "/Resources << /Font << /F1 3 0 R >> >> "
                            + "/Contents " + contentObjectNumber + " 0 R >>");
            appendPdfStreamObject(body, offsets, contentObjectNumber, content);
        }

        int xrefOffset = body.toString().getBytes(StandardCharsets.US_ASCII).length;
        body.append("xref\n");
        body.append("0 ").append(offsets.size()).append('\n');
        body.append("0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            body.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
        }
        body.append("trailer\n");
        body.append("<< /Size ").append(offsets.size()).append(" /Root 1 0 R >>\n");
        body.append("startxref\n");
        body.append(xrefOffset).append('\n');
        body.append("%%EOF");

        return body.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private List<String> buildPdfLines(SummaryReportResult result) {
        List<String> lines = new ArrayList<>();
        lines.add("CDIEM Summary Report");
        lines.add("Generated by: " + result.generatedByName() + " (User ID " + result.generatedByUserId() + ")");
        lines.add("Generated at: " + EXPORT_TIMESTAMP.format(result.generatedAt()));
        lines.add("Date range: " + formatDate(result.request().fromDate()) + " to " + formatDate(result.request().toDate()));
        lines.add("Case filter: " + result.request().caseFilter().getDisplayName());
        lines.add("Priority filter: " + formatPriority(result.request().priorityState()));
        lines.add("");
        lines.add("Summary Metrics");
        lines.add("Matched cases: " + result.matchedCaseCount());
        lines.add("SLA compliant: " + result.slaCompliantCount());
        lines.add("SLA breached: " + result.slaBreachedCount());
        lines.add("Frozen cases: " + result.frozenCaseCount());
        lines.add("Tampered evidence cases: " + result.tamperedCaseCount());
        lines.add("");
        lines.add("Status Distribution");
        for (CaseState caseState : CaseState.values()) {
            lines.add(" - " + formatEnumName(caseState.name()) + ": " + result.statusCounts().get(caseState));
        }
        lines.add("");
        lines.add("Severity Distribution");
        for (SeverityLevel severityLevel : SeverityLevel.values()) {
            lines.add(" - " + formatEnumName(severityLevel.name()) + ": " + result.severityCounts().get(severityLevel));
        }
        lines.add("");
        lines.add("Matching Cases");

        if (result.matchingCases().isEmpty()) {
            lines.add("No cases matched the selected parameters.");
            return lines;
        }

        for (SummaryReportCaseRecord caseRecord : result.matchingCases()) {
            lines.add("Case #" + caseRecord.caseId() + " | " + defaultText(caseRecord.title(), "Untitled Case"));
            lines.add("State: " + formatEnumName(caseRecord.caseState().name())
                    + " | Severity: " + formatEnumName(caseRecord.severity() == null ? null : caseRecord.severity().name())
                    + " | Priority: " + formatPriority(caseRecord.priorityState()));
            lines.add("Officer: " + defaultText(caseRecord.assignedOfficerName(), "Unassigned")
                    + " | Evidence: " + formatEvidenceStatus(caseRecord));
            lines.add("Created: " + formatDateTime(caseRecord.createdAt())
                    + " | Closed: " + formatDateTime(caseRecord.closedAt())
                    + " | SLA: " + formatSlaOutcome(caseRecord, result.generatedAt()));
            lines.add("");
        }

        return lines;
    }

    private String buildPdfPageContent(List<String> pageLines) {
        StringBuilder builder = new StringBuilder();
        builder.append("BT\n");
        builder.append("/F1 10 Tf\n");
        builder.append("50 760 Td\n");
        builder.append("14 TL\n");

        boolean firstLine = true;
        for (String line : pageLines) {
            if (!firstLine) {
                builder.append("T*\n");
            }
            builder.append('(').append(escapePdf(line)).append(") Tj\n");
            firstLine = false;
        }

        builder.append("ET");
        return builder.toString();
    }

    private List<String> wrapLine(String value, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            lines.add("");
            return lines;
        }

        String remaining = sanitizePdfText(value);
        while (remaining.length() > maxLength) {
            int splitIndex = remaining.lastIndexOf(' ', maxLength);
            if (splitIndex <= 0) {
                splitIndex = maxLength;
            }
            lines.add(remaining.substring(0, splitIndex));
            remaining = remaining.substring(splitIndex).trim();
        }

        lines.add(remaining);
        return lines;
    }

    private void appendPdfObject(StringBuilder body, List<Integer> offsets, int objectNumber, String objectBody) {
        offsets.add(body.toString().getBytes(StandardCharsets.US_ASCII).length);
        body.append(objectNumber).append(" 0 obj\n");
        body.append(objectBody).append("\n");
        body.append("endobj\n");
    }

    private void appendPdfStreamObject(StringBuilder body, List<Integer> offsets, int objectNumber,
                                       String streamContent) {
        byte[] streamBytes = streamContent.getBytes(StandardCharsets.US_ASCII);
        offsets.add(body.toString().getBytes(StandardCharsets.US_ASCII).length);
        body.append(objectNumber).append(" 0 obj\n");
        body.append("<< /Length ").append(streamBytes.length).append(" >>\n");
        body.append("stream\n");
        body.append(streamContent).append('\n');
        body.append("endstream\n");
        body.append("endobj\n");
    }

    private void appendCsvRow(StringBuilder builder, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"')
                    .append(values[i] == null ? "" : values[i].replace("\"", "\"\""))
                    .append('"');
        }
        builder.append(PDF_LINE_BREAK);
    }

    private void ensureReportAvailable(SummaryReportResult result) {
        if (result == null) {
            throw new IllegalStateException("Generate a summary report before exporting it.");
        }
    }

    private void ensureTargetPath(Path targetPath) {
        if (targetPath == null) {
            throw new IllegalArgumentException("A destination file is required.");
        }

        try {
            Path parent = targetPath.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare the export destination.", e);
        }
    }

    private RuntimeException wrapQueryException(Exception exception) {
        if (exception instanceof SQLException sqlException && isMissingReportSchema(sqlException)) {
            return new IllegalStateException(
                    "Summary report schema is missing. Apply database migrations through 012_summary_report_module.sql.",
                    sqlException
            );
        }

        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new IllegalStateException("Could not generate the summary report.", exception);
    }

    private boolean isMissingReportSchema(SQLException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Invalid column name 'ClosedAt'")
                || message.contains("Invalid object name 'Evidence'")
                || message.contains("Invalid column name 'IntegrityStatus'");
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? "Any" : value.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Not available" : EXPORT_TIMESTAMP.format(value);
    }

    private String formatPriority(PriorityState priorityState) {
        return priorityState == null ? "Any Priority" : priorityState.getDisplayName();
    }

    private String formatEvidenceStatus(SummaryReportCaseRecord caseRecord) {
        return caseRecord.latestEvidenceStatus() == null
                ? "No Evidence"
                : formatEnumName(caseRecord.latestEvidenceStatus().name());
    }

    private String formatSlaOutcome(SummaryReportCaseRecord caseRecord, LocalDateTime referenceTime) {
        if (!caseRecord.hasSlaData()) {
            return "Unavailable";
        }

        return caseRecord.isSlaCompliant(referenceTime) ? "Compliant" : "Breached";
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

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String sanitizePdfText(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            builder.append(character <= 0x7F ? character : '?');
        }
        return builder.toString();
    }

    private String escapePdf(String value) {
        return sanitizePdfText(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
