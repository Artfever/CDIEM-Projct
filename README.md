# CDIEM Project

## Overview
CDIEM is a role-based JavaFX desktop application for managing investigation cases and digital evidence with Microsoft SQL Server as the persistence layer. The system covers the full case lifecycle from case registration and evidence intake to forensic verification, tamper handling, supervisory review, closure decisions, escalation review, notifications, and reporting.

This repository implements a layered desktop system with JavaFX controllers, service classes, JDBC repositories, SQL migrations, FXML views, and local evidence storage. The project is centered around 15 core use cases, and a detailed class-to-use-case traceability document is already included in `UC1_to_UC15_System_Class_Mapping.txt`.

## Main Capabilities
- Role-based login and sign-up for three actors: Investigating Officer, Digital Forensic Analyst, and Supervisory Authority.
- Case registration with severity, officer assignment, SLA calculation, and priority-state tracking.
- Secure evidence upload into local storage with SHA-256 hashing and metadata persistence.
- Evidence integrity verification through hash recalculation and comparison.
- Evidence decision workflows for marking evidence as verified or tampered.
- Manual or automatic case freezing when evidence tampering is detected.
- Case reopening, officer reassignment, and severity updates.
- Submission of cases for supervisor review after forensic handling.
- Closure approval and closure rejection with recorded decision history.
- System notifications for review, reassignment, reopening, and closure events.
- Immutable audit logging for chain-of-custody visibility.
- Escalated-case review for breached SLA cases.
- Summary-report generation with CSV and PDF export support.

## Supported Roles
- Investigating Officer: registers cases, uploads evidence, views notifications, and submits cases for supervisor review.
- Digital Forensic Analyst: verifies evidence integrity, marks evidence as verified or tampered, freezes cases, and views notifications.
- Supervisory Authority: updates severity, reassigns officers, reopens frozen cases, approves or rejects closure, reviews escalated cases, inspects chain-of-custody logs, generates reports, and views notifications.

## Core Use Cases
1. Case registration.
2. Upload digital evidence.
3. Verify evidence integrity.
4. Freeze case due to tampered evidence.
5. Approve case closure.
6. Update case severity level.
7. Mark evidence as tampered.
8. Reopen frozen case.
9. Mark evidence as verified.
10. Generate summary report.
11. Submit case for supervisor review.
12. Reassign investigating officer.
13. View chain-of-custody log.
14. Review escalated case.
15. Reject case closure.

## Workflow Summary
The typical business flow starts when an Investigating Officer creates a case and uploads evidence. A Digital Forensic Analyst then verifies the evidence by recalculating its SHA-256 hash and decides whether the evidence is verified or tampered. Tampered evidence can freeze the case, while verified evidence keeps the case moving through forensic handling until the officer submits it for supervisor review.

At the supervisory stage, a Supervisory Authority can update severity, reopen frozen cases, reassign officers, inspect chain-of-custody history, generate summary reports, review escalated cases, and either approve or reject case closure. Notifications and audit records are persisted throughout the workflow so that state changes and actor actions remain traceable.

## Architecture
- `src/main/java/com/project/controller`: JavaFX controllers for login, dashboard navigation, case workflows, evidence workflows, reporting, notifications, escalated review, closure management, and chain-of-custody viewing.
- `src/main/java/com/project/service`: business logic for authentication, case management, evidence handling, hashing, secure storage, SLA logic, notifications, escalation, closure decisions, and reporting.
- `src/main/java/com/project/repository`: JDBC repositories for users, cases, evidence, notifications, audit logs, closure decisions, and escalated-case reviews.
- `src/main/java/com/project/model`: domain models and enums such as `Case`, `Evidence`, `User`, `CaseState`, `PriorityState`, `SeverityLevel`, `EvidenceStatus`, and report DTOs.
- `src/main/java/com/project/util`: shared utilities including database connection management, scene navigation, password hashing, and ID generation.
- `src/main/resources/view`: FXML screens and CSS stylesheets for the JavaFX user interface.
- `database`: ordered SQL migration scripts used to build the CDIEM schema and feature modules.
- `storage/evidence`: local evidence files stored outside the database, grouped by case.

## Domain Model
- A case stores its title, description, related information, severity, SLA hours, priority state, workflow state, assigned officer, creation timestamp, and closure timestamp.
- Evidence records store the original filename, stored file path, original SHA-256 hash, recalculated SHA-256 hash, integrity status, upload metadata, and verification metadata.
- Audit logs capture immutable system actions and support the chain-of-custody view.
- Notifications persist system-generated messages exchanged between officers, analysts, and supervisors.
- Closure decisions and escalated-case reviews are stored in dedicated tables so supervisory decisions remain independently traceable.

