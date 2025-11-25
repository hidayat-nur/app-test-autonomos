#!/bin/bash

echo "🔍 Testing Random Interactions - Debug Script"
echo "=============================================="
echo ""

echo "1️⃣ Checking Accessibility Service Status..."
SERVICE_STATUS=$(adb shell settings get secure enabled_accessibility_services)
if [[ $SERVICE_STATUS == *"appautomation"* ]]; then
    echo "✅ Accessibility Service: ENABLED"
else
    echo "❌ Accessibility Service: NOT ENABLED"
    echo "   Please enable in: Settings → Accessibility → App Automation"
    exit 1
fi
echo ""

echo "2️⃣ Clearing old logs..."
adb logcat -c
echo "✅ Logs cleared"
echo ""

echo "3️⃣ Starting app..."
adb shell am start -n com.appautomation/.presentation.ui.MainActivity
sleep 2
echo "✅ App started"
echo ""

echo "4️⃣ Watching logs for random interactions..."
echo "   (Press Ctrl+C to stop)"
echo ""
echo "👀 EXPECTED OUTPUT:"
echo "   - '🎮 Starting random interactions (interval: 5s)'"
echo "   - '🔥 Performing FIRST gesture immediately...'"
echo "   - '👆 Gesture #1: Random TAP' or '👇 SCROLL' or '👉 SWIPE'"
echo "   - New gesture every 5 seconds"
echo ""
echo "📊 Logs:"
echo "---"

adb logcat | grep --color=always -E "AccessibilityService|AutomationManager|Gesture"
