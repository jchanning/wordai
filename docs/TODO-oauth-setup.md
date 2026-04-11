# TODO: Configure Google OAuth Login

## Problem
Clicking "Sign in with Google" returns **Error 401: invalid_client** because no real OAuth credentials are configured on the production server. The placeholder values `YOUR_GOOGLE_CLIENT_ID` / `YOUR_GOOGLE_CLIENT_SECRET` from `application.properties` are being used.

## What Needs to Be Done

### Step 1 — Create Google OAuth credentials
1. Go to [console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials)
2. Click **Create Credentials → OAuth 2.0 Client ID**
3. Application type: **Web application**
4. Add this **Authorised redirect URI**:
   ```
   http://132.145.64.140:8080/login/oauth2/code/google
   ```
5. Note down the **Client ID** and **Client Secret**

### Step 2 — Add credentials to server config
No rebuild is needed. SSH into the server and edit the external properties file:

```
/home/opc/wordai-app/application-prod.properties
```

Add these lines (replacing the placeholders with the real values):

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_REAL_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_REAL_CLIENT_SECRET
spring.security.oauth2.client.registration.google.redirect-uri=http://132.145.64.140:8080/login/oauth2/code/google
```

### Step 3 — Restart the service
```bash
sudo systemctl restart wordai
sudo systemctl status wordai
```

## Server Details
- **IP**: `132.145.64.140`
- **SSH key**: `C:\Users\johnm\.ssh\arm-wordai.key`
- **SSH user**: `opc`
- **Config file**: `/home/opc/wordai-app/application-prod.properties`
- **Service**: `wordai.service` (systemd)

## Notes
- The fix requires **no code change and no rebuild** — only updating the server config file and restarting the service.
- If Apple Sign-In is also needed, credentials come from [developer.apple.com](https://developer.apple.com/account/resources/identifiers/list/serviceId) and follow a similar pattern.
