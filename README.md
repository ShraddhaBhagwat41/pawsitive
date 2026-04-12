# Pawsitive - Animal Rescue & Management System

Pawsitive is a platform designed to connect NGOs, volunteers, and users to help animals in need.

## 🚀 Getting Started

Follow these steps to set up and run the project on your local machine.

### 1. Firebase Service Account Setup
Before running the backend, you must obtain and add the Firebase Service Account key.

**How to get the key:**
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Select your project (**pawsitive-40f63**).
3.  Click on the **Settings (gear icon)** next to "Project Overview" and select **Project settings**.
4.  Navigate to the **Service accounts** tab.
5.  Click the **Generate new private key** button at the bottom.
6.  A JSON file will be downloaded to your computer.

**How to add it to the project:**
1.  Rename the downloaded file to exactly `serviceAccountKey.json`.
2.  Move this file into the backend directory at:
    `C:\Users\Sejal Pc\Desktop\Pawsitivefinal\pawsitive\pawsitive-admin\backend\`

### 2. Backend Setup
The backend is a Node.js server that handles authentication and data management.

**Prerequisites:**
- Ensure you have `node` and `npm` installed.
- Ensure `serviceAccountKey.json` is in the `backend` folder.

**Run the Backend:**
Open a terminal and run:
```bash
cd "C:\Users\Sejal Pc\Desktop\Pawsitivefinal\pawsitive\pawsitive-admin\backend" ; node server.js
```

### 3. Android App Setup
The main application for users and NGOs.

**Prerequisites:**
- Android SDK and ADB installed.
- Physical device or Emulator connected.

**1st Step (Debug Command):**
Open another terminal and run:
```bash
cd "C:\Users\Sejal Pc\Desktop\Pawsitivefinal\pawsitive" ; .\gradlew.bat :app:installDebug
```

**2nd Step (Launch Command):**
Run the following command to start the app:
```bash
"C:\Users\Sejal Pc\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.pawsitive.app/com.pawsitive.app.WelcomeActivity
```

## 🛠 Project Structure
- `app/`: Main Android application module.
- `pawsitive-admin/backend/`: Node.js backend server.
- `pawsitive-admin/app/`: Admin-specific Android application.

## 🔑 Firebase Configuration
- **Firestore:** Stores user profiles, NGO details, and pet reports.
- **Authentication:** Manages login for Users, NGOs, and Admins.
- **Storage:** Stores profile photos and NGO certificates.

## 📝 NGO Registration Flow
1. User selects "NGO" role during signup.
2. Fills in organization details and uploads certificates.
3. Upon clicking "Sign Up", data is sent to the backend for verification.
4. User is redirected to the login page with a "Sent for Verification" status.
