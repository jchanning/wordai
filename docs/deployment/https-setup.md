# HTTPS Setup for WordAI - Complete Guide

**Status:** Production-ready  
**Target Platform:** Oracle Cloud  
**Current URL:** http://130.162.184.150:8080  
**Goal:** https://yourdomain.com with SSL/TLS encryption

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Quick Reference Checklist](#quick-reference-checklist)
4. [Step-by-Step Setup](#step-by-step-setup)
5. [Troubleshooting & FAQ](#troubleshooting--faq)
6. [Maintenance](#maintenance)

---

## Overview

To use your custom domain with HTTPS, you'll need to:

1. **Point your domain to the Oracle Cloud instance** (DNS configuration)
2. **Install Nginx** as a reverse proxy
3. **Obtain a free SSL certificate** from Let's Encrypt
4. **Configure Nginx** to serve HTTPS and proxy to the application
5. **Set up auto-renewal** for the SSL certificate
6. **Configure Oracle Cloud firewall** for HTTPS traffic

This guide walks through each step with commands and troubleshooting tips.

---

## Architecture

### System Diagram

```
Browser (HTTPS)
    ↓
Internet
    ↓
Oracle Cloud Firewall (ports 80, 443)
    ↓
Nginx (Reverse Proxy)
    - SSL/TLS Termination
    - HTTP → HTTPS redirect
    ↓
Spring Boot Application (Port 8080)
    - WordAI
```

### Why Nginx?

Nginx acts as a reverse proxy and provides:
- **HTTPS Termination:** Handles SSL/TLS encryption
- **HTTP Redirect:** Automatically redirects HTTP → HTTPS
- **Port Mapping:** Routes requests from port 443 (HTTPS) to port 8080 (app)
- **Compression:** Gzip compression for faster delivery
- **Security Headers:** Adds security headers to responses
- **Load Balancing:** For future scalability

---

## Quick Reference Checklist

**Time to Complete:** ~30-45 minutes (including DNS propagation)

### Pre-Setup Requirements

- [ ] Domain name registered and accessible
- [ ] Domain registrar login credentials ready
- [ ] SSH access to Oracle Cloud instance (130.162.184.150)
- [ ] SSH key available: `~/.ssh/arm-wordai.key`
- [ ] Valid email address for Let's Encrypt

### Phase 1: DNS Configuration

- [ ] Log in to domain registrar
- [ ] Find DNS settings / Records section
- [ ] Create A Record pointing to `130.162.184.150`
- [ ] Wait 5-30 minutes for DNS propagation
- [ ] Verify with: `nslookup yourdomain.com`

### Phase 2: Oracle Cloud Configuration

- [ ] SSH to instance
- [ ] Add firewall rules for ports 80 and 443
- [ ] Update OCI Security List in cloud console

### Phase 3: Automated Setup

- [ ] Run `./setup-https.sh yourdomain.com your-email@example.com`
- [ ] Or manually follow manual setup steps below

---

## Step-by-Step Setup

### Step 1: DNS Configuration

Contact your domain registrar and point your domain to your Oracle Cloud instance.

#### Option A: Using A Record (Recommended)

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

#### Option B: Using CNAME Record

If you prefer www.yourdomain.com:

| Field | Value |
|-------|-------|
| **Type** | CNAME |
| **Name** | www |
| **TTL** | 3600 |
| **Value** | yourdomain.com |

#### Verification

Test DNS resolution (wait 5-30 minutes for propagation):

```bash
nslookup yourdomain.com
# or
dig yourdomain.com
```

---

### Step 2: Configure Oracle Cloud Firewall

SSH to your instance and add firewall rules for HTTP/HTTPS:

```bash
ssh -i ~/.ssh/arm-wordai.key opc@130.162.184.150

# Add HTTP (port 80) for Let's Encrypt validation
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent

# Add HTTPS (port 443)
sudo firewall-cmd --zone=public --add-port=443/tcp --permanent

# Reload firewall
sudo firewall-cmd --reload

# Verify
sudo firewall-cmd --list-ports
```

**Also update OCI Security List in Oracle Cloud Console:**
1. Go to Networking → Virtual Cloud Networks → Your VCN
2. Click your Subnet → Security List
3. Add Ingress Rules:
   - Stateless: TCP, Port 80 (HTTP)
   - Stateless: TCP, Port 443 (HTTPS)
   - Source: 0.0.0.0/0 (allow all)

---

### Step 3: Install Nginx and Certbot

```bash
# Update system
sudo yum update -y

# Install Nginx
sudo yum install -y nginx

# Install Certbot (Let's Encrypt client)
sudo yum install -y certbot python3-certbot-nginx

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

### Step 4: Obtain SSL Certificate

```bash
# Get SSL certificate from Let's Encrypt
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com

# Or use standalone if nginx isn't ready
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
```

Follow the prompts to enter your email and agree to terms.

---

### Step 5: Configure Nginx

Create or edit `/etc/nginx/conf.d/wordai.conf`:

```bash
sudo nano /etc/nginx/conf.d/wordai.conf
```

Add the following configuration:

```nginx
# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS configuration
server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL certificates
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css text/javascript application/json;

    # Proxy to Spring Boot application
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Replace `yourdomain.com` with your actual domain name.**

---

### Step 6: Verify and Reload Nginx

```bash
# Test configuration syntax
sudo nginx -t

# If OK, reload Nginx
sudo systemctl reload nginx

# Verify it's running
sudo systemctl status nginx
```

---

### Step 7: Set Up Auto-Renewal

Let's Encrypt certificates expire after 90 days. Certbot automatically renews them:

```bash
# Test the renewal process (dry-run)
sudo certbot renew --dry-run

# Enable auto-renewal cron job
sudo systemctl enable certbot-renew.timer
sudo systemctl start certbot-renew.timer

# Verify it's running
sudo systemctl status certbot-renew.timer
```

---

## Troubleshooting & FAQ

### Q: What is HTTPS and why do I need it?

**A:** HTTPS (HyperText Protocol Secure) encrypts data transmitted between your browser and the server. Benefits:
- **Security:** Browser ↔ Server communication is encrypted
- **Trust:** Users see a green lock icon, indicating the site is secure
- **SEO:** Google prioritizes HTTPS websites
- **Compliance:** Many regulations (PCI-DSS, GDPR) require HTTPS
- **Modern Standard:** Most browsers warn about non-HTTPS sites

### Q: What is SSL/TLS certificate?

**A:** A digital certificate that:
- Proves your domain ownership
- Enables encryption of data in transit
- Is verified by a trusted Certificate Authority (CA)
- Expires after a certain period (Let's Encrypt: 90 days)
- Must be renewed before expiration

### Q: What is Let's Encrypt?

**A:** A free, automated Certificate Authority that:
- Issues free SSL certificates
- Certificates valid for 90 days
- Supports automatic renewal
- Requires domain ownership verification
- Is widely trusted by all modern browsers

### Q: Do I have to pay for HTTPS?

**A:** No! We're using Let's Encrypt, which is completely free. You only pay for:
- Domain name (yearly, from registrar)
- Server hosting (Oracle Cloud)
- Optional: CDN or enhanced security features

### Q: How does DNS work?

**A:** When you type `yourdomain.com`:
1. Browser queries DNS nameservers
2. Nameserver looks up the A record
3. Returns IP address: `130.162.184.150`
4. Browser connects to that IP
5. Server responds

The A record you create points your domain name to your server's IP address.

### Q: Why do I need Nginx if I already have the application running?

**A:** See [Architecture](#architecture) section above. Nginx is essential for HTTPS termination, HTTP redirect, and security headers.

### Q: What happens after 90 days when the certificate expires?

**A:** Certbot automatically renews it. The renewal process:
- Happens in the background
- No manual intervention needed
- Certificate remains valid continuously
- Nginx is reloaded to use the new certificate
- An email is sent if renewal fails

### Q: Will my site go down during SSL setup?

**A:** No, but there may be brief moments of:
- Nginx restart (< 1 second)
- DNS propagation (5-30 minutes before domain works)
- Certificate renewal (automatic, happens in background)

### Q: How do I know if my HTTPS is working correctly?

**A:** Check these indicators:
1. **Browser address bar:** Shows green lock icon
2. **URL:** Starts with `https://` not `http://`
3. **Mixed content:** No warnings about insecure resources
4. **SSL Labs:** Get A or A+ grade at https://www.ssllabs.com/ssltest/

### Q: Can I use HTTPS with my custom domain?

**A:** Yes! This entire guide is about doing exactly that.

### Q: What if my domain registrar doesn't support DNS editing?

**A:** All major registrars support DNS records (A records). Check:
- GoDaddy, Namecheap, Google Domains, Cloudflare, Route53, etc.
- If your registrar doesn't support it, migrate to one that does (usually free)

### Q: Can I have both http://domain.com and https://domain.com?

**A:** Technically yes, but we configure automatic redirect: `http://` → `https://` so only HTTPS is actively used.

### Q: What if I change my domain or server?

**A:** You'll need to:
1. Update DNS to point to new server IP
2. Get a new certificate for the new domain
3. Update Nginx configuration
4. Restart services

### Common Issues

#### Issue: Certificate not renewing
```bash
# Check renewal logs
sudo tail -f /var/log/letsencrypt/letsencrypt.log

# Force renewal
sudo certbot renew --force-renewal
```

#### Issue: Nginx won't start
```bash
# Check for syntax errors
sudo nginx -t

# View error logs
sudo journalctl -u nginx -n 20
```

#### Issue: Domain not resolving
```bash
# Check DNS propagation
nslookup yourdomain.com
# or
dig yourdomain.com @8.8.8.8  # Google DNS
```

#### Issue: "Mixed Content" warning
Ensure all resources (images, CSS, JS) are loaded over HTTPS, not HTTP.

---

## Maintenance

### Regular Tasks

- [ ] **Monthly:** Check certificate expiration: `sudo certbot certificates`
- [ ] **Quarterly:** Review Nginx logs for errors
- [ ] **Annually:** Audit SSL/TLS configuration at https://www.ssllabs.com/ssltest/
- [ ] **As needed:** Update Nginx security headers based on latest OWASP recommendations

### Certificate Monitoring

```bash
# View all certificates
sudo certbot certificates

# Get expiration details
sudo certbot certificates --allow-subset-of-names
```

### Logs

```bash
# Nginx access logs
sudo tail -f /var/log/nginx/access.log

# Nginx error logs
sudo tail -f /var/log/nginx/error.log

# Certbot logs
sudo tail -f /var/log/letsencrypt/letsencrypt.log
```

---

## Related Documentation

- [Deployment Guide](deployment-guide.md)
- [Oracle Cloud Setup](oracle-cloud.md)
- [DNS Setup with GoDaddy](dns-setup.md)
