# Cloud Deployment Guide

This guide covers deploying WordAI to Oracle Cloud Infrastructure with proper build configurations.

## Prerequisites

1. **Oracle Cloud Instance**: ARM Ampere A1 (or x86) with at least 2GB RAM
2. **SSH Key**: Access to your OCI instance
3. **Java 21**: Installed locally for development
4. **Maven**: For building the application

## Quick Start

### 1. Configure Deployment Settings

Copy the example configuration:
```powershell
Copy-Item deployment\deployment-config.ps1.example deployment\deployment-config.ps1
```

Edit `deployment\deployment-config.ps1` and set your values:
```powershell
$IP = "130.162.184.150"  # Your OCI instance IP
$KEY = "C:\Users\johnm\.ssh\arm-wordai.key"  # Path to SSH key
```

**⚠️ IMPORTANT**: `deployment-config.ps1` is in `.gitignore` and will NOT be committed to GitHub.

### 2. Deploy to Cloud

Run the automated deployment script:
```powershell
.\deployment\deploy-cloud.ps1
```

This will:
1. Build the application for Java 17 (cloud profile)
2. Upload JAR to the server
3. Upload configuration files
4. Extract dictionary files from JAR
5. Restart the application service

### 3. Verify Deployment

Open in browser:
```
http://YOUR_SERVER_IP:8080
```

Check logs:
```powershell
ssh -i YOUR_SSH_KEY opc@YOUR_IP "sudo journalctl -u wordai -f"
```

---

## Build Profiles

WordAI uses Maven profiles for different environments:

### Local Profile (Default - Java 21)

For local development and testing:
```powershell
mvn clean package -DskipTests
# or explicitly:
mvn clean package -DskipTests -Plocal
```

**Features:**
- Compiles with Java 21
- Uses local dictionary paths
- Full Java 21 language features available

### Cloud Profile (Java 17)

For Oracle Cloud deployment:
```powershell
mvn clean package -DskipTests -Pcloud
# or use the script:
.\deployment\build-cloud.ps1
```

**Features:**
- Compiles with Java 17 target
- Compatible with OCI free tier Java installation
- Smaller runtime footprint

---

## Manual Deployment

If you prefer manual steps:

### 1. Build for Cloud
```powershell
mvn clean package -DskipTests -Pcloud
```

### 2. Upload Files
```powershell
$IP = "YOUR_IP"
$KEY = "PATH_TO_KEY"

scp -i $KEY target\wordai-1.0-SNAPSHOT.jar opc@${IP}:~/wordai-app/
scp -i $KEY deployment\wordai.properties opc@${IP}:~/wordai-app/
scp -i $KEY deployment\setup-dictionaries.sh opc@${IP}:~/wordai-app/
```

### 3. Setup on Server
```bash
ssh -i $KEY opc@$IP

cd ~/wordai-app
chmod +x setup-dictionaries.sh
./setup-dictionaries.sh
sudo systemctl restart wordai
sudo systemctl status wordai
```

---

## Configuration Files

### Local Development

**Used:** `src/main/resources/application.properties`
- Points to local OneDrive dictionary paths
- H2 database at `./data/wordai`
- H2 console enabled

### Cloud Deployment

**Used:** `~/wordai-app/wordai.properties` (on server)
- Points to extracted dictionary files in `dictionaries/`
- H2 database at `/home/opc/wordai-data/wordai`
- Production logging configuration

The `ConfigManager` class checks for `wordai.properties` first, then falls back to `application.properties`.

---

## Dictionary Management

Dictionaries are bundled in the JAR but must be extracted on the server because `ConfigManager` expects filesystem paths.

The `setup-dictionaries.sh` script:
1. Creates `~/wordai-app/dictionaries/` directory
2. Extracts dictionary files from JAR using `jar xf`
3. Moves them to the dictionaries folder
4. Cleans up temporary files

**Dictionary files:**
- `4_letter_words.txt`
- `5_letter_words.txt`
- `6_letter_words.txt`
- `7_letter_words.txt`

---

## Security Best Practices

### Files NOT Committed to Git

