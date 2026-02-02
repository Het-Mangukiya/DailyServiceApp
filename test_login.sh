#!/bin/bash

echo "=== Testing Login Flow ==="
echo "Screen: 1080x2400"

# Make sure app is in foreground
adb shell am start -n com.dailyserviceapp/.auth.LoginActivity
sleep 3

# Click email field (center of screen, upper third)
echo "Entering email..."
adb shell input tap 540 600
sleep 1
adb shell input text "test@gmail.com"
sleep 1

# Click password field 
echo "Entering password..."
adb shell input tap 540 900
sleep 1
adb shell input text "Het@1234"
sleep 1

# Hide keyboard
adb shell input keyevent KEYCODE_BACK
sleep 1

# Click login button (bottom of screen)
echo "Clicking Login..."
adb shell input tap 540 1400
sleep 3

echo "Login test completed. Check the logs..."
