# WordAI Oracle Cloud Deployment Guide

## Prerequisites
- OCI account created ✓
- SSH key pair generated

## Step-by-Step Deployment Instructions

### Step 1: Create Compute Instance (10 minutes)

1. **Log in to Oracle Cloud Console**: https://cloud.oracle.com
2. **Navigate to Compute → Instances**
3. **Click "Create Instance"**
   - **Name**: `wordai-server`
   - **Compartment**: Keep default (root)
   - **Placement**: Keep default
   - **Image**: Oracle Linux 8 (default)
   - **Shape**: Click "Change Shape"
     - Select "Virtual Machine"
     - Select "Specialty and previous generation"
     - Choose **VM.Standard.E2.1.Micro** (Always Free)
   - **Networking**: 
     - Create new VCN (keep default name)
     - Assign public IPv4 address: **Yes**
   - **SSH Keys**:
     - Upload your public key OR generate a new pair
     - Download private key if generating new
   - Click **Create**

4. **Wait for instance to be "Running"** (2-3 minutes)
5. **Note the Public IP address** - you'll need this!

### Step 2: Configure Network Security (5 minutes)

1. **From your instance page**, click on the VCN name under "Primary VNIC"
2. **Click "Security Lists"** in the left menu
3. **Click on "Default Security List"**
4. **Click "Add Ingress Rules"**
   - **Source CIDR**: `0.0.0.0/0`
   - **IP Protocol**: TCP
   - **Destination Port Range**: `8080`
   - Click **Add Ingress Rules**

### Step 3: Build and Package Application (2 minutes)

On your local Windows machine:

```powershell
cd C:\Users\johnm\OneDrive\Projects\WordAI
mvn clean package -DskipTests
```

This creates: `target\wordai-1.0-SNAPSHOT.jar`

### Step 4: Upload Files to OCI (5 minutes)

Replace `<PUBLIC_IP>` with your instance's public IP:

```powershell
# Upload JAR file
scp -i path\to\your-private-key target\wordai-1.0-SNAPSHOT.jar opc@<PUBLIC_IP>:~/

# Upload deployment files
scp -i path\to\your-private-key deployment\wordai.service opc@<PUBLIC_IP>:~/
scp -i path\to\your-private-key deployment\deploy.sh opc@<PUBLIC_IP>:~/
```

### Step 5: Connect to Instance and Deploy (15 minutes)

```powershell
ssh -i path\to\your-private-key opc@<PUBLIC_IP>
```

Once connected, run:

```bash
# Make deploy script executable
chmod +x deploy.sh

# Run deployment script
./deploy.sh

# Move JAR to app directory
mv wordai-1.0-SNAPSHOT.jar ~/wordai-app/

# Install and start service
sudo cp wordai.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable wordai
sudo systemctl start wordai

# Check status
sudo systemctl status wordai
```

### Step 6: Verify Deployment (2 minutes)

1. **Check logs**:
   ```bash
   sudo journalctl -u wordai -f
   ```
   Press Ctrl+C to exit

2. **Access your application**:
   Open browser: `http://<PUBLIC_IP>:8080`

3. **Test features**:
   - Play a game
   - Try Auto mode
   - Sign in/Register
   - Check Help page

## Useful Commands

### Check application status
```bash
sudo systemctl status wordai
```

### View logs
```bash
sudo journalctl -u wordai -f
```

### Restart application
```bash
sudo systemctl restart wordai
```

### Stop application
```bash
sudo systemctl stop wordai
```

### Check disk space
```bash
df -h
```

### Check memory usage
```bash
free -h
```

## Updating the Application

When you make changes and want to deploy a new version:

1. **Build locally**:
   ```powershell
   mvn clean package -DskipTests
   ```

2. **Upload new JAR**:
   ```powershell
   scp -i path\to\your-private-key target\wordai-1.0-SNAPSHOT.jar opc@<PUBLIC_IP>:~/
   ```

3. **On the server, run update script**:
   ```bash
   ./update.sh
   ```

## Troubleshooting

### Application won't start
```bash
# Check logs for errors
sudo journalctl -u wordai -n 50

# Check if Java is installed
java -version

# Check if port 8080 is in use
sudo netstat -tulpn | grep 8080
```

### Can't access from browser
```bash
# Check firewall
sudo firewall-cmd --list-all

# Verify service is running
sudo systemctl status wordai
```

### Database issues
```bash
# Check if data directory exists
ls -la ~/wordai-data/

# Check disk space
df -h
```

## Next Steps (Optional)

1. **Add custom domain**: Point your domain's A record to the public IP
2. **Add SSL**: Install nginx and Let's Encrypt (instructions available)
3. **Update OAuth**: Add production redirect URIs in Google/Apple developer consoles

## Cost Monitoring

Check your Always Free resources:
- Oracle Cloud Console → Governance → Limits, Quotas and Usage
- Ensure you're using Always Free shapes to avoid charges
