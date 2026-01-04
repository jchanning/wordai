#!/bin/bash
# WordAI Update Script
# Run this script to update the application with a new version

set -e

echo "=========================================="
echo "WordAI Update Script"
echo "=========================================="

# Check if JAR exists
if [ ! -f "wordai-1.0-SNAPSHOT.jar" ]; then
    echo "Error: wordai-1.0-SNAPSHOT.jar not found in current directory"
    echo "Please upload it first using:"
    echo "scp -i your-key.pem target/wordai-1.0-SNAPSHOT.jar opc@<PUBLIC_IP>:~/"
    exit 1
fi

# Backup current version
echo "[1/4] Backing up current version..."
if [ -f "~/wordai-app/wordai-1.0-SNAPSHOT.jar" ]; then
    cp ~/wordai-app/wordai-1.0-SNAPSHOT.jar ~/wordai-app/wordai-1.0-SNAPSHOT.jar.backup.$(date +%Y%m%d_%H%M%S)
    echo "Backup created"
fi

# Stop service
echo "[2/4] Stopping application..."
sudo systemctl stop wordai

# Copy new version
echo "[3/4] Installing new version..."
cp wordai-1.0-SNAPSHOT.jar ~/wordai-app/

# Start service
echo "[4/4] Starting application..."
sudo systemctl start wordai

# Wait a bit and check status
sleep 3
sudo systemctl status wordai --no-pager

echo ""
echo "=========================================="
echo "Update complete!"
echo "=========================================="
echo "Check logs: sudo journalctl -u wordai -f"
echo "=========================================="
