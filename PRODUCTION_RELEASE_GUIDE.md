# Production Release Guide

This guide walks you through generating a production-ready signed APK/AAB for Play Store release.

---

## Prerequisites

1. **Android Studio** installed
2. **Java keystore** for signing (or create new one)
3. **Play Console account** (for AAB upload)
4. **App reviewed and tested**

---

## Step 1: Generate Signing Key

If you don't have a signing key, create one:

```bash
keytool -genkey -v -keystore daily-service-release.jks \
  -alias daily-service-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Important:**
- Store the keystore file securely (never commit to Git)
- Remember the keystore password and key password
- Backup the keystore - you cannot update your app without it

---

## Step 2: Configure Signing in build.gradle

Add signing configuration to `app/build.gradle`:

```gradle
android {
    ...
    
    signingConfigs {
        release {
            storeFile file("path/to/daily-service-release.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD") ?: "your-password"
            keyAlias "daily-service-key"
            keyPassword System.getenv("KEY_PASSWORD") ?: "your-key-password"
        }
    }
    
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
    }
}
```

**Security Best Practice:**
Store passwords in environment variables, not in code:

```bash
export KEYSTORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"
```

---

## Step 3: Update Version Information

In `app/build.gradle`, update version:

```gradle
android {
    defaultConfig {
        versionCode 1        // Increment for each release
        versionName "1.0.0"  // User-visible version
    }
}
```

**Version Guidelines:**
- **versionCode:** Integer that MUST increase with each release
- **versionName:** Semantic versioning (MAJOR.MINOR.PATCH)

---

## Step 4: Clean Build

```bash
cd /Users/het/AndroidStudioProjects/DailyServiceApp

# Clean previous builds
./gradlew clean

# Verify build configuration
./gradlew tasks
```

---

## Step 5: Generate Release APK

### Option A: APK (for direct distribution)

```bash
./gradlew assembleRelease
```

Output location:
```
app/build/outputs/apk/release/app-release.apk
```

### Option B: AAB (for Play Store - RECOMMENDED)

```bash
./gradlew bundleRelease
```

Output location:
```
app/build/outputs/bundle/release/app-release.aab
```

**Why AAB?**
- Smaller download size for users
- Google Play optimization
- Required for new apps on Play Store

---

## Step 6: Verify Build

### Check APK/AAB integrity

```bash
# For APK
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# For AAB
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Should show: `jar verified.`

### Analyze APK size

```bash
# Using bundletool
java -jar bundletool.jar get-size total --apks=app.apks

# Or in Android Studio
Build > Analyze APK > Select app-release.apk
```

---

## Step 7: Test Release Build

### Install release APK on device

```bash
# Uninstall debug version first
adb uninstall com.dailyserviceapp

# Install release version
adb install app/build/outputs/apk/release/app-release.apk

# Launch and test
adb shell am start -n com.dailyserviceapp/.auth.SplashActivity
```

### Test Checklist
- [ ] App launches without crashes
- [ ] All features work correctly
- [ ] Firebase connection successful
- [ ] Offline mode functions properly
- [ ] ProGuard hasn't broken anything
- [ ] Performance is acceptable

---

## Step 8: Prepare for Play Store

### Required Assets

1. **App Icon** (512x512 PNG)
2. **Feature Graphic** (1024x500 PNG)
3. **Screenshots** (minimum 2, up to 8):
   - Phone: 16:9 or 9:16 ratio
   - Tablet: 16:9 or 9:16 ratio (optional)
4. **Promo Video** (optional, YouTube link)

### Store Listing Content

**App Title:** Daily Service App (max 50 characters)

**Short Description:** (max 80 characters)
```
Manage daily deliveries, track customers, and generate bills easily
```

**Full Description:** (max 4000 characters)
```
Daily Service App is the ultimate solution for service providers managing 
daily deliveries like milk, newspapers, water, and more.

✨ KEY FEATURES:

📱 Customer Management
• Organize all customers in one place
• Quick search and smart filtering
• Sort by name, service, or area
• Vacation mode for temporary pauses

🚚 Delivery Tracking
• Mark deliveries daily with one tap
• Adjust quantities on the fly
• Route planning with area grouping
• View delivery history

💰 Billing Made Easy
• Auto-generate monthly bills
• Track payment status
• Send bills via WhatsApp/Email
• PDF export for records

⚡ Works Offline
• Full offline functionality
• Auto-sync when connected
• Never miss a delivery entry
• Reliable local data cache

📊 Analytics Dashboard
• Today's delivery summary
• Monthly revenue tracking
• Customer statistics
• Real-time insights

🎨 Modern Design
• Material Design 3
• Intuitive interface
• Dark mode support
• Smooth animations

Perfect for:
• Milk delivery services
• Newspaper distributors
• Water suppliers
• Tiffin services
• Any daily subscription business

Download now and simplify your daily service management!

🔒 Secure & Private
• Your data is completely secure
• Firebase-powered backend
• No data sharing with third parties
```

