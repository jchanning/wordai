# WordAI Authentication Setup Guide

This guide explains how to set up authentication for WordAI, including Google and Apple OAuth.

## Overview

WordAI now supports three authentication methods:
1. **Username/Password** - Local registration and login
2. **Google OAuth** - Sign in with Google account
3. **Apple OAuth** - Sign in with Apple ID

## Database Configuration

The application uses H2 database for development. The database file is stored at `./data/wordai`.

To view the database:
1. Start the application
2. Navigate to http://localhost:8080/h2-console
3. Use these credentials:
   - JDBC URL: `jdbc:h2:file:./data/wordai`
   - Username: `sa`
   - Password: (leave blank)

## Setting Up Google OAuth

### 1. Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Navigate to "APIs & Services" → "Credentials"

### 2. Configure OAuth Consent Screen

1. Click "OAuth consent screen" in the left menu
2. Select "External" user type (or "Internal" if using Google Workspace)
3. Fill in required fields:
   - App name: `WordAI`
   - User support email: Your email
   - Developer contact: Your email
4. Add scopes:
   - `../auth/userinfo.email`
   - `../auth/userinfo.profile`
5. Save and continue

### 3. Create OAuth 2.0 Credentials

1. Click "Credentials" in the left menu
2. Click "Create Credentials" → "OAuth client ID"
3. Application type: "Web application"
4. Name: `WordAI Web Client`
5. Authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://yourdomain.com/login/oauth2/code/google` (for production)
6. Click "Create"
7. Copy the **Client ID** and **Client Secret**

### 4. Update application.properties

Replace the placeholders in `src/main/resources/application.properties`:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

## Setting Up Apple OAuth

### 1. Apple Developer Account

