USE CDIEM;
GO

DECLARE @LegacyOfficerId INT;
DECLARE @LegacyAnalystId INT;
DECLARE @LegacySupervisorId INT;
DECLARE @OfficerReplacementId INT;
DECLARE @AnalystReplacementId INT;
DECLARE @SupervisorReplacementId INT;
DECLARE @LegacyOfficerReferenced BIT = 0;
DECLARE @LegacyAnalystReferenced BIT = 0;
DECLARE @LegacySupervisorReferenced BIT = 0;

SELECT @LegacyOfficerId = UserID
FROM dbo.Users
WHERE Name = 'Officer One' AND Role = 'OFFICER';

SELECT @LegacyAnalystId = UserID
FROM dbo.Users
WHERE Name = 'Analyst One' AND Role = 'ANALYST';

SELECT @LegacySupervisorId = UserID
FROM dbo.Users
WHERE Name = 'Supervisor One' AND Role = 'SUPERVISOR';

SELECT @OfficerReplacementId = UserID
FROM dbo.Users
WHERE Username = 'adeel' AND Role = 'OFFICER';

SELECT @AnalystReplacementId = UserID
FROM dbo.Users
WHERE Username = 'hina' AND Role = 'ANALYST';

SELECT @SupervisorReplacementId = UserID
FROM dbo.Users
WHERE Username = 'sarah' AND Role = 'SUPERVISOR';

IF @LegacyOfficerId IS NOT NULL AND @OfficerReplacementId IS NOT NULL AND @LegacyOfficerId <> @OfficerReplacementId
BEGIN
    UPDATE dbo.Cases
    SET AssignedOfficerID = @OfficerReplacementId,
        AssignedOfficerName = (
            SELECT Name
            FROM dbo.Users
            WHERE UserID = @OfficerReplacementId
        )
    WHERE AssignedOfficerID = @LegacyOfficerId;

    UPDATE dbo.AuditLogs
    SET PerformedBy = @OfficerReplacementId
    WHERE PerformedBy = @LegacyOfficerId;

    IF OBJECT_ID('dbo.Evidence', 'U') IS NOT NULL
    BEGIN
        UPDATE dbo.Evidence
        SET UploadedBy = @OfficerReplacementId
        WHERE UploadedBy = @LegacyOfficerId;
    END
END

IF @LegacyAnalystId IS NOT NULL AND @AnalystReplacementId IS NOT NULL AND @LegacyAnalystId <> @AnalystReplacementId
BEGIN
    UPDATE dbo.AuditLogs
    SET PerformedBy = @AnalystReplacementId
    WHERE PerformedBy = @LegacyAnalystId;

    IF OBJECT_ID('dbo.Evidence', 'U') IS NOT NULL
    BEGIN
        UPDATE dbo.Evidence
        SET UploadedBy = @AnalystReplacementId
        WHERE UploadedBy = @LegacyAnalystId;
    END
END

IF @LegacySupervisorId IS NOT NULL AND @SupervisorReplacementId IS NOT NULL AND @LegacySupervisorId <> @SupervisorReplacementId
BEGIN
    UPDATE dbo.AuditLogs
    SET PerformedBy = @SupervisorReplacementId
    WHERE PerformedBy = @LegacySupervisorId;
END

IF @LegacyOfficerId IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.Cases WHERE AssignedOfficerID = @LegacyOfficerId)
        SET @LegacyOfficerReferenced = 1;

    IF EXISTS (SELECT 1 FROM dbo.AuditLogs WHERE PerformedBy = @LegacyOfficerId)
        SET @LegacyOfficerReferenced = 1;

    IF OBJECT_ID('dbo.Evidence', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM dbo.Evidence WHERE UploadedBy = @LegacyOfficerId)
        SET @LegacyOfficerReferenced = 1;

    IF @LegacyOfficerReferenced = 0
    BEGIN
        DELETE FROM dbo.Users
        WHERE UserID = @LegacyOfficerId;
    END
END

IF @LegacyAnalystId IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.AuditLogs WHERE PerformedBy = @LegacyAnalystId)
        SET @LegacyAnalystReferenced = 1;

    IF OBJECT_ID('dbo.Evidence', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM dbo.Evidence WHERE UploadedBy = @LegacyAnalystId)
        SET @LegacyAnalystReferenced = 1;

    IF @LegacyAnalystReferenced = 0
    BEGIN
        DELETE FROM dbo.Users
        WHERE UserID = @LegacyAnalystId;
    END
END

IF @LegacySupervisorId IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.AuditLogs WHERE PerformedBy = @LegacySupervisorId)
        SET @LegacySupervisorReferenced = 1;

    IF @LegacySupervisorReferenced = 0
    BEGIN
        DELETE FROM dbo.Users
        WHERE UserID = @LegacySupervisorId;
    END
END
GO
