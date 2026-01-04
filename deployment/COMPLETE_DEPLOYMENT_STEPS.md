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
ssh -i C:\Users\johnm\.ssh\oracle-wordai.key opc@130.162.184.150
```

2. **Create directories:**
```bash
mkdir -p ~/wordai-app ~/wordai-app/logs ~/wordai-data
exit
```

### Option 3: Upload and Run Prep Script

```powershell
# Upload prep script
scp -i C:\Users\johnm\.ssh\oracle-wordai.key deployment\prepare-server.sh opc@130.162.184.150:~/

# SSH and run it
ssh -i C:\Users\johnm\.ssh\oracle-wordai.key opc@130.162.184.150
chmod +x prepare-server.sh
./prepare-server.sh
exit
```

---

## Full Deployment Workflow

Run these commands in order:

### Step 1: Prepare Instance
```powershell
.\deployment\prepare-instance.ps1
```

### Step 2: Upload Files
```powershell
$IP = "130.162.184.150"
$KEY = "C:\Users\johnm\.ssh\oracle-wordai.key"

# Upload JAR
scp -i $KEY target\wordai-1.0-SNAPSHOT.jar opc@${IP}:~/wordai-app/

# Upload systemd service
scp -i $KEY deployment\wordai.service opc@${IP}:~/

# Upload production config
scp -i $KEY deployment\application-prod.properties opc@${IP}:~/wordai-app/

# Upload deployment script
scp -i $KEY deployment\deploy-to-arm.sh opc@${IP}:~/
```

### Step 3: Deploy Application
```powershell
ssh -i $KEY opc@$IP
```

Then on the server:
```bash
chmod +x deploy-to-arm.sh
./deploy-to-arm.sh
```

### Step 4: Verify Deployment
```bash
# Check service status
sudo systemctl status wordai

# View logs
sudo journalctl -u wordai -f
```

### Step 5: Test in Browser
```
http://130.162.184.150:8080
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

**Prepare instance:**
```powershell
.\deployment\prepare-instance.ps1
```

**Upload all files at once:**
```powershell
$IP = "130.162.184.150"; $KEY = "C:\Users\johnm\.ssh\oracle-wordai.key"
scp -i $KEY target\wordai-1.0-SNAPSHOT.jar opc@${IP}:~/wordai-app/
scp -i $KEY deployment\wordai.service opc@${IP}:~/
scp -i $KEY deployment\application-prod.properties opc@${IP}:~/wordai-app/
scp -i $KEY deployment\deploy-to-arm.sh opc@${IP}:~/
```

**Deploy:**
```powershell
ssh -i $KEY opc@$IP "chmod +x deploy-to-arm.sh && ./deploy-to-arm.sh"
```

**Check status:**
```powershell
ssh -i $KEY opc@$IP "sudo systemctl status wordai"
```
