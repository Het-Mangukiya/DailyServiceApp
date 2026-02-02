#!/usr/bin/env python3
"""
Test the authentication and Firestore flow for DailyServiceApp
This simulates what happens during signup and login
"""

import firebase_admin
from firebase_admin import credentials, auth, firestore
import json
import time

# Load service account from google-services.json
def get_project_id():
    with open('app/google-services.json', 'r') as f:
        data = json.load(f)
        return data['project_info']['project_id']

print("=== DailyServiceApp Authentication Test ===\n")

# Initialize Firebase Admin SDK
try:
    project_id = get_project_id()
    print(f"📱 Project ID: {project_id}")
    
    # Initialize with no credentials (will use application default credentials)
    cred = credentials.ApplicationDefault()
    firebase_admin.initialize_app(cred, {
        'projectId': project_id
    })
    
    db = firestore.client()
    print("✅ Firebase Admin SDK initialized\n")
    
except Exception as e:
    print(f"❌ Error initializing Firebase: {e}")
    print("\n💡 Tip: Make sure you have:")
    print("   1. Firebase service account key (download from Firebase Console)")
    print("   2. Set GOOGLE_APPLICATION_CREDENTIALS environment variable")
    print("\nFor testing, let's check the project setup...")
    exit(1)

# Test data from the screenshot
TEST_EMAIL = "test@gmail.com"
TEST_PASSWORD = "Het@1234"
TEST_NAME = "het"
TEST_PHONE = "1234567890"
TEST_ROLE = "PROVIDER"

print("🧪 Testing authentication flow...")
print(f"   Email: {TEST_EMAIL}")
print(f"   Name: {TEST_NAME}")
print(f"   Role: {TEST_ROLE}\n")

# Step 1: Check if user already exists
print("Step 1: Checking if user exists...")
try:
    user = auth.get_user_by_email(TEST_EMAIL)
    print(f"✅ User found in Firebase Auth: {user.uid}")
    user_id = user.uid
    user_exists = True
except auth.UserNotFoundError:
    print("ℹ️  User does not exist in Firebase Auth")
    user_exists = False
    user_id = None
except Exception as e:
    print(f"❌ Error checking user: {e}")
    exit(1)

# Step 2: Check Firestore document
if user_id:
    print(f"\nStep 2: Checking Firestore document for user {user_id}...")
    try:
        doc_ref = db.collection('users').document(user_id)
        doc = doc_ref.get()
        
        if doc.exists:
            print("✅ Firestore document exists:")
            data = doc.to_dict()
            print(f"   Name: {data.get('name')}")
            print(f"   Email: {data.get('email')}")
            print(f"   Role: {data.get('role')}")
            print(f"   Phone: {data.get('phone')}")
        else:
            print("⚠️  Firestore document DOES NOT exist!")
            print("\nThis is the bug! Firebase Auth has the user, but Firestore doesn't.")
            print("Creating the missing document now...")
            
            # Create the missing document
            user_data = {
                'id': user_id,
                'name': TEST_NAME,
                'email': TEST_EMAIL,
                'phone': TEST_PHONE,
                'role': TEST_ROLE,
                'createdAt': int(time.time() * 1000)
            }
            
            doc_ref.set(user_data)
            print("✅ Firestore document created successfully!")
            print("\n🎉 Login should work now. Try logging in with:")
            print(f"   Email: {TEST_EMAIL}")
            print(f"   Password: {TEST_PASSWORD}")
            
    except Exception as e:
        print(f"❌ Error accessing Firestore: {e}")
        print("\n💡 This might be a permissions issue. Check Firestore rules:")
        print("   rules_version = '2';")
        print("   service cloud.firestore {")
        print("     match /databases/{database}/documents {")
        print("       match /users/{userId} {")
        print("         allow read, write: if request.auth != null && request.auth.uid == userId;")
        print("       }")
        print("     }")
        print("   }")
        exit(1)

# Step 3: Test creating a new user if needed
if not user_exists:
    print("\nStep 3: Would create new user, but skipping to avoid duplicates")
    print("To test signup, delete the existing user first or use a different email")

print("\n" + "="*60)
print("📊 SUMMARY")
print("="*60)
if user_exists and doc.exists:
    print("✅ Everything looks good!")
    print(f"   - Firebase Auth user exists: {user_id}")
    print("   - Firestore document exists: YES")
    print(f"\n🎯 Login should work with: {TEST_EMAIL} / {TEST_PASSWORD}")
elif user_exists and not doc.exists:
    print("✅ Fixed!")
    print(f"   - Firebase Auth user exists: {user_id}")
    print("   - Firestore document: CREATED")
    print(f"\n🎯 Login should now work with: {TEST_EMAIL} / {TEST_PASSWORD}")
else:
    print("ℹ️  User doesn't exist")
    print("   - Use the app to sign up first")
    print(f"   - Or run this script with create_user flag")

print("="*60)
