# Complete Deployment Guide for ARM Instance

## Pre-Deployment Preparation

### Option 1: Automated Preparation (PowerShell)

From your local machine, run:
```powershell
.\deployment\prepare-instance.ps1
```

### Option 2: Manual Preparation (SSH)

1. **SSH into the instance:**
```powershell
ssh -i C:\Users\johnm\.ssh\oracle-wordai.key opc@132.145.64.140
```

2. **Create directories:**
```bash
mkdir -p ~/wordai-app ~/wordai-app/logs ~/wordai-data
exit
```

### Option 3: Upload and Run Prep Script

```powershell
# Upload prep script
scp -i C:\Users\johnm\.ssh\oracle-wordai.key deployment\prepare-server.sh opc@132.145.64.140:~/

# SSH and run it
ssh -i C:\Users\johnm\.ssh\oracle-wordai.key opc@132.145.64.140
chmod +x prepare-server.sh
./prepare-server.sh
exit
```

---

## Full Deployment Workflow

Run this command:

```powershell
.\deployment\deploy.ps1
```

This single script builds the app, uploads the latest JAR and config, extracts dictionaries, and restarts the `wordai` service.

### Verify Deployment
```bash
# Check service status
sudo systemctl status wordai

# View logs
sudo journalctl -u wordai -f
```

### Step 5: Test in Browser
```
http://132.145.64.140:8080
```

---

## Troubleshooting

### If directories don't exist:
```bash
mkdir -p ~/wordai-app ~/wordai-app/logs ~/wordai-data
```

### If upload fails with "No such file or directory":
Run the preparation script first (Step 1)

### If you get permission denied:
```bash
chmod 755 ~/wordai-app
chmod 755 ~/wordai-data
```

---

## Quick Commands Reference

**Deploy end-to-end:**
```powershell
.\deployment\deploy.ps1
```

**Check status:**
```powershell
ssh -i $KEY opc@$IP "sudo systemctl status wordai"
```
