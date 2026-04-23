USE CDIEM;
GO

IF COL_LENGTH('dbo.Users', 'Username') IS NULL
BEGIN
    ALTER TABLE dbo.Users ADD Username NVARCHAR(50) NULL;
END
GO

IF COL_LENGTH('dbo.Users', 'Email') IS NULL
BEGIN
    ALTER TABLE dbo.Users ADD Email NVARCHAR(120) NULL;
END
GO

IF COL_LENGTH('dbo.Users', 'PasswordHash') IS NULL
BEGIN
    ALTER TABLE dbo.Users ADD PasswordHash CHAR(64) NULL;
END
GO

UPDATE dbo.Users
SET Username = 'adeel',
    Email = 'adeel@cdiem.local'
WHERE Name = 'Adeel Farooq' AND Role = 'OFFICER';
GO

UPDATE dbo.Users
SET Username = 'sana',
    Email = 'sana@cdiem.local'
WHERE Name = 'Sana Malik' AND Role = 'OFFICER';
GO

UPDATE dbo.Users
SET Username = 'hamza',
    Email = 'hamza@cdiem.local'
WHERE Name = 'Hamza Qureshi' AND Role = 'OFFICER';
GO

UPDATE dbo.Users
SET Username = 'hina',
    Email = 'hina@cdiem.local'
WHERE Name = 'Hina Raza' AND Role = 'ANALYST';
GO

UPDATE dbo.Users
SET Username = 'bilal',
    Email = 'bilal@cdiem.local'
WHERE Name = 'Bilal Ahmed' AND Role = 'ANALYST';
GO

UPDATE dbo.Users
SET Username = 'sarah',
    Email = 'sarah@cdiem.local'
WHERE Name = 'Sarah Khan' AND Role = 'SUPERVISOR';
GO

UPDATE dbo.Users
SET Username = 'omar',
    Email = 'omar@cdiem.local'
WHERE Name = 'Omar Siddiqui' AND Role = 'SUPERVISOR';
GO

UPDATE dbo.Users
SET Username = CONCAT(LOWER(REPLACE(REPLACE(Name, ' ', '.'), '''', '')), '.', UserID)
WHERE Username IS NULL OR LTRIM(RTRIM(Username)) = '';
GO

UPDATE dbo.Users
SET Email = CONCAT(LOWER(REPLACE(REPLACE(Name, ' ', '.'), '''', '')), '.', UserID, '@cdiem.local')
WHERE Email IS NULL OR LTRIM(RTRIM(Email)) = '';
GO

UPDATE dbo.Users
SET PasswordHash = 'f2558afb40832ff11937129112bdfddc860299edfc1c44cc31e4339de08445e6'
WHERE PasswordHash IS NULL OR LTRIM(RTRIM(PasswordHash)) = '';
GO

IF EXISTS (
    SELECT Username
    FROM dbo.Users
    GROUP BY Username
    HAVING COUNT(*) > 1
)
BEGIN
    THROW 50002, 'Duplicate usernames found. Resolve them before enforcing unique login access.', 1;
END
GO

IF EXISTS (
    SELECT Email
    FROM dbo.Users
    GROUP BY Email
    HAVING COUNT(*) > 1
)
BEGIN
    THROW 50003, 'Duplicate emails found. Resolve them before enforcing unique login access.', 1;
END
GO

ALTER TABLE dbo.Users ALTER COLUMN Username NVARCHAR(50) NOT NULL;
GO

ALTER TABLE dbo.Users ALTER COLUMN Email NVARCHAR(120) NOT NULL;
GO

ALTER TABLE dbo.Users ALTER COLUMN PasswordHash CHAR(64) NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Users')
      AND name = 'UX_Users_Username'
)
BEGIN
    CREATE UNIQUE INDEX UX_Users_Username ON dbo.Users(Username);
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Users')
      AND name = 'UX_Users_Email'
)
BEGIN
    CREATE UNIQUE INDEX UX_Users_Email ON dbo.Users(Email);
END
GO
