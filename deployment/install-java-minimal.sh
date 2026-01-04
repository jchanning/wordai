#!/bin/bash
# Minimal Java installation for OCI Free Tier (1GB RAM)
# This script tries multiple approaches to install Java

set -e

echo "=== Attempting Java Installation on OCI Free Tier ==="
echo "System has limited RAM (1GB) - using memory-safe approach"
echo ""

# Create swap space to help with installation
echo "Step 1: Creating swap space (2GB) to assist installation..."
if [ ! -f /swapfile ]; then
    sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 status=progress
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "Swap created successfully"
else
    echo "Swap file already exists"
fi

echo ""
free -h
echo ""

# Method 1: Try Java 17 (lighter than 21, more likely available)
echo "Step 2: Attempting to install Java 17 (headless, lighter version)..."
if sudo yum install -y java-17-openjdk-headless; then
    echo "✓ Java 17 installed successfully"
    java -version
    exit 0
fi

echo "Java 17 failed, trying Java 11..."
if sudo yum install -y java-11-openjdk-headless; then
    echo "✓ Java 11 installed successfully"
    java -version
    exit 0
fi

# Method 2: Try Oracle JDK from archive
echo "Step 3: Attempting manual Oracle JDK download..."
cd /tmp
wget -q --show-progress https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz || {
    echo "Download failed, trying alternative mirror..."
    wget -q --show-progress https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
}

if [ -f jdk-17_linux-x64_bin.tar.gz ] || [ -f openjdk-17.0.2_linux-x64_bin.tar.gz ]; then
    sudo mkdir -p /usr/lib/jvm
    sudo tar -xzf *jdk-17*_linux-x64_bin.tar.gz -C /usr/lib/jvm/
    
    # Find the extracted directory
    JDK_DIR=$(ls -d /usr/lib/jvm/jdk-17* | head -1)
    
    # Set alternatives
    sudo update-alternatives --install /usr/bin/java java ${JDK_DIR}/bin/java 1
    sudo update-alternatives --set java ${JDK_DIR}/bin/java
    
    echo "✓ Oracle JDK 17 installed manually"
    java -version
    exit 0
fi

# Method 3: Try Amazon Corretto (optimized, smaller footprint)
echo "Step 4: Trying Amazon Corretto 17..."
sudo rpm --import https://yum.corretto.aws/corretto.key
sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo
if sudo yum install -y java-17-amazon-corretto-headless; then
    echo "✓ Amazon Corretto 17 installed successfully"
    java -version
    exit 0
fi

echo "❌ All installation methods failed"
echo "Please check system logs: sudo journalctl -xe"
exit 1
