
USE CDIEM;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Adeel Farooq' AND Role = 'OFFICER')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Adeel Farooq', 'OFFICER');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Sana Malik' AND Role = 'OFFICER')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Sana Malik', 'OFFICER');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Hamza Qureshi' AND Role = 'OFFICER')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Hamza Qureshi', 'OFFICER');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Hina Raza' AND Role = 'ANALYST')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Hina Raza', 'ANALYST');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Bilal Ahmed' AND Role = 'ANALYST')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Bilal Ahmed', 'ANALYST');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Sarah Khan' AND Role = 'SUPERVISOR')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Sarah Khan', 'SUPERVISOR');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Name = 'Omar Siddiqui' AND Role = 'SUPERVISOR')
BEGIN
    INSERT INTO dbo.Users (Name, Role) VALUES ('Omar Siddiqui', 'SUPERVISOR');
END
GO

IF COL_LENGTH('dbo.Cases', 'Description') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD Description NVARCHAR(1000) NULL;
END
GO

IF COL_LENGTH('dbo.Cases', 'RelatedInfo') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD RelatedInfo NVARCHAR(1000) NULL;
END
GO

IF COL_LENGTH('dbo.Cases', 'AssignedOfficerID') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD AssignedOfficerID INT NULL;
END
GO

IF COL_LENGTH('dbo.Cases', 'AssignedOfficerName') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD AssignedOfficerName NVARCHAR(100) NULL;
END
GO

DECLARE @DefaultOfficerId INT;
SELECT TOP (1) @DefaultOfficerId = UserID
FROM dbo.Users
WHERE Role = 'OFFICER'
ORDER BY UserID;

IF @DefaultOfficerId IS NULL
BEGIN
    THROW 50001, 'At least one OFFICER user must exist before running this migration.', 1;
END
GO

UPDATE dbo.Cases
SET Description = COALESCE(NULLIF(Description, ''), 'Migrated case description pending refinement.')
WHERE Description IS NULL OR LTRIM(RTRIM(Description)) = '';
GO

UPDATE dbo.Cases
SET RelatedInfo = NULLIF(RelatedInfo, '')
WHERE RelatedInfo IS NOT NULL;
GO

DECLARE @OfficerForAssignment INT;
DECLARE @OwnerBackfillSql NVARCHAR(MAX);

SELECT TOP (1) @OfficerForAssignment = UserID
FROM dbo.Users
WHERE Role = 'OFFICER'
ORDER BY UserID;

SET @OwnerBackfillSql = N'
UPDATE dbo.Cases
SET AssignedOfficerID = COALESCE(AssignedOfficerID'
    + CASE
        WHEN COL_LENGTH('dbo.Cases', 'CreatedBy') IS NOT NULL THEN N', CreatedBy'
        ELSE N''
      END
    + N', @OfficerForAssignment)
WHERE AssignedOfficerID IS NULL;';

EXEC sp_executesql
    @OwnerBackfillSql,
    N'@OfficerForAssignment INT',
    @OfficerForAssignment = @OfficerForAssignment;
GO

UPDATE c
SET AssignedOfficerName = u.Name
FROM dbo.Cases c
JOIN dbo.Users u
    ON u.UserID = c.AssignedOfficerID
WHERE c.AssignedOfficerName IS NULL
   OR LTRIM(RTRIM(c.AssignedOfficerName)) = '';
GO

DECLARE @StatusCheckBeforeUpdate sysname;
SELECT TOP (1) @StatusCheckBeforeUpdate = cc.name
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID('dbo.Cases')
  AND cc.definition LIKE '%Status%';

IF @StatusCheckBeforeUpdate IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @StatusCheckBeforeUpdate + ']');
END
GO

DECLARE @StatusDefaultBeforeUpdate sysname;
SELECT TOP (1) @StatusDefaultBeforeUpdate = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c
    ON c.object_id = dc.parent_object_id
   AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID('dbo.Cases')
  AND c.name = 'Status';

IF @StatusDefaultBeforeUpdate IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @StatusDefaultBeforeUpdate + ']');
END
GO

UPDATE dbo.Cases
SET Status = CASE
    WHEN Status = 'OPEN' THEN 'CASE_CREATED'
    WHEN Status = 'UNDER_REVIEW' THEN 'SUPERVISOR_REVIEW'
    WHEN Status = 'REOPENED' THEN 'FORENSIC_REVIEW'
    ELSE Status
END;
GO

DECLARE @StatusCheck sysname;
SELECT TOP (1) @StatusCheck = cc.name
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID('dbo.Cases')
  AND cc.definition LIKE '%Status%';

IF @StatusCheck IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @StatusCheck + ']');
END
GO

DECLARE @SeverityCheck sysname;
SELECT TOP (1) @SeverityCheck = cc.name
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID('dbo.Cases')
  AND cc.definition LIKE '%Severity%';

IF @SeverityCheck IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @SeverityCheck + ']');
END
GO

DECLARE @StatusDefault sysname;
SELECT TOP (1) @StatusDefault = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c
    ON c.object_id = dc.parent_object_id
   AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID('dbo.Cases')
  AND c.name = 'Status';

IF @StatusDefault IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @StatusDefault + ']');
END
GO

ALTER TABLE dbo.Cases ALTER COLUMN Description NVARCHAR(1000) NOT NULL;
GO

ALTER TABLE dbo.Cases ALTER COLUMN AssignedOfficerID INT NOT NULL;
GO

ALTER TABLE dbo.Cases ALTER COLUMN AssignedOfficerName NVARCHAR(100) NOT NULL;
GO

ALTER TABLE dbo.Cases ALTER COLUMN Status NVARCHAR(30) NOT NULL;
GO

ALTER TABLE dbo.Cases
ADD CONSTRAINT CK_Cases_Severity
CHECK (Severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));
GO

ALTER TABLE dbo.Cases
ADD CONSTRAINT CK_Cases_Status
CHECK (Status IN ('CASE_CREATED', 'EVIDENCE_UPLOADED', 'FORENSIC_REVIEW', 'SUPERVISOR_REVIEW', 'FROZEN', 'CASE_REASSIGNED', 'CLOSED'));
GO

ALTER TABLE dbo.Cases
ADD CONSTRAINT DF_Cases_Status DEFAULT 'CASE_CREATED' FOR Status;
GO

DECLARE @CreatedByForeignKey sysname;
SELECT TOP (1) @CreatedByForeignKey = fk.name
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc
    ON fkc.constraint_object_id = fk.object_id
JOIN sys.columns c
    ON c.object_id = fkc.parent_object_id
   AND c.column_id = fkc.parent_column_id
WHERE fk.parent_object_id = OBJECT_ID('dbo.Cases')
  AND c.name = 'CreatedBy';

IF @CreatedByForeignKey IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @CreatedByForeignKey + ']');
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_Cases_AssignedOfficer'
      AND parent_object_id = OBJECT_ID('dbo.Cases')
)
BEGIN
    ALTER TABLE dbo.Cases
    ADD CONSTRAINT FK_Cases_AssignedOfficer
        FOREIGN KEY (AssignedOfficerID) REFERENCES dbo.Users(UserID);
END
GO

IF COL_LENGTH('dbo.Cases', 'CreatedBy') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Cases DROP COLUMN CreatedBy;
END
GO