You need an [Apple Developer Account](https://developer.apple.com/account/) (paid membership required for production).

### 2. Register App ID

1. Go to [Apple Developer Portal](https://developer.apple.com/account/resources/identifiers/list)
2. Click "+" to register a new identifier
3. Select "App IDs" → Continue
4. Select "App" → Continue
5. Description: `WordAI`
6. Bundle ID: `com.fistraltech.wordai` (or your own)
7. Enable "Sign in with Apple"
8. Click "Continue" → "Register"

### 3. Create Services ID

1. Click "+" to register a new identifier
2. Select "Services IDs" → Continue
3. Description: `WordAI Web`
4. Identifier: `com.fistraltech.wordai.web`
5. Click "Continue" → "Register"
6. Click on the Services ID you just created
7. Enable "Sign in with Apple"
8. Click "Configure" next to Sign in with Apple
9. Primary App ID: Select your App ID from step 2
10. Website URLs:
    - Domains: `localhost` (for dev), `yourdomain.com` (for production)
    - Return URLs: `http://localhost:8080/login/oauth2/code/apple`
11. Click "Next" → "Done" → "Continue" → "Save"

### 4. Create Private Key

1. Go to "Keys" in the left menu
2. Click "+" to register a new key
3. Key Name: `WordAI Sign in with Apple Key`
4. Enable "Sign in with Apple"
5. Click "Configure" next to Sign in with Apple
6. Primary App ID: Select your App ID
7. Click "Save" → "Continue" → "Register"
8. Download the `.p8` private key file (you can only download it once!)
9. Note the **Key ID** shown on the page

### 5. Generate Client Secret

Apple OAuth requires a JWT client secret generated from your private key. This is more complex than Google OAuth.

You can use this helper script (save as `generate_apple_secret.sh`):

```bash
#!/bin/bash

KEY_ID="YOUR_KEY_ID"
TEAM_ID="YOUR_TEAM_ID"  # Found in top-right of developer portal
CLIENT_ID="com.fistraltech.wordai.web"  # Your Services ID
KEY_FILE="AuthKey_${KEY_ID}.p8"  # Your downloaded key

# Generate JWT header
HEADER=$(echo -n '{"alg":"ES256","kid":"'$KEY_ID'"}' | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Generate JWT payload
NOW=$(date +%s)
EXP=$((NOW + 15777000))  # 6 months
PAYLOAD=$(echo -n '{"iss":"'$TEAM_ID'","iat":'$NOW',"exp":'$EXP',"aud":"https://appleid.apple.com","sub":"'$CLIENT_ID'"}' | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Sign
UNSIGNED_TOKEN="${HEADER}.${PAYLOAD}"
SIGNATURE=$(echo -n "$UNSIGNED_TOKEN" | openssl dgst -sha256 -sign "$KEY_FILE" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# Final JWT
CLIENT_SECRET="${HEADER}.${PAYLOAD}.${SIGNATURE}"
echo "Client Secret:"
echo "$CLIENT_SECRET"
```

Run the script and copy the generated client secret.

### 6. Update application.properties

Replace the placeholders in `src/main/resources/application.properties`:

```properties
spring.security.oauth2.client.registration.apple.client-id=com.fistraltech.wordai.web
spring.security.oauth2.client.registration.apple.client-secret=YOUR_GENERATED_JWT_SECRET
```

**Note:** The client secret JWT expires after 6 months, so you'll need to regenerate it periodically.

## Local Development Setup

For local development without OAuth:

1. Comment out the OAuth configurations in `application.properties`:
```properties
# spring.security.oauth2.client.registration.google.client-id=...
# spring.security.oauth2.client.registration.apple.client-id=...
```

2. Use username/password registration only
3. Create a test account at http://localhost:8080/login

## Production Deployment

For production:

1. Use HTTPS (required for OAuth)
2. Update redirect URIs in Google and Apple consoles
3. Use environment variables for secrets:
```bash
export GOOGLE_CLIENT_ID=your_client_id
export GOOGLE_CLIENT_SECRET=your_client_secret
export APPLE_CLIENT_ID=your_client_id
export APPLE_CLIENT_SECRET=your_client_secret
```

4. Update `application.properties` to use environment variables:
```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.apple.client-id=${APPLE_CLIENT_ID}
spring.security.oauth2.client.registration.apple.client-secret=${APPLE_CLIENT_SECRET}
```

5. Consider using PostgreSQL instead of H2:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wordai
spring.datasource.username=wordai_user
spring.datasource.password=secure_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

## Testing Authentication

1. Start the application: `mvn spring-boot:run`
2. Navigate to http://localhost:8080
3. You should be redirected to http://localhost:8080/login
4. Try each authentication method:
   - Register with username/password
   - Sign in with Google (if configured)
   - Sign in with Apple (if configured)
5. After successful login, you'll be redirected to the main application
6. Your name should appear in the top-right corner
7. Click "Logout" to sign out

## Security Best Practices

1. **Never commit secrets to git** - Use `.gitignore` for `application.properties` or use environment variables
2. **Use HTTPS in production** - OAuth requires secure connections
3. **Rotate secrets regularly** - Especially the Apple client secret (every 6 months)
4. **Limit OAuth scopes** - Only request email and profile information
5. **Monitor failed login attempts** - Implement rate limiting if needed

## Troubleshooting

### "redirect_uri_mismatch" error (Google)
- Ensure the redirect URI in Google Console exactly matches `http://localhost:8080/login/oauth2/code/google`
- Include the protocol (`http://` or `https://`)

### "invalid_client" error (Apple)
- Check that your client secret JWT hasn't expired (6-month validity)
- Verify the Services ID matches the client-id in application.properties
- Ensure the redirect URI is correctly configured in Apple Developer Portal

### Database locked error
- Stop any running instances of the application
- Delete `./data/wordai.lock.db` file if it exists

### User can't log in after registration
- Check the H2 console to verify the user was created
- Ensure password is at least 6 characters
- Check application logs for error messages

## Support

For issues or questions:
1. Check the application logs: `target/wordai.log`
2. Review Spring Security documentation: https://spring.io/guides/gs/securing-web/
3. Review OAuth2 documentation: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
