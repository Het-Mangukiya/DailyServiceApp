#!/bin/bash

echo "=== Firebase/Firestore Connectivity Test ==="
echo ""

# Project details
PROJECT_ID="sgp-1-53142"
API_KEY="AIzaSyAoklRbDNcplP_VNqGU0MNfcpVj3WrZadQ"
TEST_EMAIL="test@gmail.com"
TEST_PASSWORD="Het@1234"

echo "📱 Project: $PROJECT_ID"
echo "📧 Test Email: $TEST_EMAIL"
echo ""

# Test 1: Firebase Auth - Sign in
echo "Test 1: Attempting Firebase Authentication..."
AUTH_RESPONSE=$(curl -s -X POST \
  "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$API_KEY" \
  -H 'Content-Type: application/json' \
  -d "{
    \"email\":\"$TEST_EMAIL\",
    \"password\":\"$TEST_PASSWORD\",
    \"returnSecureToken\":true
  }")

if echo "$AUTH_RESPONSE" | grep -q "idToken"; then
    echo "✅ Authentication successful!"
    
    # Extract user ID
    USER_ID=$(echo "$AUTH_RESPONSE" | grep -o '"localId":"[^"]*"' | cut -d'"' -f4)
    ID_TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"idToken":"[^"]*"' | cut -d'"' -f4)
    
    echo "   User ID: $USER_ID"
    echo ""
    
    # Test 2: Check Firestore document
    echo "Test 2: Checking Firestore user document..."
    FIRESTORE_URL="https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/users/$USER_ID"
    
    FIRESTORE_RESPONSE=$(curl -s -X GET \
      "$FIRESTORE_URL" \
      -H "Authorization: Bearer $ID_TOKEN")
    
    if echo "$FIRESTORE_RESPONSE" | grep -q "\"name\":"; then
        echo "✅ Firestore document exists!"
        echo ""
        echo "Document data:"
        echo "$FIRESTORE_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$FIRESTORE_RESPONSE"
    elif echo "$FIRESTORE_RESPONSE" | grep -q "NOT_FOUND"; then
        echo "⚠️  Firestore document NOT FOUND!"
        echo "   This is the bug - Firebase Auth has user but Firestore doesn't have the document."
        echo ""
        echo "   Creating document now..."
        
        # Create the document
        CREATE_RESPONSE=$(curl -s -X PATCH \
          "$FIRESTORE_URL?updateMask.fieldPaths=name&updateMask.fieldPaths=email&updateMask.fieldPaths=role&updateMask.fieldPaths=phone" \
          -H "Authorization: Bearer $ID_TOKEN" \
          -H 'Content-Type: application/json' \
          -d "{
            \"fields\": {
              \"id\": {\"stringValue\": \"$USER_ID\"},
              \"name\": {\"stringValue\": \"het\"},
              \"email\": {\"stringValue\": \"$TEST_EMAIL\"},
              \"role\": {\"stringValue\": \"PROVIDER\"},
              \"phone\": {\"stringValue\": \"1234567890\"},
              \"createdAt\": {\"integerValue\": \"$(date +%s)000\"}
            }
          }")
        
        if echo "$CREATE_RESPONSE" | grep -q "\"name\":"; then
            echo "✅ Document created successfully!"
        else
            echo "❌ Failed to create document:"
            echo "$CREATE_RESPONSE"
        fi
    else
        echo "❌ Error accessing Firestore:"
        echo "$FIRESTORE_RESPONSE"
    fi
    
elif echo "$AUTH_RESPONSE" | grep -q "INVALID_PASSWORD"; then
    echo "❌ Invalid password!"
    echo "   The password 'Het@1234' is wrong for $TEST_EMAIL"
    
elif echo "$AUTH_RESPONSE" | grep -q "EMAIL_NOT_FOUND"; then
    echo "❌ Email not found!"
    echo "   No account exists for $TEST_EMAIL"
    echo "   Please sign up first using the app."
    
else
    echo "❌ Authentication failed:"
    echo "$AUTH_RESPONSE"
fi

echo ""
echo "=== Test Complete ==="
