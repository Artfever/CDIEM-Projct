# 🔍 CDIEM - Digital Evidence Investigation Management

> A modern, role-based desktop application for managing criminal investigation cases and digital evidence with enterprise-grade audit trails and forensic verification workflows.

---

## 📋 Quick Navigation

- [✨ What is CDIEM?](#-what-is-cdiem)
- [🎯 Key Features](#-key-features)
- [👥 User Roles](#-user-roles)
- [🚀 Getting Started](#-getting-started)
- [🏗️ System Architecture](#-system-architecture)
- [📊 Core Workflows](#-core-workflows)
- [🗄️ Database Setup](#-database-setup)
- [💻 Technology Stack](#-technology-stack)
- [📚 Project Structure](#-project-structure)
- [🔒 Security & Compliance](#-security--compliance)

---

## ✨ What is CDIEM?

**CDIEM** (Criminal Digital Investigation & Evidence Management) is a comprehensive desktop application built with **JavaFX** for law enforcement agencies to manage digital investigations from case registration through closure. The system enforces strict chain-of-custody protocols, cryptographic evidence verification, and supervisory oversight.

The project is centered around **15 core use cases** with complete class-to-use-case traceability documented in `UseCase-to-System-Mapping.md`.



### Perfect for:
- 🚔 Law enforcement digital forensics teams
- 📋 Criminal investigation units needing centralized case management
- 🔐 Organizations requiring audit-compliant evidence handling
- ⚖️ Systems with multi-level supervisory oversight requirements

---

## 🎯 Key Features

| Feature | Description |
|---------|-------------|
| 🔑 **Role-Based Access** | Three distinct user roles with tailored workflows (Investigating Officer, Analyst, Supervisor) |
| 📁 **Evidence Management** | Secure upload, SHA-256 verification, tamper detection with immutable audit logs |
| ⏱️ **SLA Tracking** | Automatic SLA calculation, deadline monitoring, and escalation alerts |
| 🔄 **Case Workflows** | Complete lifecycle from registration → forensic analysis → supervisory review → closure |
| 🚨 **Tamper Detection** | Automatic case freezing when evidence integrity is compromised |
| 📊 **Reporting** | Summary reports with CSV and PDF export capabilities |
| 📢 **Real-Time Notifications** | Event-driven alerts for assignments, reviews, closures, and escalations |
| 📜 **Chain of Custody** | Immutable audit logging for complete case history visibility |
| 🔓 **Case Recovery** | Reopen frozen cases and reassign officers for continued investigation |

---

## 👥 User Roles

### 🕵️ Investigating Officer
- Register new cases with severity levels
- Upload digital evidence securely
- View notifications and case status
- Submit cases for supervisor review
- Receive notifications on case assignments and reviews

### 🔬 Digital Forensic Analyst
- Verify evidence integrity using SHA-256 hash verification
- Mark evidence as verified or tampered
- Auto-freeze cases upon tampering detection
- Monitor investigation notifications
- Document all verification decisions

### 👨‍⚖️ Supervisory Authority
- Approve or reject case closures with recorded decisions
- Update case severity and reassign officers
- Reopen frozen cases for further investigation
- Review SLA-breached escalated cases
- Generate detailed audit and summary reports
- Inspect complete chain-of-custody logs
- Manage system escalations and priorities

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or later
- **Maven 3.9+** (or use the included wrapper)
- **Microsoft SQL Server** (local or remote, 2019+)
- **Git** (for cloning the repository)

### 5-Minute Setup Guide

#### 1️⃣ Clone & Build
```bash
git clone <repository-url>
cd CDIEM
mvn clean install
```

#### 2️⃣ Configure Database Connection
Copy and edit the database configuration:
```bash
# Copy the configuration template
cp config/db.properties.example config/db.properties

# Edit config/db.properties with your SQL Server credentials
```

**Example configuration:**
```properties
db.url=jdbc:sqlserver://localhost\SQLEXPRESS;databaseName=CDIEM;encrypt=true;trustServerCertificate=true
db.user=YOUR_SQL_USER
db.password=YOUR_SQL_PASSWORD
```

**Or use environment variables:**
- `CDIEM_DB_URL`
- `CDIEM_DB_USER`
- `CDIEM_DB_PASSWORD`

#### 3️⃣ Initialize Database
Run all migration scripts **in numeric order** from the `database/` folder. On Windows with SQL Server installed:

```bash
sqlcmd -S localhost\SQLEXPRESS -d CDIEM -i database/001_manage_case_schema.sql
sqlcmd -S localhost\SQLEXPRESS -d CDIEM -i database/002_manage_case_module1_migration.sql
# ... continue with remaining scripts 003-012
```

Or use your preferred SQL Server client (SSMS, VS Code, etc.) to execute the scripts in order.

#### 4️⃣ Run the Application
```bash
mvn clean javafx:run
```

### 👤 Test Login Credentials

After running `003_auth_and_dashboard_structure.sql`, use these pre-seeded accounts:

| Role | Username | Password | Email |
|------|----------|----------|-------|
| 🕵️ Investigating Officer | `adeel` | `CDIEM@123` | adeel@cdiem.local |
| 🕵️ Investigating Officer | `sana` | `CDIEM@123` | sana@cdiem.local |
| 🕵️ Investigating Officer | `hamza` | `CDIEM@123` | hamza@cdiem.local |
| 🔬 Digital Forensic Analyst | `hina` | `CDIEM@123` | hina@cdiem.local |
| 🔬 Digital Forensic Analyst | `bilal` | `CDIEM@123` | bilal@cdiem.local |
| 👨‍⚖️ Supervisory Authority | `sarah` | `CDIEM@123` | sarah@cdiem.local |
| 👨‍⚖️ Supervisory Authority | `omar` | `CDIEM@123` | omar@cdiem.local |

New accounts can be created through the sign-up screen.

---

## 🏗️ System Architecture

### 📦 Layered Architecture

```
┌─────────────────────────────────────────────────┐
│  🖥️ JavaFX UI Layer (Controllers, FXML, CSS)   │
├─────────────────────────────────────────────────┤
│  ⚙️ Service Layer (Business Logic & Workflows)  │
├─────────────────────────────────────────────────┤
│  💾 Repository Layer (JDBC DAOs)                │
├─────────────────────────────────────────────────┤
│  🗄️ Microsoft SQL Server Database              │
└─────────────────────────────────────────────────┘
```

### 🗂️ Module Breakdown

| Module | Purpose | Key Components |
|--------|---------|----------------|
| **Controllers** | GUI interaction & navigation | `LoginController`, `DashboardController`, `CaseManagementController`, `EvidenceVerificationController` |
| **Services** | Business logic & workflows | `CaseService`, `EvidenceService`, `NotificationService`, `AuditService`, `ReportService` |
| **Repositories** | Data access layer (JDBC) | `CaseRepository`, `EvidenceRepository`, `UserRepository`, `AuditLogRepository`, `NotificationRepository` |
| **Models** | Domain objects & DTOs | `Case`, `Evidence`, `User`, `CaseState`, `EvidenceStatus`, `SeverityLevel` |
| **Utils** | Shared utilities | `DatabaseConnection`, `NavigationManager`, `SHA256Hashing`, `IDGenerator` |
| **Views** | FXML + CSS UI | `.fxml` screens & `.css` stylesheets in `src/main/resources/view/` |

---

## 📊 Core Workflows

### 1️⃣ Case Registration & Evidence Intake
```
Officer creates case
    ↓
System calculates SLA based on severity
    ↓
Officer uploads evidence files
    ↓
System computes SHA-256 hash & stores metadata
    ↓
Evidence stored securely in storage/evidence/{case-id}/
```

### 2️⃣ Evidence Verification & Tampering Detection
```
Analyst views pending evidence
    ↓
System recalculates SHA-256 hash
    ↓
Analyst compares original vs. recalculated hash
    ↓
Match? → Evidence marked verified, case continues
Mismatch? → Evidence marked tampered, case auto-frozen
```

### 3️⃣ Supervisory Review & Closure
```
Officer submits case for review
    ↓
Supervisor can:
  • Approve closure (final decision recorded)
  • Reject closure (returns to officer)
  • Reopen frozen case (if evidence tampering detected)
  • Reassign investigating officer
  • Escalate & review SLA-breached cases
    ↓
All decisions logged in immutable audit trail
```

### 4️⃣ Notifications & Escalations
```
Case reassignment → Notified to officer
Evidence tampering → Notified to analyst & supervisor
SLA breach → Escalated case flag & notification
Closure decision → Notified to relevant parties
All events → Immutably logged in audit trail
```

---

## 🗄️ Database Setup

### Sequential Migration Scripts

Run these scripts in **exact numeric order** to build the complete database schema:

| # | Script | Purpose |
|---|--------|---------|
| 1 | `001_manage_case_schema.sql` | Base tables: Users, Cases, AuditLogs, Notifications |
| 2 | `002_manage_case_module1_migration.sql` | Enhanced case registration fields |
| 3 | `003_auth_and_dashboard_structure.sql` | Authentication & user management + seeded accounts |
| 4 | `004_cleanup_legacy_seed_data.sql` | Data cleanup & normalization |
| 5 | `005_manage_case_uc6_uc12_migration.sql` | SLA & priority state support |
| 6 | `006_case_assignment_owner_migration.sql` | Officer assignment fields |
| 7 | `007_notification_service_migration.sql` | Notification infrastructure |
| 8 | `008_manage_evidence_workflow.sql` | Evidence table, hashing, & indexes |
| 9 | `009_manage_case_state_transitions.sql` | Enhanced audit logging |
| 10 | `010_manage_case_closure.sql` | Closure decision tracking table |
| 11 | `011_review_escalated_case.sql` | Escalated case reviews table |
| 12 | `012_summary_report_module.sql` | Reporting support & analytics |

### Database Inspection & Testing
For manual database checks, use `database/testing.sql` with sample inspection queries.

---

## 💻 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 21 |
| **UI Framework** | JavaFX | 17 |
| **Build Tool** | Maven | 3.9+ |
| **Database** | Microsoft SQL Server | 2019+ |
| **JDBC Driver** | mssql-jdbc | Latest |
| **UI Components** | ControlsFX | 11.2.1 |
| **Build Plugins** | maven-compiler-plugin, maven-surefire-plugin | Latest |

---

## 📚 Project Structure

```
CDIEM/
├── 📖 Documentation
│   ├── README.md                           # This file
│   ├── Architecture Rationale.md           # Design decisions
│   ├── UseCase-to-System-Mapping.md       # UC↔Class traceability
│   ├── NFRs.md                            # Non-functional requirements
│   └── RubricMapping.md                   # Rubric alignment
│
├── 📦 Source Code
│   └── src/main/java/com/project/
│       ├── controller/                    # JavaFX Controllers (UI logic)
│       ├── service/                       # Business Logic & Workflows
│       ├── repository/                    # Data Access Layer (JDBC)
│       ├── model/                         # Domain Models & Enums
│       ├── util/                          # Utilities (DB, Navigation, Hashing)
│       └── module-info.java              # Module definitions
│
├── 🎨 UI Resources
│   └── src/main/resources/view/           # FXML screens & CSS stylesheets
│
├── 🗄️ Database
│   ├── 001_manage_case_schema.sql        # (run in order)
│   ├── 002_manage_case_module1_migration.sql
│   ├── ... (continuing through 012)
│   ├── 012_summary_report_module.sql
│   └── testing.sql                        # Inspection queries
│
├── ⚙️ Configuration & Scripts
│   ├── config/
│   │   ├── db.properties                 # Database config (gitignored)
│   │   └── db.properties.example         # Configuration template
│   └── scripts/
│       ├── package-windows.ps1           # Windows packaging script
│       └── package-windows.cmd           # Windows packaging fallback
│
├── 💾 Evidence Storage
│   └── storage/evidence/                 # Local evidence files by case
│
├── 📦 Build Output
│   ├── pom.xml                           # Maven configuration
│   └── target/                           # Compiled classes & packaged app
│
└── 📝 License & Metadata
    └── Various configuration files
```

---

## 🔐 Security & Compliance

### 🛡️ Evidence Integrity
- SHA-256 cryptographic hashing for all evidence files
- Hash comparison for tamper detection
- Immutable storage of original hashes in database
- Automatic case freezing on tampering detection

### 🔑 Access Control
- Role-based authorization (Officer, Analyst, Supervisor)
- User authentication with password hashing
- Session management per user role
- Audit logging of all user actions

### 📋 Audit & Compliance
- Immutable audit logs for every system action
- Chain-of-custody visibility through audit trail
- Complete decision history (closures, escalations, assignments)
- Compliance-ready reporting with CSV & PDF export

---

## 🏃 Running the Application

### Development Mode
```bash
mvn clean javafx:run
```

### Build for Deployment
```bash
mvn clean package
```

The compiled artifact is produced under `target/` with name `JavaFXSQLApp-1.0-SNAPSHOT.jar`.

### Build a Windows Executable

Generate a standalone Windows application:

```powershell
.\scripts\package-windows.ps1
```

**Output location:** `dist\CDIEM\CDIEM.exe`

If PowerShell blocks execution:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

For a Windows installer instead:
```powershell
.\scripts\package-windows.ps1 -PackageType exe
```

*Note: WiX Toolset required for installer generation.*

---

## 📖 Domain Model Overview

### 🔹 Case
- Title, description, severity level, priority state
- Assigned investigating officer & supervisor
- SLA hours & deadline tracking
- Workflow state (Open, Frozen, Submitted, Closed)
- Creation & closure timestamps

### 🔹 Evidence
- Original filename & storage path
- Original & recalculated SHA-256 hashes
- Integrity status (Verified, Tampered, Pending)
- Upload & verification timestamps
- Complete chain-of-custody metadata

### 🔹 Audit Log
- Immutable record of all system actions
- Actor, action type, timestamp, and details
- Foundation for chain-of-custody reports
- Complete case history reconstruction

### 🔹 Notifications
- System-generated event alerts
- Event types: assignment, review, closure, tampering, escalation
- Read/unread tracking per user
- User-specific, role-based delivery

### 🔹 Closure Decisions & Escalated Reviews
- Independent tracking of supervisory decisions
- Decision timestamps & recorded rationale
- Complete audit trail for compliance

---

## 🎯 Core Use Cases (15 Total)

| # | Use Case | Actor | Purpose |
|---|----------|-------|---------|
| 1 | Case Registration | Officer | Create new investigation case |
| 2 | Upload Digital Evidence | Officer | Add evidence to case |
| 3 | Verify Evidence Integrity | Analyst | Hash-based verification |
| 4 | Freeze Case | Analyst/System | Freeze on tampering detection |
| 5 | Approve Case Closure | Supervisor | Finalize case decision |
| 6 | Update Severity Level | Supervisor | Adjust case priority |
| 7 | Mark Evidence Tampered | Analyst | Record evidence compromise |
| 8 | Reopen Frozen Case | Supervisor | Resume investigation |
| 9 | Mark Evidence Verified | Analyst | Record integrity confirmation |
| 10 | Generate Summary Report | Supervisor | Export case report |
| 11 | Submit for Review | Officer | Request supervisory review |
| 12 | Reassign Officer | Supervisor | Change case assignment |
| 13 | View Chain of Custody | Supervisor | Inspect audit history |
| 14 | Review Escalated Case | Supervisor | Handle SLA breaches |
| 15 | Reject Case Closure | Supervisor | Return case to officer |

---

## 🤝 Contributing

### Development Workflow
1. Create a feature branch: `git checkout -b feature/descriptive-name`
2. Follow existing code style & naming conventions
3. Test thoroughly with all three user roles
4. Commit with clear, descriptive messages
5. Create a pull request with documentation

### Code Style Guidelines
- Follow Java naming conventions (camelCase, PascalCase)
- Use meaningful class & method names that reflect intent
- Add Javadoc for public APIs
- Keep methods focused and single-responsibility
- Write self-documenting code

---

## 🔧 Troubleshooting

### Database Connection Issues
- ✅ Verify `config/db.properties` with correct SQL Server credentials
- ✅ Ensure SQL Server is running and accessible
- ✅ Check that `CDIEM` database exists and migrations ran in order
- ✅ Test connection with SQL Server Management Studio first

### Build Issues
- ✅ Ensure Java 21 is installed: `java -version`
- ✅ Rebuild clean: `mvn clean install -U`
- ✅ Check Maven version: `mvn -version` (3.9+ recommended)

### Runtime Issues
- ✅ Check application logs for error messages
- ✅ Verify evidence storage directory exists: `storage/evidence/`
- ✅ Try restarting the application
- ✅ Check database logs for constraint violations

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [Architecture Rationale.md](Architecture%20Rationale.md) | Design decisions & architectural patterns |
| [UseCase-to-System-Mapping.md](UseCase-to-System-Mapping.md) | Use case ↔ class traceability matrix |
| [NFRs.md](NFRs.md) | Non-functional requirements & constraints |
| [RubricMapping.md](RubricMapping.md) | Project rubric alignment & compliance |
| `database/testing.sql` | Database inspection queries |

---

## 📞 Support

**Need help?**
1. Check this README for common solutions
2. Review architecture documentation
3. Inspect application logs
4. Check database state using `database/testing.sql`

**Found a bug?**
1. Document the steps to reproduce
2. Check if migrations ran correctly
3. Verify all prerequisites are installed
4. Include relevant logs when reporting

---

## 📄 License

This project is provided for educational and law enforcement use. Refer to project documentation for specific licensing details.

---

<div align="center">

### 🛡️ Built with ❤️ for Digital Forensics & Criminal Investigation

Made with **JavaFX** | Secured with **SHA-256** | Audited Completely

**Your trusted evidence management system**

</div>

