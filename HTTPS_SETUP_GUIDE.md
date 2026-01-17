# Setting Up Custom Domain with HTTPS on Oracle Cloud

**Target:** WordAI application  
**Current URL:** http://130.162.184.150:8080  
**Goal:** https://yourdomain.com with SSL/TLS encryption

---

## Overview

To use your custom domain with HTTPS, you'll need to:

1. **Point your domain to the Oracle Cloud instance** (DNS configuration)
2. **Install Nginx** as a reverse proxy
3. **Obtain a free SSL certificate** from Let's Encrypt
4. **Configure Nginx** to serve HTTPS and proxy to the application
5. **Set up auto-renewal** for the SSL certificate
6. **Configure Oracle Cloud firewall** for HTTPS traffic

---

## Step 1: DNS Configuration

Contact your domain registrar and point your domain to your Oracle Cloud instance.

### Option A: Using A Record (Recommended)

**Create or update an A record in your domain's DNS settings:**

| Field | Value |
|-------|-------|
| **Type** | A |
| **Name** | @ (for root) or www (for www.yourdomain.com) |
| **TTL** | 3600 (1 hour, or 300 for testing) |
| **Value** | 130.162.184.150 |

**Common registrars:**
- GoDaddy, Namecheap, Google Domains, Route53, Cloudflare, etc.
- Steps are similar: Log in → Domain settings → DNS records → Add A record

### Option B: Using CNAME Record

If you prefer www.yourdomain.com:

| Field | Value |
|-------|-------|
| **Type** | CNAME |
| **Name** | www |
| **TTL** | 3600 |
| **Value** | yourdomain.com |

### Verification

Test DNS resolution (wait 5-30 minutes for propagation):

```bash
nslookup yourdomain.com
# or
dig yourdomain.com
```

---

## Step 2: Configure Oracle Cloud Firewall (Ingress Rules)

SSH to your instance and add firewall rules for HTTP/HTTPS:

```bash
# Add HTTP (port 80) for Let's Encrypt validation
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent

# Add HTTPS (port 443)
sudo firewall-cmd --zone=public --add-port=443/tcp --permanent

# Reload firewall
sudo firewall-cmd --reload

# Verify
sudo firewall-cmd --list-ports
```

**Also update OCI Security List** (in Oracle Cloud Console):
- Go to Networking → Virtual Cloud Networks → Your VCN
- Click your Subnet → Security List
- Add Ingress Rules:
  - Stateless: TCP, Port 80 (HTTP)
  - Stateless: TCP, Port 443 (HTTPS)
  - Source: 0.0.0.0/0 (allow all)

---

## Step 3: Install Nginx and Certbot

SSH to your instance and install required packages:

```bash
# Update system
sudo yum update -y

# Install Nginx
sudo yum install -y nginx

# Install Certbot and Let's Encrypt
sudo yum install -y certbot python3-certbot-nginx

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Verify Nginx is running
sudo systemctl status nginx
```

---

## Step 4: Obtain SSL Certificate

Use Certbot to automatically obtain and configure SSL certificate:

```bash
# Replace yourdomain.com with your actual domain
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com

# Or for manual validation (if Nginx isn't configured yet):
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
```

**Follow the prompts:**
- Enter email address
- Agree to terms
- Choose whether to share email with EFF
- Wait for certificate validation

**Certificate location:**
```
/etc/letsencrypt/live/yourdomain.com/
├── cert.pem           (certificate)
├── chain.pem          (intermediate certs)
├── fullchain.pem      (full chain - use this)
└── privkey.pem        (private key)
```

---

## Step 5: Configure Nginx as Reverse Proxy

Create/edit the Nginx configuration for your domain:

```bash
sudo nano /etc/nginx/conf.d/wordai.conf
```

Add the following configuration:

```nginx
# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com www.yourdomain.com;
    
    # Allow Let's Encrypt validation
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    # Redirect all other traffic to HTTPS
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS configuration
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    # SSL certificates from Let's Encrypt
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    # SSL configuration (security best practices)
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # HSTS (HTTP Strict Transport Security)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    
    # Proxy configuration
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $server_name;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_types text/plain text/css text/xml text/javascript 
               application/x-javascript application/xml+rss 
               application/javascript application/json;
}
```

**Save and verify the configuration:**

```bash
# Test Nginx configuration syntax
sudo nginx -t

# If OK, reload Nginx
sudo systemctl reload nginx
```

---

## Step 6: Auto-Renewal Setup

Let's Encrypt certificates expire after 90 days. Set up automatic renewal:

```bash
# Create renewal script
sudo nano /usr/local/bin/renew-ssl.sh
```

Add this content:

