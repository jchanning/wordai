#!/bin/bash
# WordAI First-Time Setup for Local Linux Server (ZorinOS / Ubuntu)
# Run this ONCE on the server before the first deploy.
#
# From Windows:
#   scp -i $KEY deployment\setup-local.sh jchanning@AI-Server:~/
#   ssh -i $KEY jchanning@AI-Server "chmod +x ~/setup-local.sh && ~/setup-local.sh"

set -e

echo "=== WordAI Local Server Setup ==="
echo ""

# ---- 1. Java ----------------------------------------------------------------
echo "[1/4] Checking Java installation..."
if command -v java &>/dev/null; then
    echo "  Java found: $(java -version 2>&1 | head -1)"
else
    echo "  Java not found — installing OpenJDK 17..."
    sudo apt-get update -q
    sudo apt-get install -y openjdk-17-jre-headless
    echo "  Java installed: $(java -version 2>&1 | head -1)"
fi

# ---- 2. Application directories ---------------------------------------------
echo ""
echo "[2/4] Creating application directories..."
mkdir -p ~/wordai-app/logs
mkdir -p ~/wordai-data/analysis
echo "  Created: ~/wordai-app  ~/wordai-data"

# ---- 3. Environment file ----------------------------------------------------
echo ""
echo "[3/4] Checking /etc/default/wordai environment file..."
if [ -f /etc/default/wordai ]; then
    echo "  /etc/default/wordai already exists — skipping."
else
    echo "  Creating /etc/default/wordai with placeholder values..."
    sudo tee /etc/default/wordai > /dev/null <<'EOF'
# WordAI runtime credentials — update these values before starting the service.
# File permissions should be: sudo chmod 600 /etc/default/wordai
WORDAI_ADMIN_EMAIL=admin@example.com
WORDAI_ADMIN_PASSWORD=changeme123
WORDAI_ADMIN_USERNAME=admin
WORDAI_ADMIN_FULLNAME=System Administrator
WORDAI_CORS_ALLOWED_ORIGINS=http://localhost:8080,http://AI-Server:8080
EOF
    sudo chmod 600 /etc/default/wordai
    echo ""
    echo "  *** ACTION REQUIRED: Edit /etc/default/wordai with your real credentials ***"
    echo "      sudo nano /etc/default/wordai"
fi

# ---- 4. Verify --------------------------------------------------------------
echo ""
echo "[4/4] Verification:"
java -version 2>&1 | head -1 | sed 's/^/  Java: /'
echo "  App dir:  $(ls -d ~/wordai-app)"
echo "  Data dir: $(ls -d ~/wordai-data)"
ls /etc/default/wordai &>/dev/null && echo "  Env file: /etc/default/wordai (exists)" || echo "  Env file: MISSING"

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "  1. Update credentials: sudo nano /etc/default/wordai"
echo "  2. Run from Windows:   .\\deployment\\deploy-local.ps1"
echo ""
