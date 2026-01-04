# Quick Deployment to ARM Instance (130.162.184.150)

## Upload Files to New Instance

From PowerShell on your local machine:

```powershell
# Set variables
$IP = "130.162.184.150"
$KEY = "C:\Users\johnm\.ssh\oracle-wordai.key"

# Upload JAR file
scp -i $KEY target\wordai-1.0-SNAPSHOT.jar opc@${IP}:~/wordai-app/

# Upload systemd service file
scp -i $KEY deployment\wordai.service opc@${IP}:~/

# Upload production config
scp -i $KEY deployment\application-prod.properties opc@${IP}:~/wordai-app/

# Upload deployment script
scp -i $KEY deployment\deploy-to-arm.sh opc@${IP}:~/
```

## Deploy Application

SSH into the instance:
```powershell
ssh -i $KEY opc@${IP}
```

Then run the deployment script:
```bash
chmod +x deploy-to-arm.sh
./deploy-to-arm.sh
```

## Access Your Application

Open in browser:
```
http://130.162.184.150:8080
```

## Useful Commands

### View Live Logs
```bash
sudo journalctl -u wordai -f
```

### Check Service Status
```bash
sudo systemctl status wordai
```

### Restart Application
```bash
sudo systemctl restart wordai
```

### Stop Application
```bash
sudo systemctl stop wordai
```

### Check Java Version
```bash
java -version
```

### Check Memory Usage
```bash
free -h
```

### View Application Logs File
```bash
tail -f ~/wordai-app/logs/wordai.log
```

## Troubleshooting

### If service fails to start:
```bash
# Check detailed logs
sudo journalctl -u wordai -xe

# Check if port is already in use
sudo netstat -tulpn | grep 8080

# Verify JAR file exists
ls -lh ~/wordai-app/wordai-1.0-SNAPSHOT.jar

# Test JAR manually
cd ~/wordai-app
java -jar wordai-1.0-SNAPSHOT.jar
```

### If you can't access from browser:
```bash
# Verify firewall
sudo firewall-cmd --list-all

# Check if service is listening
sudo netstat -tulpn | grep 8080

# Check OCI security list in web console
# Make sure port 8080 ingress rule exists
```

## Update Application

When you have new changes:

```powershell
# 1. Rebuild locally
mvn clean package -DskipTests

# 2. Upload new JAR
scp -i $KEY target\wordai-1.0-SNAPSHOT.jar opc@${IP}:~/wordai-app/

# 3. Restart service (SSH into instance)
sudo systemctl restart wordai
```

## Clean Up Old Instance

Once confirmed working, terminate the old instance (141.147.106.72) from OCI console to avoid confusion.