Add these to `.gitignore`:
```
deployment/deployment-config.ps1
*.key
*.pem
```

### Sensitive Configuration

Never commit:
- SSH keys
- Server IP addresses (use config file)
- OAuth client secrets
- Database passwords

### Use Environment Variables

For production secrets in `application-prod.properties`:
```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
```

Set on server:
```bash
export GOOGLE_CLIENT_ID=your_id
export GOOGLE_CLIENT_SECRET=your_secret
```

---

## Updating the Application

### Quick Update
```powershell
.\deployment\deploy-cloud.ps1
```

### Update Only JAR
```powershell
mvn clean package -DskipTests -Pcloud
scp -i $KEY target\wordai-1.0-SNAPSHOT.jar opc@${IP}:~/wordai-app/
ssh -i $KEY opc@$IP "sudo systemctl restart wordai"
```

### Update Configuration Only
```powershell
scp -i $KEY deployment\wordai.properties opc@${IP}:~/wordai-app/
ssh -i $KEY opc@$IP "sudo systemctl restart wordai"
```

---

## Troubleshooting

### Build Issues

**Java version mismatch:**
```powershell
java -version  # Should show Java 21
mvn -version   # Should show Java 21
```

**Profile not active:**
```powershell
mvn clean package -DskipTests -Pcloud -X  # Debug output
# Look for: "maven.compiler.target = 17"
```

### Deployment Issues

**SSH connection failed:**
- Check SSH key path in `deployment-config.ps1`
- Verify key permissions (read-only for owner)
- Test: `ssh -i $KEY opc@$IP "echo connected"`

**Dictionary extraction failed:**
- Ensure `jar` command available on server
- Check disk space: `df -h`
- Manually extract: `cd ~/wordai-app && ./setup-dictionaries.sh`

**Application won't start:**
```bash
# Check logs
sudo journalctl -u wordai -n 100 --no-pager

# Test JAR manually
cd ~/wordai-app
java -jar wordai-1.0-SNAPSHOT.jar

# Verify dictionaries exist
ls -la ~/wordai-app/dictionaries/
```

### Runtime Issues

**Dictionary not found errors:**
- Run `setup-dictionaries.sh` again
- Check `wordai.properties` paths match actual file locations

**Java version errors:**
- Rebuild with cloud profile: `mvn clean package -Pcloud`
- Verify: `javap -v target/classes/com/fistraltech/WordAIApplication.class | grep "major version"`
  - Should show: `major version: 61` (Java 17)

---

## CI/CD Integration

For automated deployment with GitHub Actions, create `.github/workflows/deploy-cloud.yml`:

```yaml
name: Deploy to Oracle Cloud

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          
      - name: Build with cloud profile
        run: mvn clean package -DskipTests -Pcloud
        
      - name: Deploy to OCI
        env:
          SSH_KEY: ${{ secrets.OCI_SSH_KEY }}
          SERVER_IP: ${{ secrets.OCI_SERVER_IP }}
        run: |
          # Add deployment steps here
```

**Required GitHub Secrets:**
- `OCI_SSH_KEY`: Private SSH key content
- `OCI_SERVER_IP`: Server IP address

---

## Cost Optimization

Oracle Cloud Free Tier limits:
- **ARM Ampere A1**: Up to 4 OCPUs, 24GB RAM (FREE)
- **x86 Micro**: 1 OCPU, 1GB RAM (FREE)

**Recommended setup:**
- ARM instance: 2 OCPUs, 12GB RAM
- Java 17 (lighter than 21)
- Disable H2 console in production
- Use logrotate for log management

---

## Next Steps

- [ ] Set up custom domain with DNS
- [ ] Configure SSL/TLS with Let's Encrypt
- [ ] Set up automated backups
- [ ] Configure monitoring/alerting
- [ ] Implement CI/CD pipeline
- [ ] Add health check endpoint

---

For more details, see:
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Initial setup
- [OCI_JAVA_INSTALL_GUIDE.md](OCI_JAVA_INSTALL_GUIDE.md) - Java installation troubleshooting
