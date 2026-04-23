USE CDIEM;
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
    THROW 50004, 'At least one OFFICER user must exist before running this migration.', 1;
END
GO

DECLARE @OwnerBackfillOfficerId INT;
DECLARE @OwnerBackfillSql NVARCHAR(MAX);

SELECT TOP (1) @OwnerBackfillOfficerId = UserID
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
    + N', @OwnerBackfillOfficerId)
WHERE AssignedOfficerID IS NULL;';

EXEC sp_executesql
    @OwnerBackfillSql,
    N'@OwnerBackfillOfficerId INT',
    @OwnerBackfillOfficerId = @OwnerBackfillOfficerId;
GO

UPDATE c
SET AssignedOfficerName = u.Name
FROM dbo.Cases c
JOIN dbo.Users u
    ON u.UserID = c.AssignedOfficerID
WHERE c.AssignedOfficerName IS NULL
   OR LTRIM(RTRIM(c.AssignedOfficerName)) = '';
GO

ALTER TABLE dbo.Cases ALTER COLUMN AssignedOfficerID INT NOT NULL;
GO

ALTER TABLE dbo.Cases ALTER COLUMN AssignedOfficerName NVARCHAR(100) NOT NULL;
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
