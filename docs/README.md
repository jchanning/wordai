# Documentation Index

Welcome to the WordAI Documentation. This directory contains comprehensive guides for getting started, using, deploying, and developing WordAI.

## Governance Source of Truth

The following documents are the authoritative governance set for current work:

| Topic | Authoritative document |
|---|---|
| Blueprint / master plan | [ARCHITECTURE.md](./ARCHITECTURE.md) |
| Execution workflow | [EXECUTION_PLAYBOOK.md](./EXECUTION_PLAYBOOK.md) |
| Coding standards | [coding-standards.md](./coding-standards.md) |
| API sketch | [API.md](./API.md) |
| Glossary | [GLOSSARY.md](./GLOSSARY.md) |
| State machines | [STATE_MACHINES.md](./STATE_MACHINES.md) |
| Current implementation status | [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) |
| Active remediation backlog | [../specs/README.md](../specs/README.md) |
| Contributor workflow | [../CONTRIBUTING.md](../CONTRIBUTING.md) |

Agent-specific execution rules remain in [.github/instructions/](../.github/instructions/), but the documents above are the human-readable governance baseline.

## 📚 Documentation Sections

### 🚀 [Getting Started](./getting-started/)
Quick start guides to get WordAI up and running.
- [Project Overview](./getting-started/overview.md) - Main README and feature overview
- [System Requirements](./getting-started/overview.md#-quick-start) - Prerequisites and setup

### 👤 [User Guides](./user-guides/)
End-user documentation for playing and using WordAI.
- [User Guide](./user-guides/USER_GUIDE.md) - How to play, interface guide, features

### 🛠️ [Deployment](./deployment/)
Complete deployment instructions for various environments.
- [Deployment Overview](./deployment/deployment-guide.md) - General deployment concepts
- [Oracle Cloud Setup](./deployment/oracle-cloud.md) - Deploy to OCI instances
- [HTTPS & SSL Setup](./deployment/https-setup.md) - Custom domain with HTTPS (consolidated)
- [DNS Configuration](./deployment/dns-setup.md) - Domain registrar setup
- [GitHub Setup](./deployment/GITHUB_SETUP.md) - Repository configuration
- [Authentication](./deployment/authentication.md) - User authentication setup
- [Azure/ARM Deployment](./deployment/DEPLOY_TO_ARM.md) - Alternative cloud platforms

### 💻 [Development](./development/)
Technical documentation for developers.
- [Architecture Guide](./ARCHITECTURE.md) - Project structure, boundaries, and runtime invariants
- [Execution Playbook](./EXECUTION_PLAYBOOK.md) - Required spec, validation, and status workflow
- [Coding Standards](./coding-standards.md) - Repository implementation standards
- [Contribution Guide](../CONTRIBUTING.md) - Minimum ticket, test, and status update workflow
- [Java 25 Upgrade](./development/java-upgrade-notes.md) - Java and Spring baseline migration notes
- [Performance Optimization](./development/performance-optimization.md) - Performance tuning guide

### ✨ [Features](./features/)
Documentation for specific features and improvements.
- [Game History Feature](./features/GAME_HISTORY_FEATURE.md) - Game history tracking
- [UI Improvements](./features/UI%20Improvements.md) - Interface enhancements
- [Phase 1 Inventory](./features/UI_OVERHAUL_PHASE1_INVENTORY.md) - UI overhaul planning

### 📦 [Releases](./releases/)
Release notes and deployment reports.
- [CHANGELOG](./releases/CHANGELOG.md) - Master changelog (coming soon)
- [v1.5.2 Release](./releases/v1.5.2.md) - Latest stable release
- [v1.5.0 Report](./releases/v1.5.0-report.md) - Deployment report
- [v1.2 Report](./releases/v1.2-report.md) - Historical deployment report

### ❓ [Troubleshooting](./troubleshooting/)
Solutions for common issues.
- [Common Issues](./troubleshooting/common-issues.md) - FAQ and solutions
- [HTTPS Troubleshooting](./deployment/https-setup.md#troubleshooting--faq) - SSL/certificate issues

---

## Quick Links

| Task | Resource |
|------|----------|
| **I want to play WordAI** | → [User Guide](./user-guides/USER_GUIDE.md) |
| **I want to deploy to Oracle Cloud** | → [Oracle Cloud Setup](./deployment/oracle-cloud.md) + [HTTPS Guide](./deployment/https-setup.md) |
| **I want to set up a custom domain** | → [HTTPS Setup](./deployment/https-setup.md) + [DNS Setup](./deployment/dns-setup.md) |
| **I'm a developer** | → [Development Guide](./development/) + [Architecture](./ARCHITECTURE.md) |
| **I need to troubleshoot** | → [Troubleshooting](./troubleshooting/) |
| **I want to see what changed** | → [Releases](./releases/) |

---

## Documentation Status

Last Updated: April 25, 2026  
Status: API version boundary added for ARCH-24

For more details on documentation progress, see [DOCUMENTATION_STATUS.md](./DOCUMENTATION_STATUS.md).

---

## Contributing

Use [../CONTRIBUTING.md](../CONTRIBUTING.md) for the repository contribution workflow.
Documentation changes that affect governance should also update [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) and the active ticket in [../specs/](../specs/).

---

## Feedback

Have questions or suggestions about the documentation? Open an issue on [GitHub](https://github.com/jchanning/wordai).
