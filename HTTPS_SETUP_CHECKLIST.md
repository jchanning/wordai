# HTTPS Setup Quick Reference Checklist

**Goal:** Convert http://130.162.184.150:8080 → https://yourdomain.com with HTTPS encryption

---

## Pre-Setup Requirements

- [ ] Domain name registered and accessible
- [ ] Domain registrar login credentials ready
- [ ] SSH access to Oracle Cloud instance (130.162.184.150)
- [ ] SSH key available: `~/.ssh/arm-wordai.key`
- [ ] Valid email address for Let's Encrypt

---

## Phase 1: DNS Configuration (Registrar)

**Time: 5 minutes + 5-30 minutes for DNS propagation**

- [ ] Log in to domain registrar (GoDaddy, Namecheap, Google Domains, etc.)
- [ ] Find DNS settings / Records section
- [ ] Create A Record:
  - Name: `@` (for yourdomain.com)
  - Type: A
  - TTL: 3600
  - Value: `130.162.184.150`
- [ ] OR create for www subdomain:
  - Name: `www`
  - Type: CNAME
  - TTL: 3600
  - Value: `yourdomain.com`
- [ ] Wait 5-30 minutes for DNS to propagate
- [ ] Verify: `nslookup yourdomain.com`

---

## Phase 2: Oracle Cloud Configuration (5 minutes)

**SSH to instance:**
```bash
ssh -i ~/.ssh/arm-wordai.key opc@130.162.184.150
```

**Add firewall rules:**
```bash
# HTTP (for Let's Encrypt validation)
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent

# HTTPS
sudo firewall-cmd --zone=public --add-port=443/tcp --permanent

# Reload
sudo firewall-cmd --reload
```

**Also in Oracle Cloud Console:**
- [ ] Go to: Networking → Virtual Cloud Networks → Your VCN
- [ ] Select your Subnet → Security List
- [ ] Add Ingress Rules:
  - [ ] TCP port 80 (HTTP) from 0.0.0.0/0
  - [ ] TCP port 443 (HTTPS) from 0.0.0.0/0

---

## Phase 3: Automated Setup (10-15 minutes)

**Option A: Use Automated Script (Recommended)**

```bash
# Download or copy the setup-https.sh script to the instance
# Then run:
cd ~/wordai-app
chmod +x setup-https.sh
./setup-https.sh yourdomain.com your-email@example.com
```

**Option B: Manual Setup**

```bash
# Update system and install packages
sudo yum update -y
sudo yum install -y nginx certbot python3-certbot-nginx

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Get SSL certificate
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com

# Create Nginx config (use template from HTTPS_SETUP_GUIDE.md)
sudo nano /etc/nginx/conf.d/wordai.conf

# Verify and reload
sudo nginx -t
sudo systemctl reload nginx

# Set up auto-renewal
sudo certbot renew --dry-run
```

---

## Phase 4: Verification (5 minutes)

**Check certificate:**
```bash
sudo certbot certificates
```

**Test HTTPS connection:**
```bash
curl -I https://yourdomain.com
# Should see: HTTP/1.1 200 OK
```

**Browser test:**
- [ ] Open https://yourdomain.com in browser
- [ ] Verify green lock icon
- [ ] Test that WordAI application loads

**SSL Quality Test:**
- [ ] Go to: https://www.ssllabs.com/ssltest/analyze.html?d=yourdomain.com
- [ ] Wait for results (may take 1-2 minutes)
- [ ] Verify grade is A or A+

---

## Phase 5: Monitoring (Ongoing)

- [ ] Certificate expiry: 90 days from issue
- [ ] Auto-renewal: Checked weekly by cron job
- [ ] Monitor: `/var/log/ssl-renewal.log`
- [ ] Manual check: `sudo certbot certificates`

---

## Troubleshooting

### DNS Not Resolving
```bash
# Force DNS refresh
sudo systemctl restart systemd-resolved
nslookup yourdomain.com
```

### Certificate Renewal Failed
```bash
# Check Certbot logs
sudo tail -50 /var/log/letsencrypt/letsencrypt.log

# Force renewal
sudo certbot renew --force-renewal
```

### Nginx Not Working
```bash
# Check Nginx syntax
sudo nginx -t

# Check logs
sudo tail -50 /var/log/nginx/error.log

# Restart Nginx
sudo systemctl restart nginx
```

### WordAI Not Accessible Through Nginx
```bash
# Verify WordAI is running on port 8080
sudo netstat -tulpn | grep 8080

# Check Nginx is proxying correctly
sudo tail -50 /var/log/nginx/access.log
```

---

## Timeline

| Phase | Time | Status |
|-------|------|--------|
| **1. DNS Setup** | 5 min + propagation | Start here |
| **2. Firewall Config** | 5 min | → |
| **3. Installation & Setup** | 10-15 min | → |
| **4. Verification** | 5 min | → |
| **5. Monitoring** | Ongoing | ✓ Complete |

**Total Active Time:** ~25-30 minutes  
**Total Wait Time:** 5-30 minutes (DNS propagation)

---

## Key Files & Locations

| File/Location | Purpose |
|---|---|
| `/etc/nginx/conf.d/wordai.conf` | Nginx reverse proxy configuration |
| `/etc/letsencrypt/live/yourdomain.com/` | SSL certificates |
| `/var/log/nginx/access.log` | Nginx access logs |
| `/var/log/nginx/error.log` | Nginx error logs |
| `/var/log/ssl-renewal.log` | Certificate renewal log |
| `/usr/local/bin/renew-ssl.sh` | Renewal script |

---

## Important Commands Reference

```bash
# Check certificate details
sudo certbot certificates

# Renew certificate manually
sudo certbot renew

# Test renewal without actually renewing
sudo certbot renew --dry-run

# View Nginx status
sudo systemctl status nginx

# Reload Nginx (no downtime)
sudo systemctl reload nginx

# Restart Nginx (brief downtime)
sudo systemctl restart nginx

# Monitor SSL renewal logs
tail -f /var/log/ssl-renewal.log

# Check if ports 80/443 are open
sudo firewall-cmd --list-ports
```

---

## Post-Setup Tasks

After HTTPS is working:

- [ ] Update any hardcoded HTTP URLs in application to use HTTPS
- [ ] Update bookmarks/links to use `https://yourdomain.com`
- [ ] Add domain to browser home page
- [ ] Set up email forwarding at registrar (optional)
- [ ] Consider CDN/DDoS protection (Cloudflare, etc.) - optional
- [ ] Monitor certificate renewal logs monthly

---

## Security Summary

After completing setup:

| Feature | Status |
|---------|--------|
| **Encryption** | ✅ HTTPS/TLS 1.2+ |
| **Certificate** | ✅ Let's Encrypt (free, auto-renewing) |
| **Domain** | ✅ Custom domain name |
| **Redirect** | ✅ HTTP → HTTPS |
| **Headers** | ✅ Security headers configured |
| **HSTS** | ✅ Enabled (1 year) |

---

## Support Resources

- Let's Encrypt Docs: https://letsencrypt.org/docs/
- Certbot Docs: https://certbot.eff.org/
- Nginx Documentation: https://nginx.org/en/docs/
- SSL Labs Best Practices: https://github.com/ssllabs/research/wiki/SSL-and-TLS-Deployment-Best-Practices
