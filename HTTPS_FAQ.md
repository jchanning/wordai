# HTTPS Setup FAQ & Architecture Overview

---

## Frequently Asked Questions

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

**A:** Nginx acts as a reverse proxy:
- **HTTPS Termination:** Handles SSL/TLS encryption
- **HTTP Redirect:** Automatically redirects HTTP → HTTPS
- **Port Mapping:** Routes requests from port 443 (HTTPS) to port 8080 (app)
- **Compression:** Gzip compression for faster delivery
- **Security Headers:** Adds security headers to responses
- **Load Balancing:** (For future scalability)

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

**A:** 
- **Change domain:** Generate new certificate for new domain
- **Change server:** Export certificate from old server, import on new (or regenerate)

### Q: Is my certificate tied to my domain or server?

**A:** **Tied to your domain name.** The certificate is valid for `yourdomain.com` regardless of which server hosts it. If you move to a different server:
- Certificate still works (if you copy it)
- Or simply regenerate for the new server

### Q: What's the difference between www.yourdomain.com and yourdomain.com?

**A:** Technically, they're different:
- `yourdomain.com` = Root domain (naked domain)
- `www.yourdomain.com` = Subdomain

Our setup works for both using a single certificate with both names listed.

### Q: Do I need a static IP address?

**A:** You already have one! Oracle Cloud instance IP `130.162.184.150` is static. If it changes, you'd update your DNS records.

### Q: What if I want to use a CDN like Cloudflare?

