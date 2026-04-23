USE CDIEM;
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