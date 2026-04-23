USE CDIEM;
GO

IF OBJECT_ID('dbo.CaseClosureDecisions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CaseClosureDecisions (
        DecisionID INT IDENTITY(1,1) PRIMARY KEY,
        CaseID INT NOT NULL,
        DecisionType NVARCHAR(20) NOT NULL
            CHECK (DecisionType IN ('APPROVED', 'REJECTED')),
        Reason NVARCHAR(500) NULL,
        PreviousState NVARCHAR(30) NOT NULL
            CHECK (PreviousState IN ('CASE_CREATED', 'EVIDENCE_UPLOADED', 'FORENSIC_REVIEW',
                                     'SUPERVISOR_REVIEW', 'FROZEN', 'CASE_REASSIGNED', 'CLOSED')),
        ResultingState NVARCHAR(30) NOT NULL
            CHECK (ResultingState IN ('CASE_CREATED', 'EVIDENCE_UPLOADED', 'FORENSIC_REVIEW',
                                      'SUPERVISOR_REVIEW', 'FROZEN', 'CASE_REASSIGNED', 'CLOSED')),
        DecidedByUserID INT NOT NULL,
        DecidedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_CaseClosureDecisions_Case
            FOREIGN KEY (CaseID) REFERENCES dbo.Cases(CaseID),
        CONSTRAINT FK_CaseClosureDecisions_User
            FOREIGN KEY (DecidedByUserID) REFERENCES dbo.Users(UserID),
        CONSTRAINT CK_CaseClosureDecisions_Reason
            CHECK (DecisionType = 'APPROVED'
                   OR LEN(LTRIM(RTRIM(ISNULL(Reason, '')))) > 0)
    );
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.CaseClosureDecisions')
      AND name = 'IX_CaseClosureDecisions_CaseID_DecidedAt'
)
BEGIN
    CREATE INDEX IX_CaseClosureDecisions_CaseID_DecidedAt
        ON dbo.CaseClosureDecisions (CaseID, DecidedAt DESC, DecisionID DESC);
END
GO
