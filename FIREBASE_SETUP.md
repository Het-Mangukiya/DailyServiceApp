# Firebase Setup Guide

## Issue: CONFIGURATION_NOT_FOUND Error

The app is showing "An internal error has occurred. [CONFIGURATION_NOT_FOUND]" because Firebase Authentication is not properly configured in your Firebase project.

## Solution: Enable Firebase Authentication

### Step 1: Open Firebase Console
1. Go to https://console.firebase.google.com/
2. Select your project: **sgp-1-53142**

### Step 2: Enable Authentication
1. In the left sidebar, click **Build** → **Authentication**
2. Click **Get Started** button
3. Go to **Sign-in method** tab
4. Enable **Email/Password** provider:
   - Click on "Email/Password"
   - Toggle "Enable" to ON
   - Click "Save"

### Step 3: Enable Firestore Database
1. In the left sidebar, click **Build** → **Firestore Database**
2. Click **Create database**
3. Choose **Test mode** (for development)
4. Select your preferred location (asia-south1 recommended)
5. Click **Enable**

### Step 4: Download Updated google-services.json
1. In Firebase Console, click the gear icon → **Project settings**
2. Scroll down to **Your apps** section
3. Find your Android app: `com.dailyserviceapp`
4. Click **google-services.json** download button
5. Replace the file at: `app/google-services.json`

### Step 5: Rebuild and Test
```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Alternative: Test Without Firebase (Mock Mode)

If you want to test the app without Firebase setup, I can create a mock authentication mode that stores data locally using Room database instead of Firebase.

## Firebase Features Required

For the Daily Service App to work fully, enable these Firebase services:

1. ✅ **Authentication** - User login/signup
2. ✅ **Firestore** - Store customers, bills, payments, etc.
3. ⚠️ **Cloud Storage** - Store profile pictures, bill PDFs (optional)
4. ⚠️ **Cloud Messaging (FCM)** - Push notifications (optional)
5. ⚠️ **Crashlytics** - Error reporting (optional)

## Current Configuration

Your current `google-services.json` includes:
- Project ID: sgp-1-53142
- Package: com.dailyserviceapp
- API Key: [Your Firebase API Key]

But it's missing:
- Authentication configuration
- Firestore configuration

## Quick Fix Command

After enabling Authentication and Firestore in Firebase Console and downloading the new google-services.json:

```bash
# Replace the google-services.json file
cp ~/Downloads/google-services.json app/google-services.json

# Clean and rebuild
./gradlew clean assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing Signup

Once Firebase is configured:
1. Open the app
2. Click "Sign Up"
3. Fill in details:
   - Name: het mangukiya
   - Email: hetmangukiya@gmail.com
   - Phone: 1234567890
   - Password: password123
   - Role: Service Provider
4. Click "SIGN UP"
5. Should create user and navigate to login

## Need Help?

If you continue to face issues, share:
1. Screenshot of Firebase Console → Authentication page
2. Screenshot of Firebase Console → Firestore Database page
3. The error message from logcat: `adb logcat | grep SignupActivity`