## Technology Stack
- Java 21
- JavaFX 17
- Maven
- Microsoft SQL Server
- JDBC via `mssql-jdbc`
- FXML and CSS for the desktop UI

## Database Schema and Migrations
The SQL scripts in the `database` folder are designed to be applied in numeric order:

1. `001_manage_case_schema.sql`: creates the base `Users`, `Cases`, `AuditLogs`, and `Notifications` tables and seeds initial users.
2. `002_manage_case_module1_migration.sql`: extends the case-management schema for richer case registration fields.
3. `003_auth_and_dashboard_structure.sql`: adds usernames, emails, password hashes, and login constraints.
4. `004_cleanup_legacy_seed_data.sql`: cleans older seed data.
5. `005_manage_case_uc6_uc12_migration.sql`: adds SLA and priority-state support.
6. `006_case_assignment_owner_migration.sql`: finalizes assignment ownership fields.
7. `007_notification_service_migration.sql`: ensures notification-table support.
8. `008_manage_evidence_workflow.sql`: creates the `Evidence` table and supporting index.
9. `009_manage_case_state_transitions.sql`: improves audit-log support for case state transitions.
10. `010_manage_case_closure.sql`: creates `CaseClosureDecisions`.
11. `011_review_escalated_case.sql`: creates `EscalatedCaseReviews`.
12. `012_summary_report_module.sql`: adds `ClosedAt` support and reporting indexes.

`database/testing.sql` contains simple inspection queries for manual database checks.

## Configuration
Create `config/db.properties` from `config/db.properties.example` and provide your SQL Server connection details:

```properties
db.url=jdbc:sqlserver://localhost\SQLEXPRESS;databaseName=CDIEM;encrypt=true;trustServerCertificate=true
db.user=YOUR_SQL_USER
db.password=YOUR_SQL_PASSWORD
```

The application also supports environment variables instead of the properties file:

- `CDIEM_DB_URL`
- `CDIEM_DB_USER`
- `CDIEM_DB_PASSWORD`

## Seeded Accounts
After applying `003_auth_and_dashboard_structure.sql`, the seeded users can log in with password `CDIEM@123`.

| Role | Name | Username | Email |
| --- | --- | --- | --- |
| Investigating Officer | Adeel Farooq | `adeel` | `adeel@cdiem.local` |
| Investigating Officer | Sana Malik | `sana` | `sana@cdiem.local` |
| Investigating Officer | Hamza Qureshi | `hamza` | `hamza@cdiem.local` |
| Digital Forensic Analyst | Hina Raza | `hina` | `hina@cdiem.local` |
| Digital Forensic Analyst | Bilal Ahmed | `bilal` | `bilal@cdiem.local` |
| Supervisory Authority | Sarah Khan | `sarah` | `sarah@cdiem.local` |
| Supervisory Authority | Omar Siddiqui | `omar` | `omar@cdiem.local` |

New accounts can also be created through the sign-up screen.

## Running the Application
### Prerequisites
- JDK 21
- Maven
- Microsoft SQL Server with a `CDIEM` database
- The SQL migration scripts applied in order
- Valid database credentials in `config/db.properties` or environment variables

### Run in Development
```bash
mvn clean javafx:run
```

### Build the Project
```bash
mvn clean package
```

The Maven artifact is currently produced under `target` with the artifact name `JavaFXSQLApp`.

## Security and Traceability Notes
- User passwords are stored as SHA-256 hashes.
- Evidence integrity is verified by recalculating SHA-256 hashes from stored files.
- Evidence files are copied into `storage/evidence/case-{id}` using sanitized filenames and timestamped unique names.
- Audit logging is used throughout the workflow to preserve a chain-of-custody history.

## Current Implementation Notes
- Evidence verification is split into two stages: UC3 stores the hash comparison snapshot, while UC7 and UC9 finalize the tampered or verified decision.
- Verified evidence does not automatically move a case into supervisor review; the dedicated submission workflow is used for that transition.
- Chain-of-custody inspection is represented through the UI/service layer instead of a separate persisted case state.
- Escalated-case review records instructions and updates priority state, but it does not currently send notifications.
- Closure rejection currently requires a nonblank reason.

## Repository Notes
- A compiled build output already exists in `target`.
- Sample evidence storage content already exists under `storage/evidence`.
- There is currently no `src/test` test suite in this repository, so validation is mainly driven through the UI, database state, and manual workflow testing.

## Reference
For class-level traceability of the business requirements, review `UC1_to_UC15_System_Class_Mapping.txt`.
