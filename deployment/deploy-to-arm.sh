#!/bin/bash
# WordAI Deployment Script for OCI ARM Instance
# Oracle Linux 9 on Ampere A1

set -e

echo "=== WordAI Deployment for ARM Instance ==="
echo "Starting deployment process..."
echo ""

# Update system
echo "Step 1: Updating system packages..."
sudo dnf update -y

# Install Java 17 (works great on ARM)
echo "Step 2: Installing Java 17..."
sudo dnf install -y java-17-openjdk java-17-openjdk-devel

# Verify Java installation
echo "Step 3: Verifying Java installation..."
java -version
echo ""

# Configure firewall
echo "Step 4: Configuring firewall..."
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
echo "Port 8080 opened"
echo ""

# Create application directories
echo "Step 5: Creating application directories..."
mkdir -p ~/wordai-app
mkdir -p ~/wordai-app/logs
mkdir -p ~/wordai-data
echo "Directories created"
echo ""

# Set up systemd service
echo "Step 6: Setting up systemd service..."
sudo cp ~/wordai.service /etc/systemd/system/
sudo systemctl daemon-reload
echo "Service configured"
echo ""

# Set proper permissions
echo "Step 7: Setting permissions..."
chmod +x ~/wordai-app/wordai.jar
echo "Permissions set"
echo ""

# Enable and start service
echo "Step 8: Starting WordAI service..."
sudo systemctl enable wordai
sudo systemctl start wordai

# Wait a moment for startup
sleep 5

# Check status
echo ""
echo "Step 9: Checking service status..."
sudo systemctl status wordai --no-pager

echo ""
echo "=== Deployment Complete! ==="
echo ""
echo "Application URL: http://130.162.184.150:8080"
echo ""
echo "Useful commands:"
echo "  View logs:        sudo journalctl -u wordai -f"
echo "  Check status:     sudo systemctl status wordai"
echo "  Restart service:  sudo systemctl restart wordai"
echo "  Stop service:     sudo systemctl stop wordai"
echo ""
