#!/bin/bash

# Test signup flow
echo "Starting signup test..."

# Click on "Sign Up" button if we're on login screen
adb shell input tap 323 1130

sleep 2

# Fill in name field (tap and type)
adb shell input tap 323 320
adb shell input text "het"

sleep 1

# Fill in email field
adb shell input tap 323 453
adb shell input text "test@gmail.com"

sleep 1

# Fill in phone field
adb shell input tap 323 586
adb shell input text "1234567890"

sleep 1

# Fill in password field
adb shell input tap 323 719
adb shell input text "Het@1234"

sleep 1

# Fill in confirm password field
adb shell input tap 323 850
adb shell input text "Het@1234"

sleep 1

# Select role dropdown
adb shell input tap 323 1009

sleep 1

# Select "Service Provider" from dropdown
adb shell input tap 323 1050

sleep 1

# Click Sign Up button
adb shell input tap 323 1130

echo "Signup test completed. Check logs..."
