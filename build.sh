#!/bin/bash

# App Automation Build Script

echo "======================================"
echo "App Automation - Build Script"
echo "======================================"
echo ""

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "❌ Error: gradlew not found!"
    echo "Please run this script from the project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

echo "Select build option:"
echo "1. Build Debug APK"
echo "2. Build Release APK"
echo "3. Install Debug on Device"
echo "4. Clean Build"
echo "5. Run Tests"
echo ""
read -p "Enter option (1-5): " option

case $option in
    1)
        echo ""
        echo "📦 Building Debug APK..."
        ./gradlew assembleDebug
        
        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ Debug APK built successfully!"
            echo "📍 Location: app/build/outputs/apk/debug/app-debug.apk"
        else
            echo ""
            echo "❌ Build failed!"
            exit 1
        fi
        ;;
    2)
        echo ""
        echo "📦 Building Release APK..."
        ./gradlew assembleRelease
        
        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ Release APK built successfully!"
            echo "📍 Location: app/build/outputs/apk/release/app-release.apk"
            echo "⚠️  Note: APK is not signed for production"
        else
            echo ""
            echo "❌ Build failed!"
            exit 1
        fi
        ;;
    3)
        echo ""
        echo "📱 Installing Debug APK on device..."
        ./gradlew installDebug
        
        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ App installed successfully!"
        else
            echo ""
            echo "❌ Installation failed!"
            echo "Make sure:"
            echo "  - Device is connected via USB"
            echo "  - USB debugging is enabled"
            echo "  - Device is authorized"
            exit 1
        fi
        ;;
    4)
        echo ""
        echo "🧹 Cleaning build..."
        ./gradlew clean
        
        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ Clean completed!"
        else
            echo ""
            echo "❌ Clean failed!"
            exit 1
        fi
        ;;
    5)
        echo ""
        echo "🧪 Running tests..."
        ./gradlew test
        
        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ All tests passed!"
        else
            echo ""
            echo "❌ Some tests failed!"
            exit 1
        fi
        ;;
    *)
        echo "❌ Invalid option!"
        exit 1
        ;;
esac

echo ""
echo "======================================"
echo "Done!"
echo "======================================"
