# GoDaddy DNS Configuration for WordAI HTTPS Setup

**Quick Guide for Setting Up DNS with GoDaddy**

---

## Step 1: Access GoDaddy DNS Management

1. Go to https://www.godaddy.com and sign in
2. Click your profile icon (top right) → **My Products**
3. Find your domain in the list
4. Click the **DNS** button next to your domain
   - Or click the domain name, then go to **DNS** tab

---

## Step 2: Create the A Record

Once you're in the DNS Management page:

### Add/Edit A Record for Root Domain

1. Scroll to the **Records** section
2. Look for an existing **A** record with Name **@**
   - If it exists: Click the **pencil icon** to edit
   - If not: Click **Add** button

3. Configure the A record:
   ```
   Type:    A
   Name:    @
   Value:   130.162.184.150
   TTL:     600 (10 minutes - for quick testing)
            or 3600 (1 hour - for production)
   ```

4. Click **Save**

### Add A Record for www Subdomain (Optional but Recommended)

1. Click **Add** to create a new record
2. Configure:
   ```
   Type:    A
   Name:    www
   Value:   130.162.184.150
   TTL:     600 or 3600
   ```

3. Click **Save**

---

## Step 3: Verify DNS Configuration

After saving, your records should look like this:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| A | @ | 130.162.184.150 | 600 |
| A | www | 130.162.184.150 | 600 |

---

## Step 4: Wait for DNS Propagation

- **Typical time:** 5-10 minutes with TTL=600
- **Maximum time:** Up to 30 minutes
- **GoDaddy is usually fast:** Often works in < 5 minutes

### Check Propagation Status

**Windows (PowerShell):**
```powershell
nslookup yourdomain.com
```

**Expected output:**
```
Server:  UnKnown
Address:  ...

Non-authoritative answer:
Name:    yourdomain.com
Address:  130.162.184.150
```

**Online tools:**
- https://dnschecker.org/ (checks from multiple locations worldwide)
- https://www.whatsmydns.net/

---

## Important Notes

### TTL (Time To Live)
- **600 seconds (10 minutes):** Good for testing/setup phase
- **3600 seconds (1 hour):** Good for production
- You can change TTL back to 3600 after everything is working

### Common GoDaddy Issues

**Issue:** "This record is already in use"
- **Solution:** Edit the existing record instead of creating new one

**Issue:** Default records present
- **Solution:** Replace/update them with your Oracle Cloud IP

**Issue:** Parked domain page still showing
- **Solution:** 
  1. Go to **Settings** tab in GoDaddy
  2. Turn off **Domain forwarding** if enabled
  3. Turn off **Parked page** if enabled

### What NOT to Do

❌ Don't delete nameserver records (NS records)
❌ Don't modify the SOA record
❌ Don't change MX records unless you need email

---

## Visual Reference

Your GoDaddy DNS page should look similar to:

```
[Add] button

Records
┌─────┬──────┬──────────────────┬──────┬────────┐
│Type │ Name │ Value            │ TTL  │ Actions│
├─────┼──────┼──────────────────┼──────┼────────┤
│  A  │  @   │ 130.162.184.150  │ 600  │ [Edit] │
│  A  │ www  │ 130.162.184.150  │ 600  │ [Edit] │
│ NS  │  @   │ ns**.domainco... │ 3600 │        │
│ SOA │  @   │ Primary namese...│ 3600 │        │
└─────┴──────┴──────────────────┴──────┴────────┘
```

---

## Next Steps After DNS is Configured

Once your DNS records are saved and propagating:

1. ✅ **Wait 5-10 minutes** for propagation
2. ✅ **Test DNS resolution** with `nslookup yourdomain.com`
3. ✅ **Proceed to Oracle Cloud setup** (next step)

---

## Need Help?

**GoDaddy Support:**
- Help Center: https://www.godaddy.com/help/
- Phone: Check GoDaddy website for support number
- Chat: Available in account dashboard

**Common GoDaddy DNS Documentation:**
- https://www.godaddy.com/help/add-an-a-record-19238
- https://www.godaddy.com/help/manage-dns-records-680

---

## Ready for Next Step?

Once DNS is configured and propagating, you'll:
1. SSH to your Oracle Cloud instance
2. Run the HTTPS setup script
3. Let Let's Encrypt verify your domain ownership
4. Access your site at https://yourdomain.com

**Estimated total time from now:** 15-20 minutes active + 5-30 min DNS wait
