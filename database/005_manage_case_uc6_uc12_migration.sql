USE CDIEM;
GO

IF COL_LENGTH('dbo.Cases', 'SlaHours') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD SlaHours INT NULL;
END
GO

IF COL_LENGTH('dbo.Cases', 'PriorityState') IS NULL
BEGIN
    ALTER TABLE dbo.Cases ADD PriorityState NVARCHAR(30) NULL;
END
GO

UPDATE dbo.Cases
SET SlaHours = CASE Severity
    WHEN 'LOW' THEN 120
    WHEN 'MEDIUM' THEN 72
    WHEN 'HIGH' THEN 24
    WHEN 'CRITICAL' THEN 8
    ELSE 72
END
WHERE SlaHours IS NULL;
GO

UPDATE dbo.Cases
SET PriorityState = CASE Severity
    WHEN 'CRITICAL' THEN 'ESCALATED'
    WHEN 'HIGH' THEN 'PRIORITY'
    ELSE 'STANDARD'
END
WHERE PriorityState IS NULL OR LTRIM(RTRIM(PriorityState)) = '';
GO

DECLARE @PriorityCheck sysname;
SELECT TOP (1) @PriorityCheck = cc.name
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID('dbo.Cases')
  AND cc.definition LIKE '%PriorityState%';

IF @PriorityCheck IS NOT NULL
BEGIN
    EXEC('ALTER TABLE dbo.Cases DROP CONSTRAINT [' + @PriorityCheck + ']');
END
GO

ALTER TABLE dbo.Cases ALTER COLUMN SlaHours INT NOT NULL;
GO

ALTER TABLE dbo.Cases ALTER COLUMN PriorityState NVARCHAR(30) NOT NULL;
GO

ALTER TABLE dbo.Cases
ADD CONSTRAINT CK_Cases_PriorityState
CHECK (PriorityState IN ('STANDARD', 'PRIORITY', 'ESCALATED', 'UNDER_ACTIVE_REVIEW'));
GO