```bash
#!/bin/bash

# Certbot automatic renewal
/usr/bin/certbot renew --quiet

# Reload Nginx to use new certificate
sudo systemctl reload nginx

# Optional: Log renewal
echo "SSL certificate renewal completed on $(date)" >> /var/log/ssl-renewal.log
```

Make it executable:

```bash
sudo chmod +x /usr/local/bin/renew-ssl.sh
```

**Set up automated renewal with cron:**

```bash
# Edit crontab
sudo crontab -e

# Add this line (runs daily at 2 AM):
0 2 * * * /usr/local/bin/renew-ssl.sh >> /var/log/ssl-renewal.log 2>&1

# Save and exit
```

Or use systemd timer (alternative to cron):

```bash
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
sudo systemctl status certbot.timer
```

---

## Step 7: Verification & Testing

### Test HTTPS Access

```bash
# Test your domain
curl -I https://yourdomain.com

# Should return:
# HTTP/1.1 200 OK
# ...with SSL/TLS connection established
```

### Test SSL Certificate

```bash
# Check certificate validity
echo | openssl s_client -servername yourdomain.com -connect yourdomain.com:443 2>/dev/null | openssl x509 -noout -dates

# Check certificate expiration
sudo certbot certificates
```

### Online SSL Validator

Use SSL Labs to test your configuration:
https://www.ssllabs.com/ssltest/analyze.html?d=yourdomain.com

### Browser Testing

1. Open https://yourdomain.com in your browser
2. You should see the WordAI application
3. Check the browser's address bar for the lock icon (secure connection)

---

## Step 8: Update Application Configuration (Optional)

If your application generates URLs, configure Spring Boot to use HTTPS:

**Edit `/home/opc/wordai-app/wordai.properties`:**

```properties
# Enable HTTPS headers awareness
server.servlet.context-parameters.force-https=true

# Trust proxy headers (important for reverse proxy)
server.tomcat.remoteip.remote-ip-header=x-forwarded-for
server.tomcat.remoteip.protocol-header=x-forwarded-proto
server.tomcat.remoteip.protocol-header-value=https

# Force secure cookies
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
```

Restart the application:

```bash
sudo systemctl restart wordai
```

---

## Troubleshooting

### Certificate Renewal Failed

```bash
# Force renewal
sudo certbot renew --force-renewal

# Check renewal logs
sudo tail -50 /var/log/letsencrypt/letsencrypt.log
```

### Nginx Not Proxying Correctly

```bash
# Check Nginx logs
sudo tail -50 /var/log/nginx/access.log
sudo tail -50 /var/log/nginx/error.log

# Verify WordAI is running on port 8080
sudo netstat -tulpn | grep 8080
```

### Mixed Content Errors

If you see "mixed content" warnings in browser:
- Ensure all resources are served as HTTPS
- Check Nginx headers are set correctly
- Update `X-Forwarded-Proto` header

### DNS Not Resolving

```bash
# Force clear DNS cache
sudo systemctl restart systemd-resolved

# Test resolution again
nslookup yourdomain.com
```

---

## Summary of Changes

After following these steps:

| Property | Before | After |
|----------|--------|-------|
| **URL** | http://130.162.184.150:8080 | https://yourdomain.com |
| **Protocol** | HTTP (unencrypted) | HTTPS (encrypted) |
| **Certificate** | None | Let's Encrypt (free) |
| **Auto-Renew** | N/A | Yes (90 days) |
| **Browser Security** | No lock icon | Green lock icon |

---

## Security Checklist

- ✅ Domain points to Oracle Cloud instance
- ✅ Firewall rules allow ports 80 (HTTP) and 443 (HTTPS)
- ✅ SSL certificate obtained and installed
- ✅ Nginx configured as reverse proxy
- ✅ HTTP redirects to HTTPS
- ✅ Security headers configured
- ✅ Auto-renewal enabled
- ✅ Browser shows secure connection

---

## Important Notes

1. **Replace `yourdomain.com`** with your actual domain name throughout all commands
2. **Email address** - Certbot needs a valid email for expiration notices
3. **DNS propagation** - Can take 5-30 minutes, be patient
4. **Certificate renewal** - Certbot handles this automatically; check logs monthly
5. **Backup certificates** - Keep backups of `/etc/letsencrypt/` directory
6. **Monitoring** - Monitor `/var/log/letsencrypt/` for renewal issues

---

## Next Steps

1. Purchase/verify your domain name
2. Update DNS records at your registrar
3. Follow Steps 2-7 above
4. Test with `https://yourdomain.com`
5. Monitor renewal logs for the first month

Once complete, your WordAI application will be:
- **Secure** (HTTPS with Let's Encrypt SSL)
- **Professional** (custom domain)
- **Reliable** (auto-renewing certificates)
- **SEO-friendly** (HTTPS is preferred by search engines)