### Category & Tags

- **Category:** Business / Productivity
- **Tags:** delivery, service, billing, customer management, offline, business

### Content Rating

Complete Google Play's content rating questionnaire:
- Violence: None
- Sexual Content: None
- User Interaction: None
- Data Collection: Email for authentication

### Privacy Policy

Required for Play Store. Host at: `https://yourwebsite.com/privacy-policy`

Sample structure:
1. Information Collection
2. How We Use Information
3. Data Security
4. Third-party Services (Firebase)
5. User Rights
6. Contact Information

---

## Step 9: Upload to Play Console

### Initial Upload (Internal Testing)

1. Go to [Google Play Console](https://play.google.com/console)
2. Create new app or select existing
3. Navigate to: **Testing > Internal testing**
4. Click **Create new release**
5. Upload `app-release.aab`
6. Add release notes (from RELEASE_NOTES.md)
7. Save and review
8. Submit for internal testing

### Add Internal Testers

1. Go to **Testing > Internal testing > Testers**
2. Create email list of testers
3. Share opt-in URL with testers
4. Collect feedback

### Promote to Production

After testing:

1. Go to **Testing > Internal testing**
2. Select release
3. Click **Promote release**
4. Choose **Production**
5. Fill required sections:
   - Countries/regions
   - Content rating
   - App content
   - Pricing & distribution
6. Review and rollout

---

## Step 10: Post-Release Monitoring

### Enable Firebase Crashlytics

Add to `app/build.gradle`:
```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-crashlytics'
    implementation 'com.google.firebase:firebase-analytics'
}
```

### Monitor Key Metrics

1. **Crash-free users rate** (target: >99%)
2. **ANR rate** (target: <0.5%)
3. **Installation success rate**
4. **User ratings** (target: >4.0)
5. **Daily Active Users (DAU)**

### Respond to Reviews

- Respond within 24-48 hours
- Address negative reviews professionally
- Thank users for positive feedback
- Fix reported bugs in updates

---

## Common Issues & Solutions

### Issue: ProGuard breaks Firebase

**Solution:** Add to `proguard-rules.pro`:
```proguard
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keep class com.dailyserviceapp.models.** { *; }
```

### Issue: APK too large

**Solutions:**
1. Use AAB instead of APK
2. Enable resource shrinking
3. Optimize images (WebP format)
4. Remove unused dependencies

### Issue: Signing fails

**Check:**
1. Keystore file path is correct
2. Passwords are correct
3. Key alias matches
4. Keystore hasn't expired

---

## Release Checklist

Before uploading to Play Store:

- [ ] Version code incremented
- [ ] Version name updated
- [ ] Signed with release keystore
- [ ] Tested on physical device
- [ ] All features working
- [ ] ProGuard tested
- [ ] Firebase connected
- [ ] Crashlytics enabled
- [ ] Analytics added
- [ ] Screenshots prepared
- [ ] Store listing complete
- [ ] Privacy policy published
- [ ] Content rating completed
- [ ] Backup keystore securely

---

## Quick Commands Reference

```bash
# Clean build
./gradlew clean

# Generate debug APK
./gradlew assembleDebug

# Generate release APK
./gradlew assembleRelease

# Generate release AAB (Play Store)
./gradlew bundleRelease

# Check app size
./gradlew :app:printApkSize

# Run all tests
./gradlew test

# Install release on device
adb install app/build/outputs/apk/release/app-release.apk

# Verify signature
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk
```

---

## Support

For release-related issues:
- Check [Android Developer Documentation](https://developer.android.com/studio/publish)
- Visit [Play Console Help](https://support.google.com/googleplay/android-developer)
- Contact: dev@dailyserviceapp.com

---

**Good luck with your release! 🚀**
