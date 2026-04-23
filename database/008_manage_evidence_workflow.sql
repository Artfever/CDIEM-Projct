USE CDIEM;
GO

IF OBJECT_ID('dbo.Evidence', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Evidence (
        EvidenceID INT IDENTITY(1,1) PRIMARY KEY,
        CaseID INT NOT NULL,
        OriginalFileName NVARCHAR(255) NOT NULL,
        StoredFilePath NVARCHAR(500) NOT NULL,
        OriginalSHA256 CHAR(64) NOT NULL,
        RecalculatedSHA256 CHAR(64) NULL,
        IntegrityStatus NVARCHAR(20) NOT NULL
            CONSTRAINT DF_Evidence_IntegrityStatus DEFAULT 'UPLOADED'
            CHECK (IntegrityStatus IN ('UPLOADED', 'VERIFIED', 'TAMPERED')),
        UploadedByUserID INT NOT NULL,
        UploadedAt DATETIME2 NOT NULL
            CONSTRAINT DF_Evidence_UploadedAt DEFAULT SYSUTCDATETIME(),
        LastVerifiedByUserID INT NULL,
        LastVerifiedAt DATETIME2 NULL,
        CONSTRAINT FK_Evidence_Case
            FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID),
        CONSTRAINT FK_Evidence_UploadedByUser
            FOREIGN KEY (UploadedByUserID) REFERENCES dbo.Users(UserID),
        CONSTRAINT FK_Evidence_LastVerifiedByUser
            FOREIGN KEY (LastVerifiedByUserID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Evidence')
      AND name = 'IX_Evidence_CaseID_UploadedAt'
)
BEGIN
    CREATE INDEX IX_Evidence_CaseID_UploadedAt
        ON dbo.Evidence (CaseID, UploadedAt DESC, EvidenceID DESC);
END
GO
