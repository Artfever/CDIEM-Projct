USE CDIEM;
GO

IF COL_LENGTH('dbo.AuditLogs', 'Action') IS NOT NULL
BEGIN
    ALTER TABLE dbo.AuditLogs ALTER COLUMN Action NVARCHAR(500) NOT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.AuditLogs')
      AND name = 'IX_AuditLogs_CaseID_Timestamp'
)
BEGIN
    CREATE INDEX IX_AuditLogs_CaseID_Timestamp
        ON dbo.AuditLogs (CaseID, [Timestamp] DESC, LogID DESC);
END
GO


SELECT * FROM Cases;
SELECT * FROM Evidence;