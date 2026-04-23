USE CDIEM;
GO

IF OBJECT_ID('dbo.EscalatedCaseReviews', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.EscalatedCaseReviews (
        ReviewID INT IDENTITY(1,1) PRIMARY KEY,
        CaseID INT NOT NULL,
        Instructions NVARCHAR(500) NOT NULL,
        PreviousPriorityState NVARCHAR(30) NOT NULL
            CHECK (PreviousPriorityState IN ('STANDARD', 'PRIORITY', 'ESCALATED', 'UNDER_ACTIVE_REVIEW')),
        ResultingPriorityState NVARCHAR(30) NOT NULL
            CHECK (ResultingPriorityState IN ('STANDARD', 'PRIORITY', 'ESCALATED', 'UNDER_ACTIVE_REVIEW')),
        ReviewedByUserID INT NOT NULL,
        ReviewedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_EscalatedCaseReviews_Case
            FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID),
        CONSTRAINT FK_EscalatedCaseReviews_User
            FOREIGN KEY (ReviewedByUserID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.EscalatedCaseReviews')
      AND name = 'IX_EscalatedCaseReviews_CaseID_ReviewedAt'
)
BEGIN
    CREATE INDEX IX_EscalatedCaseReviews_CaseID_ReviewedAt
        ON dbo.EscalatedCaseReviews (CaseID, ReviewedAt DESC, ReviewID DESC);
END
GO


    private int generateUniqueCaseID(int storedCaseId) {
  88:         return idGenerator.generateUniqueCaseID(storedCaseId);
  89:     }
  90: }

