USE CDIEM;
GO

IF COL_LENGTH('dbo.Cases', 'ClosedAt') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD ClosedAt DATETIME2 NULL;
END
GO

UPDATE c
SET ClosedAt = closureAudit.ClosedAt
FROM dbo.Cases c
OUTER APPLY (
    SELECT TOP (1) a.[Timestamp] AS ClosedAt
    FROM dbo.AuditLogs a
    WHERE a.CaseID = c.CaseID
      AND a.Action LIKE 'CASE_CLOSURE_APPROVED%'
    ORDER BY a.[Timestamp] DESC, a.LogID DESC
) closureAudit
WHERE c.Status = 'CLOSED'
  AND c.ClosedAt IS NULL
  AND closureAudit.ClosedAt IS NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Cases')
      AND name = 'IX_Cases_SummaryReportFilters'
)
BEGIN
    CREATE INDEX IX_Cases_SummaryReportFilters
        ON dbo.Cases (CreatedAt DESC, Status, PriorityState, CaseID DESC)
        INCLUDE (Title, Severity, SlaHours, AssignedOfficerName, ClosedAt);
END
GO
