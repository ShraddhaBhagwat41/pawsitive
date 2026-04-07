# Pawsitive Admin

This project consists of an Android application and a Node.js backend for managing a pet rescue and adoption platform.

## ⚠️ Security Warning

**Important:** The `serviceAccountKey.json` file contains sensitive Firebase credentials. **Do not push this file to any public repository.** 

It has been added to `.gitignore` to prevent accidental commits.

### Setting up the Backend
If you are cloning this repository, you will need to:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Navigate to **Project Settings** > **Service accounts**.
3. Click **Generate new private key**.
4. Save the downloaded JSON file as `serviceAccountKey.json` inside the `backend/` directory.

## How to Push to GitHub

Follow these steps to push your code while keeping your secrets safe:

1. **Initialize Git** (if not already done):
   ```bash
   git init
   ```

2. **Add Remote Repository**:
   ```bash
   git remote add origin <your-repository-url>
   ```

3. **Check Ignored Files**:
   Run the following command to ensure `serviceAccountKey.json` is NOT being tracked:
   ```bash
   git status
   ```
   If you see `serviceAccountKey.json` listed under "Untracked files", ensure your `.gitignore` is correctly configured.

4. **Stage and Commit**:
   ```bash
   git add .
   git commit -m "Initial commit with NGO Registration UI and security fixes"
   ```

5. **Push to Main Branch**:
   ```bash
   git branch -M main
   git push -u origin main
   ```

## Project Structure
- `/app`: Android Studio project (Java).
- `/backend`: Node.js server using Firebase Admin SDK.
- `.gitignore`: Configured to exclude sensitive keys and build files.
