# Java Installation Guide for OCI Free Tier

## Problem
Oracle Cloud Infrastructure free tier VM (1GB RAM) often kills processes during package installation due to memory constraints.

## Solutions

### Quick Diagnosis

SSH into your OCI instance and run:
```bash
chmod +x debug-java-install.sh
./debug-java-install.sh
```

This will show:
- Available Java versions in yum repositories
- Current memory status
- Whether Java is already installed
- Swap space configuration

---

## Solution 1: Install Java 17 (Recommended for OCI Free Tier)

**Why Java 17?**
- Spring Boot 3.4.0 supports Java 17+
- Lighter footprint than Java 21
- More widely available in Oracle Linux 8 repos
- Better tested on low-memory systems

### Manual Steps:

```bash
# 1. Create swap space first (critical for 1GB RAM)
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 2. Verify swap is active
free -h

# 3. Install Java 17 headless (no GUI components)
sudo yum install -y java-17-openjdk-headless

# 4. Verify installation
java -version
```

### Automated Installation:

```bash
chmod +x install-java-minimal.sh
./install-java-minimal.sh
```

This script tries multiple approaches:
1. Java 17 headless
2. Java 11 headless (fallback)
3. Manual Oracle JDK download
4. Amazon Corretto (optimized)

---

## Solution 2: Check What's Already Installed

Your OCI instance might already have Java installed:

```bash
# Check for any Java installation
java -version

# Check installed Java packages
rpm -qa | grep -i openjdk

# Find Java installation path
which java
readlink -f $(which java)
```

If Java is already installed, update `deploy.sh` to skip the installation step.

---

## Solution 3: Enable Required Repositories

Oracle Linux 8 might need additional repositories enabled:

```bash
# Enable Oracle Linux 8 AppStream
sudo dnf config-manager --enable ol8_appstream

# Enable CodeReady Builder
sudo dnf config-manager --enable ol8_codeready_builder

# Update repository metadata
sudo yum clean all
sudo yum makecache

# Try installation again
sudo yum install -y java-17-openjdk-headless
```

---

## Solution 4: Amazon Corretto (Optimized Distribution)

Amazon Corretto is optimized for cloud environments:

```bash
# Add Corretto repository
sudo rpm --import https://yum.corretto.aws/corretto.key
sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo

# Install Corretto 17
sudo yum install -y java-17-amazon-corretto-headless
```

---

## Solution 5: Manual Download (When yum Fails)

If package managers fail completely:

```bash
# Download OpenJDK 17
cd /tmp
wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz

# Extract to /usr/lib/jvm
sudo mkdir -p /usr/lib/jvm
sudo tar -xzf openjdk-17.0.2_linux-x64_bin.tar.gz -C /usr/lib/jvm/

# Set up alternatives
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-17.0.2/bin/java 1
sudo update-alternatives --set java /usr/lib/jvm/jdk-17.0.2/bin/java

# Verify
java -version
```

---

## Debugging Steps

### 1. Monitor Memory During Installation

In one terminal:
```bash
watch -n 1 'free -h'
```

In another terminal:
```bash
sudo yum install -y java-17-openjdk-headless
```

If you see the process killed, it's memory related.

### 2. Check System Logs

```bash
# View recent system messages
sudo journalctl -xe

# Check for OOM (Out of Memory) kills
sudo dmesg | grep -i kill

# View yum logs
sudo tail -f /var/log/yum.log
```

### 3. Verify Repository Access

```bash
# List enabled repositories
yum repolist enabled

# Search for Java packages
yum search openjdk

# Get package details
yum info java-17-openjdk-headless
```

---

## Update Your Application for Java 17

If you install Java 17 instead of 21, update your `pom.xml`:

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

Then rebuild locally:
```bash
mvn clean package -DskipTests
```

And redeploy the new JAR to OCI.

---

## Common Errors and Solutions

### Error: "Transaction check error"
**Solution:** Clean yum cache
```bash
sudo yum clean all
sudo yum makecache
```

### Error: "Killed" during installation
**Solution:** Add swap space (see Solution 1)

### Error: "No package java-21-openjdk available"
**Solution:** Use Java 17 instead (widely available)

### Error: "Cannot allocate memory"
**Solution:** Increase swap space to 4GB
```bash
sudo swapoff /swapfile
sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
sudo mkswap /swapfile
sudo swapon /swapfile
```

---

## Verification Checklist

After successful installation:

- [ ] Java version matches expected (17 or 21)
- [ ] `java -version` works
- [ ] `echo $JAVA_HOME` shows path (if set)
- [ ] Swap space active (`swapon --show`)
- [ ] Enough disk space (`df -h`)
- [ ] Firewall allows port 8080 (`sudo firewall-cmd --list-all`)

---

## Next Steps

Once Java is installed:

1. **Test the JAR locally:**
   ```bash
   cd /home/opc/wordai-app
   java -jar wordai-1.0-SNAPSHOT.jar
   ```

2. **Set up systemd service:**
   ```bash
   sudo cp wordai.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable wordai
   sudo systemctl start wordai
   ```

3. **Check application logs:**
   ```bash
   sudo journalctl -u wordai -f
   ```

4. **Access the app:**
   ```
   http://141.147.106.72:8080
   ```

---

## Pro Tip: Increase Instance RAM

If you continue having issues, consider:
- Upgrading to a paid instance with more RAM
- Using OCI's "Always Free" ARM instance (4GB RAM available)
- Setting up a larger swap partition (4-8GB)

The ARM-based Ampere A1 instances in OCI's free tier have up to 24GB RAM available across 4 OCPUs - much better for Java applications!
