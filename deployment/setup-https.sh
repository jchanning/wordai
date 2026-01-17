#!/bin/bash

# WordAI HTTPS Setup Script for Oracle Cloud
# This script automates the installation and configuration of HTTPS
# 
# Usage: ./setup-https.sh yourdomain.com
# Example: ./setup-https.sh wordai.example.com

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if domain provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Domain name required${NC}"
    echo "Usage: $0 yourdomain.com"
    exit 1
fi

DOMAIN=$1
EMAIL=${2:-"admin@$DOMAIN"}

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}WordAI HTTPS Setup for Oracle Cloud${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""
echo -e "Domain: ${GREEN}$DOMAIN${NC}"
echo -e "Email: ${GREEN}$EMAIL${NC}"
echo ""

# Step 1: Verify domain DNS
echo -e "${YELLOW}Step 1: Verifying DNS configuration...${NC}"
if dig +short $DOMAIN | grep -q .; then
    echo -e "${GREEN}✓ DNS resolves to: $(dig +short $DOMAIN | head -1)${NC}"
else
    echo -e "${YELLOW}⚠ Warning: DNS not yet resolved. This may be normal if you just updated your records.${NC}"
    echo "Please wait 5-30 minutes for DNS propagation and try again."
    exit 1
fi
echo ""

# Step 2: Configure firewall
echo -e "${YELLOW}Step 2: Configuring firewall...${NC}"
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent
sudo firewall-cmd --zone=public --add-port=443/tcp --permanent
sudo firewall-cmd --reload
echo -e "${GREEN}✓ Firewall rules added${NC}"
echo ""

# Step 3: Install dependencies
echo -e "${YELLOW}Step 3: Installing dependencies...${NC}"
sudo yum update -y > /dev/null
sudo yum install -y nginx certbot python3-certbot-nginx > /dev/null
echo -e "${GREEN}✓ Nginx and Certbot installed${NC}"
echo ""

# Step 4: Start Nginx
echo -e "${YELLOW}Step 4: Starting Nginx...${NC}"
sudo systemctl start nginx
sudo systemctl enable nginx
echo -e "${GREEN}✓ Nginx started and enabled${NC}"
echo ""

# Step 5: Obtain SSL certificate
echo -e "${YELLOW}Step 5: Obtaining SSL certificate from Let's Encrypt...${NC}"
echo "This may take a few minutes..."
sudo certbot certonly --nginx -d $DOMAIN -d www.$DOMAIN \
    --non-interactive --agree-tos --email $EMAIL \
    --preferred-challenges http 2>&1 | grep -E "Certificate|ERROR|Success" || true
echo -e "${GREEN}✓ SSL certificate obtained${NC}"
echo ""

# Step 6: Configure Nginx
echo -e "${YELLOW}Step 6: Configuring Nginx...${NC}"

sudo tee /etc/nginx/conf.d/wordai.conf > /dev/null <<EOF
# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN www.$DOMAIN;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

# HTTPS configuration
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $DOMAIN www.$DOMAIN;
    
    ssl_certificate /etc/letsencrypt/live/$DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$DOMAIN/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host \$server_name;
        
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/javascript application/json;
}
EOF

# Test Nginx configuration
if sudo nginx -t 2>&1 | grep -q "successful"; then
    sudo systemctl reload nginx
    echo -e "${GREEN}✓ Nginx configured and reloaded${NC}"
else
    echo -e "${RED}✗ Nginx configuration test failed${NC}"
    sudo nginx -t
    exit 1
fi
echo ""

# Step 7: Set up auto-renewal
echo -e "${YELLOW}Step 7: Setting up auto-renewal...${NC}"

sudo tee /usr/local/bin/renew-ssl.sh > /dev/null <<'EOF'
#!/bin/bash
/usr/bin/certbot renew --quiet
sudo systemctl reload nginx
echo "SSL certificate renewal completed on $(date)" >> /var/log/ssl-renewal.log
EOF

sudo chmod +x /usr/local/bin/renew-ssl.sh

# Add to crontab
CRON_JOB="0 2 * * * /usr/local/bin/renew-ssl.sh >> /var/log/ssl-renewal.log 2>&1"
if ! sudo crontab -l 2>/dev/null | grep -q "renew-ssl.sh"; then
    (sudo crontab -l 2>/dev/null; echo "$CRON_JOB") | sudo crontab -
    echo -e "${GREEN}✓ Auto-renewal cron job added${NC}"
else
    echo -e "${GREEN}✓ Auto-renewal already configured${NC}"
fi
echo ""

# Step 8: Verification
echo -e "${YELLOW}Step 8: Verifying setup...${NC}"
sleep 2

if curl -s -I https://$DOMAIN 2>/dev/null | grep -q "200\|301\|302"; then
    echo -e "${GREEN}✓ HTTPS connection successful${NC}"
else
    echo -e "${YELLOW}⚠ Could not verify connection. Waiting for DNS propagation...${NC}"
fi

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}HTTPS Setup Complete!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo -e "Your application is now available at:"
echo -e "  ${GREEN}https://$DOMAIN${NC}"
echo ""
echo "SSL Certificate Details:"
sudo certbot certificates | grep -A 5 "$DOMAIN" || true
echo ""
echo -e "${BLUE}Important Information:${NC}"
echo "  • Certificate expires: $(sudo certbot certificates 2>/dev/null | grep "Expiry Date" | tail -1 || echo 'See above')"
echo "  • Auto-renewal: Enabled (daily at 2 AM)"
echo "  • Logs: /var/log/ssl-renewal.log"
echo "  • Nginx config: /etc/nginx/conf.d/wordai.conf"
echo ""
echo -e "${BLUE}Test Your SSL Certificate:${NC}"
echo "  https://www.ssllabs.com/ssltest/analyze.html?d=$DOMAIN"
echo ""
