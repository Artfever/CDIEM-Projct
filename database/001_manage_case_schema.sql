USE CDIEM;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserID INT IDENTITY(1,1) PRIMARY KEY,
        Name NVARCHAR(100) NOT NULL,
        Role NVARCHAR(20) NOT NULL
            CHECK (Role IN ('OFFICER', 'ANALYST', 'SUPERVISOR'))
    );
END
GO

IF OBJECT_ID('dbo.Cases', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Cases (
        CaseID INT IDENTITY(1,1) PRIMARY KEY,
        Title NVARCHAR(255) NOT NULL,
        Description NVARCHAR(1000) NOT NULL,
        RelatedInfo NVARCHAR(1000) NULL,
        Severity NVARCHAR(20) NOT NULL
            CHECK (Severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
        SlaHours INT NOT NULL,
        PriorityState NVARCHAR(30) NOT NULL
            CHECK (PriorityState IN ('STANDARD', 'PRIORITY', 'ESCALATED', 'UNDER_ACTIVE_REVIEW')),
        Status NVARCHAR(30) NOT NULL DEFAULT 'CASE_CREATED'
            CHECK (Status IN ('CASE_CREATED', 'EVIDENCE_UPLOADED', 'FORENSIC_REVIEW', 'SUPERVISOR_REVIEW', 'FROZEN', 'CASE_REASSIGNED', 'CLOSED')),
        AssignedOfficerID INT NOT NULL,
        AssignedOfficerName NVARCHAR(100) NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Cases_AssignedOfficer
            FOREIGN KEY (AssignedOfficerID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID('dbo.AuditLogs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AuditLogs (
        LogID INT IDENTITY(1,1) PRIMARY KEY,
        CaseID INT NULL,
        Action NVARCHAR(255) NOT NULL,
        PerformedBy INT NOT NULL,
        [Timestamp] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_AuditLogs_Case
            FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID),
        CONSTRAINT FK_AuditLogs_User
            FOREIGN KEY (PerformedBy) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID('dbo.Notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications (
        NotificationID INT IDENTITY(1,1) PRIMARY KEY,
        CaseID INT NULL,
        RecipientUserID INT NOT NULL,
        SentByUserID INT NOT NULL,
        Message NVARCHAR(255) NOT NULL,
        Channel NVARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Notifications_Case
            FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID),
        CONSTRAINT FK_Notifications_RecipientUser
            FOREIGN KEY (RecipientUserID) REFERENCES dbo.Users(UserID),
        CONSTRAINT FK_Notifications_SentByUser
            FOREIGN KEY (SentByUserID) REFERENCES dbo.Users(UserID)
    );
END
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
