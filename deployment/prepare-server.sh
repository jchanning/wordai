#!/bin/bash
# Server-side preparation script for WordAI deployment
# Run this script on the OCI instance to prepare for deployment

echo "=== Preparing Instance for WordAI Deployment ==="
echo ""

# Display system information
echo "System Information:"
cat /etc/os-release | grep PRETTY_NAME
echo ""

# Check Java (will install later if not present)
echo "Checking Java installation:"
java -version 2>&1 || echo "Java not installed (will be installed during deployment)"
echo ""

# Check available memory
echo "Memory Status:"
free -h
echo ""

# Check disk space
echo "Disk Space:"
df -h /
echo ""

# Create directory structure
echo "Creating application directories..."
mkdir -p ~/wordai-app
mkdir -p ~/wordai-app/logs
mkdir -p ~/wordai-data

echo "✓ Created ~/wordai-app"
echo "✓ Created ~/wordai-app/logs"
echo "✓ Created ~/wordai-data"
echo ""

# Set proper permissions
chmod 755 ~/wordai-app
chmod 755 ~/wordai-data

# Verify directory structure
echo "Directory structure:"
ls -la ~ | grep wordai
echo ""

echo "=== Preparation Complete ==="
echo ""
echo "Instance is ready for file uploads:"
echo "  - JAR file → ~/wordai-app/"
echo "  - Service file → ~/"
echo "  - Config file → ~/wordai-app/"
echo "  - Deployment script → ~/"
echo ""
