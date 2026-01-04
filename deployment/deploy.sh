#!/bin/bash
# WordAI Initial Deployment Script for Oracle Cloud
# Run this script on the OCI compute instance

set -e

echo "=========================================="
echo "WordAI Deployment Script"
echo "=========================================="

# Update system
echo "[1/8] Updating system packages..."
sudo yum update -y

# Install Java 21
echo "[2/8] Installing Java 21..."
sudo yum install -y java-21-openjdk java-21-openjdk-devel
java -version

# Configure firewall
echo "[3/8] Configuring firewall..."
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
echo "Firewall configured: Port 8080 opened"

# Create directories
echo "[4/8] Creating application directories..."
mkdir -p ~/wordai-app
mkdir -p ~/wordai-app/logs
mkdir -p ~/wordai-data
mkdir -p ~/backups

# Install systemd service
echo "[5/8] Installing systemd service..."
if [ -f "wordai.service" ]; then
    sudo cp wordai.service /etc/systemd/system/
    sudo systemctl daemon-reload
    echo "Service file installed"
else
    echo "Warning: wordai.service not found. You'll need to create it manually."
fi

# Check if JAR exists
echo "[6/8] Checking for application JAR..."
if [ -f "wordai-1.0-SNAPSHOT.jar" ]; then
    echo "JAR file found"
else
    echo "Warning: wordai-1.0-SNAPSHOT.jar not found in current directory"
    echo "Please upload it using: scp -i your-key.pem target/wordai-1.0-SNAPSHOT.jar opc@<PUBLIC_IP>:~/"
fi

# Set up daily backup cron job
echo "[7/8] Setting up automatic backups..."
(crontab -l 2>/dev/null; echo "0 2 * * * cp -r /home/opc/wordai-data /home/opc/backups/wordai-\$(date +\%Y\%m\%d)") | crontab -
echo "Daily backup scheduled at 2 AM"

echo "[8/8] Deployment preparation complete!"
echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Upload JAR file if not already done:"
echo "   scp -i your-key.pem target/wordai-1.0-SNAPSHOT.jar opc@<PUBLIC_IP>:~/wordai-app/"
echo ""
echo "2. Upload service file:"
echo "   scp -i your-key.pem deployment/wordai.service opc@<PUBLIC_IP>:~/"
echo ""
echo "3. Start the application:"
echo "   sudo systemctl enable wordai"
echo "   sudo systemctl start wordai"
echo ""
echo "4. Check status:"
echo "   sudo systemctl status wordai"
echo ""
echo "5. View logs:"
echo "   sudo journalctl -u wordai -f"
echo ""
echo "6. Access your app at:"
echo "   http://<YOUR_PUBLIC_IP>:8080"
echo "=========================================="
