USE CDIEM;
GO

-- Keep normal foreign keys in place.
-- The application now deletes child rows itself before deleting a case,
-- which avoids reference conflicts without relying on cascading deletes.

IF OBJECT_ID('dbo.Cases', 'U') IS NOT NULL
    AND OBJECT_ID('dbo.AuditLogs', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_AuditLogs_Case')
    BEGIN
        ALTER TABLE dbo.AuditLogs DROP CONSTRAINT FK_AuditLogs_Case;
    END

    ALTER TABLE dbo.AuditLogs
    ADD CONSTRAINT FK_AuditLogs_Case
        FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID);
END
GO

IF OBJECT_ID('dbo.Cases', 'U') IS NOT NULL
    AND OBJECT_ID('dbo.Notifications', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Notifications_Case')
    BEGIN
        ALTER TABLE dbo.Notifications DROP CONSTRAINT FK_Notifications_Case;
    END

    ALTER TABLE dbo.Notifications
    ADD CONSTRAINT FK_Notifications_Case
        FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID);
END
GO

IF OBJECT_ID('dbo.Cases', 'U') IS NOT NULL
    AND OBJECT_ID('dbo.Evidence', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Evidence_Case')
    BEGIN
        ALTER TABLE dbo.Evidence DROP CONSTRAINT FK_Evidence_Case;
    END

    ALTER TABLE dbo.Evidence
    ADD CONSTRAINT FK_Evidence_Case
        FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID);
END
GO

IF OBJECT_ID('dbo.Cases', 'U') IS NOT NULL
    AND OBJECT_ID('dbo.CaseClosureDecisions', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_CaseClosureDecisions_Case')
    BEGIN
        ALTER TABLE dbo.CaseClosureDecisions DROP CONSTRAINT FK_CaseClosureDecisions_Case;
    END

    ALTER TABLE dbo.CaseClosureDecisions
    ADD CONSTRAINT FK_CaseClosureDecisions_Case
        FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID);
END
GO

IF OBJECT_ID('dbo.Cases', 'U') IS NOT NULL
    AND OBJECT_ID('dbo.EscalatedCaseReviews', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_EscalatedCaseReviews_Case')
    BEGIN
        ALTER TABLE dbo.EscalatedCaseReviews DROP CONSTRAINT FK_EscalatedCaseReviews_Case;
    END

    ALTER TABLE dbo.EscalatedCaseReviews
    ADD CONSTRAINT FK_EscalatedCaseReviews_Case
        FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID);
END
GO
