# GitHub Setup Instructions for WordAI

This guide will walk you through publishing your WordAI project to GitHub.

## Step 1: Create a GitHub Repository

1. Go to https://github.com
2. Log in to your account
3. Click the "+" icon in the top right corner
4. Select "New repository"
5. Fill in the details:
   - **Repository name**: `WordAI` (or your preferred name)
   - **Description**: "Intelligent word-guessing game with advanced bot strategies and analytics"
   - **Visibility**: Choose Public or Private
   - **DO NOT** initialize with README, .gitignore, or license (we already have these)
6. Click "Create repository"

## Step 2: Initialize Local Git Repository

Open PowerShell in your WordAI project directory and run these commands:

```powershell
# Navigate to your project directory (if not already there)
cd C:\Users\johnm\OneDrive\Projects\WordAI

# Initialize git repository
git init

# Add all files to staging
git add .

# Create your first commit
git commit -m "Initial commit: WordAI v1.0 - Complete game engine, bot system, and web interface"
```

## Step 3: Connect to GitHub and Push

After creating your repository on GitHub, you'll see a page with instructions. Use these commands:

```powershell
# Add the remote repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/WordAI.git

# Rename the default branch to 'main' (if needed)
git branch -M main

# Push your code to GitHub
git push -u origin main
```

## Step 4: Verify Your Upload

1. Go to your repository page: `https://github.com/YOUR_USERNAME/WordAI`
2. You should see all your files listed
3. The README.md will be displayed automatically on the repository homepage

## Step 5: Update README.md with Your Username

After pushing, update the README.md file to replace placeholders:

1. Edit the README.md file on GitHub or locally
2. Replace `YOUR_USERNAME` with your actual GitHub username in:
   - Clone URL
   - Badge URLs
   - Issues link

Then commit and push the changes:

```powershell
git add README.md
git commit -m "Update README with GitHub username"
git push
```

## Alternative: Using GitHub Desktop

If you prefer a GUI:

1. Download and install GitHub Desktop: https://desktop.github.com/
2. Open GitHub Desktop
3. Click "File" → "Add local repository"
4. Browse to: `C:\Users\johnm\OneDrive\Projects\WordAI`
5. Click "Add repository"
6. Click "Publish repository" button
7. Choose repository name and visibility
8. Click "Publish repository"

## Troubleshooting

### Authentication Issues

If you encounter authentication errors:

1. **Use a Personal Access Token (PAT)**:
   - Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Click "Generate new token (classic)"
   - Select scopes: `repo` (full control)
   - Copy the token
   - When prompted for password, paste the token instead

2. **Or use GitHub CLI**:
   ```powershell
   # Install GitHub CLI
   winget install --id GitHub.cli
   
   # Authenticate
   gh auth login
   
   # Create and push repository
   gh repo create WordAI --public --source=. --push
   ```

### Large File Issues

If you get errors about large files:

```powershell
# Check which files are large
git ls-files -s | awk '$4 > 50000000' 

# If needed, add them to .gitignore
echo "large-file.csv" >> .gitignore
git rm --cached large-file.csv
git commit -m "Remove large file from tracking"
```

## What's Been Prepared

✅ `.gitignore` - Excludes build artifacts, IDE files, and sensitive data
✅ `README.md` - Professional project documentation
✅ `LICENSE` - MIT License for open source
✅ `DOCUMENTATION_STATUS.md` - Documentation tracking
✅ All source code properly documented

## Next Steps After Publishing

1. **Add Topics** on GitHub to make your repo discoverable:
   - `wordle`, `java`, `spring-boot`, `game`, `ai`, `bot`, `algorithm`

2. **Enable GitHub Pages** (if you want to host the game):
   - Go to repository Settings → Pages
   - Select source branch
   - Your game will be available at `https://YOUR_USERNAME.github.io/WordAI`

3. **Set up GitHub Actions** for automated testing:
   - Create `.github/workflows/maven.yml` for CI/CD

4. **Create Issues** for future enhancements:
   - Additional selection algorithms
   - Multiplayer support
   - Mobile app version

## Files Ready for GitHub

```
WordAI/
├── .gitignore              ✅ Created (excludes build artifacts)
├── README.md               ✅ Created (comprehensive documentation)
├── LICENSE                 ✅ Created (MIT License)
├── DOCUMENTATION_STATUS.md ✅ Exists (documentation tracking)
├── pom.xml                 ✅ Exists (Maven configuration)
├── src/                    ✅ Exists (all source code)
└── All other files         ✅ Ready to commit
```

## Summary Commands (Copy-Paste Ready)

```powershell
# Step 1: Initialize and commit
git init
git add .
git commit -m "Initial commit: WordAI v1.0 - Complete game engine, bot system, and web interface"

# Step 2: Connect to GitHub (replace YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/WordAI.git
git branch -M main

# Step 3: Push to GitHub
git push -u origin main
```

## Need Help?

- GitHub Guides: https://guides.github.com/
- Git Documentation: https://git-scm.com/doc
- GitHub Support: https://support.github.com/

---

**Ready to publish your WordAI project to the world! 🚀**
