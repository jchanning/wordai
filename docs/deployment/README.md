# Deployment Guide

This section contains all deployment-related documentation for WordAI.

## Overview

WordAI can be deployed to multiple platforms:
- **Oracle Cloud** - Primary production target
- **Azure/ARM** - Alternative cloud platform
- **Local/Docker** - Development and testing

## Available Guides

### 🌍 Oracle Cloud (Primary)

- **[Oracle Cloud Setup](./oracle-cloud.md)** - Install Java, configure environment
- **[Deploy to ARM](./DEPLOY_TO_ARM.md)** - Alternative ARM-based cloud deployment
- **[HTTPS & SSL Setup](./https-setup.md)** - Enable HTTPS with custom domain
- **[DNS Configuration](./dns-setup.md)** - Point domain to server

### 🔐 Security & Authentication

- **[Authentication Setup](./authentication.md)** - User login and security
- **[HTTPS Complete Guide](./https-setup.md)** - SSL/TLS certificates, auto-renewal

### 🔧 Integration & Configuration

- **[GitHub Setup](./GITHUB_SETUP.md)** - Repository and CI/CD configuration
- **[General Deployment Guide](./deployment-guide.md)** - Common deployment concepts

---

## Quick Deployment Paths

### Path 1: Oracle Cloud with HTTPS

1. Start: [Oracle Cloud Setup](./oracle-cloud.md)
2. Then: [HTTPS & SSL Setup](./https-setup.md)
3. Finally: [DNS Configuration](./dns-setup.md)

### Path 2: Just Testing (Local)

1. See: [Getting Started](../getting-started/README.md)

### Path 3: Production with All Features

1. [Oracle Cloud Setup](./oracle-cloud.md)
2. [Authentication](./authentication.md)
3. [HTTPS & SSL](./https-setup.md)
4. [DNS Setup](./dns-setup.md)

---

## Pre-Deployment Checklist

- [ ] Application builds successfully: `mvn clean install`
- [ ] All tests pass: `mvn clean test`
- [ ] Version bumped in `pom.xml`
- [ ] Release notes written
- [ ] Git tag created: `git tag v1.x.x`

## Post-Deployment

- [ ] Verify application is running: `curl http://localhost:8080`
- [ ] Check logs for errors
- [ ] Test all key features
- [ ] Update status/monitoring dashboards

---

## Automated Deployment Scripts

The `deployment/` folder contains PowerShell scripts for automated deployment:

- **`build-cloud.ps1`** - Build JAR for cloud deployment
- **`deploy-cloud.ps1`** - Deploy to Oracle Cloud

Usage:
```powershell
.\deployment\build-cloud.ps1
.\deployment\deploy-cloud.ps1
```

---

## Troubleshooting

For common deployment issues, see [Troubleshooting Guide](../troubleshooting/).

## Need Help?

- Check the specific deployment guide for your platform
- Review logs: `journalctl -u wordai -f`
- See [HTTPS Troubleshooting](./https-setup.md#troubleshooting--faq)