**A:** You can! Cloudflare can sit between your domain and Oracle Cloud:
1. Point domain to Cloudflare nameservers
2. Cloudflare manages DNS
3. Cloudflare → Oracle Cloud (traffic flows through Cloudflare's network)

Advantages:
- DDoS protection
- Global caching
- Additional security features
- Often free tier available

### Q: How do I monitor if my certificate is about to expire?

**A:** Certbot sends email reminders. Also:
```bash
# Check expiry date
sudo certbot certificates

# Check from browser's padlock icon → Certificate Details
```

---

## Architecture Overview

### Before HTTPS Setup

```
Internet User
    ↓
Browser (unsecured)
    ↓ HTTP
http://130.162.184.150:8080
    ↓
Firewall (Port 8080 only)
    ↓
WordAI App (Java Spring Boot)
    ↓ Port 8080
    ✗ No encryption
    ✗ Unencrypted data visible on network
    ✗ Browser shows warning
```

### After HTTPS Setup

```
Internet User
    ↓
Browser
    ↓ HTTPS (encrypted)
https://yourdomain.com
    ↓ 
DNS Lookup: yourdomain.com → 130.162.184.150
    ↓
Firewall (Ports 80, 443 allowed)
    ↓
Nginx Reverse Proxy (Port 443)
    ├─ SSL/TLS Encryption/Decryption
    ├─ Security Headers
    └─ Request Routing
    ↓
Application (Localhost:8080)
    ├─ No longer exposed to internet
    └─ Only talks to Nginx locally
    
✓ All traffic encrypted
✓ Browser shows lock icon
✓ Domain name is professional
✓ Auto-renewing certificate
```

### Network Flow

```
                    ┌─────────────────────────────────────┐
                    │    Internet (Untrusted)             │
                    │    HTTPS Encrypted Connection       │
                    └────────┬────────────────────────────┘
                             │
                    ┌────────▼──────────┐
                    │  DNS Resolution   │
                    │  yourdomain.com   │
                    │  → 130.162.184.150│
                    └────────┬──────────┘
                             │
                    ┌────────▼──────────────┐
                    │   Oracle Cloud VM     │
                    │  130.162.184.150      │
                    └────────┬──────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │ Port 80 (HTTP)     │ Port 443 (HTTPS)   │
        │ (Redirect only)    │ (Main traffic)     │
        └────────────────────┼────────────────────┘
                             │
                    ┌────────▼──────────────┐
                    │  Nginx Reverse Proxy  │
                    │  ┌──────────────────┐ │
                    │  │ SSL Termination  │ │
                    │  │ Decrypt HTTPS    │ │
                    │  └──────────────────┘ │
                    │  ┌──────────────────┐ │
                    │  │ HTTP Redirect    │ │
                    │  │ Port 80 → 443    │ │
                    │  └──────────────────┘ │
                    │  ┌──────────────────┐ │
                    │  │ Reverse Proxy    │ │
                    │  │ localhost:8080   │ │
                    │  └──────────────────┘ │
                    └────────┬──────────────┘
                             │
                ┌────────────▼─────────────┐
                │ WordAI Application       │
                │ Port 8080 (Internal)     │
                │ No internet exposure     │
                │ - Entropy calculations   │
                │ - Game logic             │
                │ - Dictionary processing  │
                └──────────────────────────┘
```

---

## Timeline & Dependencies

```
Phase 1: DNS Setup (5 min + propagation)
    ↓ (DNS propagation: 5-30 min)
    
Phase 2: Firewall & Oracle Cloud (5 min)
    ↓
    
Phase 3: Install & Configure (10-15 min)
    ├─ Install Nginx
    ├─ Install Certbot
    ├─ Obtain SSL certificate
    ├─ Configure reverse proxy
    └─ Set up auto-renewal
    ↓
    
Phase 4: Verification (5 min)
    ├─ Test HTTPS
    ├─ Check certificate
    └─ Verify browser access
    ↓
    
✓ COMPLETE
```

---

## Comparison: Before vs After

| Aspect | Before | After |
|--------|--------|-------|
| **URL** | http://130.162.184.150:8080 | https://yourdomain.com |
| **Encryption** | None | TLS 1.2+ |
| **Certificate** | None | Let's Encrypt |
| **Port** | 8080 | 443 (standard HTTPS) |
| **Professional** | No (IP address) | Yes (domain name) |
| **Browser Warning** | Yes (unencrypted) | No (lock icon) |
| **Auto-Renewal** | N/A | Yes |
| **Setup Time** | N/A | ~30 minutes |
| **Cost** | Free | Free |
| **Firewall Ports** | 8080 | 80, 443 |

---

## Decision Tree: What Should I Do?

```
Do you have a domain name?
    │
    ├─ NO → Purchase one first at GoDaddy, Namecheap, Google Domains, etc.
    │       Then return here
    │
    └─ YES
        │
        ├─ Setup time available?
        │   │
        │   ├─ YES → Follow HTTPS_SETUP_GUIDE.md (recommended)
        │   │
        │   └─ NO  → Use automated script: setup-https.sh
        │           (Still requires ~20 min of setup)
        │
        ├─ Technical comfort?
        │   │
        │   ├─ Very comfortable → Manual setup (more control)
        │   │
        │   └─ Prefer automated → Use setup-https.sh script
        │
        └─ Budget for tools?
            │
            ├─ Minimal cost → Use Let's Encrypt + Nginx
            │               (Completely free)
            │
            └─ Can spend → Consider Cloudflare (free tier) +
                           Premium CDN/DDoS protection
```

---

## Checklist Summary

- [ ] Domain purchased and accessible
- [ ] DNS A record points to 130.162.184.150
- [ ] Firewall rules allow ports 80 & 443
- [ ] Nginx installed and running
- [ ] SSL certificate obtained from Let's Encrypt
- [ ] Nginx configured as reverse proxy
- [ ] Auto-renewal set up
- [ ] HTTPS verified working
- [ ] Browser shows lock icon
- [ ] SSL Labs test passes
- [ ] Monitoring log configured

---

## Next Steps

1. **Decide on domain:** Do you have one, or need to purchase?
2. **Review timeline:** ~30-60 minutes total
3. **Choose setup method:**
   - Detailed: Follow [HTTPS_SETUP_GUIDE.md](HTTPS_SETUP_GUIDE.md)
   - Quick: Use [setup-https.sh](deployment/setup-https.sh) script
4. **Follow checklist:** Use [HTTPS_SETUP_CHECKLIST.md](HTTPS_SETUP_CHECKLIST.md)
5. **Test & verify:** Ensure everything works
6. **Monitor:** Check renewal logs monthly

---

## Support & Troubleshooting

If you encounter issues:

1. **Check the relevant troubleshooting section** in HTTPS_SETUP_GUIDE.md
2. **Review logs:**
   - Nginx: `/var/log/nginx/error.log`
   - Certbot: `/var/log/letsencrypt/letsencrypt.log`
   - Renewal: `/var/log/ssl-renewal.log`
3. **Common issues:**
   - DNS not resolving → Wait longer or check registrar
   - Certificate renewal failed → Check Certbot logs
   - Nginx not proxying → Verify WordAI is running on 8080

For more help:
- Let's Encrypt Community: https://community.letsencrypt.org/
- Nginx Documentation: https://nginx.org/en/docs/
- Certbot Documentation: https://certbot.eff.org/docs/
