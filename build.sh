#!/bin/bash
# YouTube Frontend - Build script
# InnerTune API powered YouTube clone

echo "🎬 YouTube Frontend Builder"
echo "============================"

# Check if wrapper jar exists, download if not
if [ ! -s gradle/wrapper/gradle-wrapper.jar ]; then
    echo "📥 Downloading gradle-wrapper.jar..."
    mkdir -p gradle/wrapper
    curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar
    if [ ! -s gradle/wrapper/gradle-wrapper.jar ]; then
        echo "❌ Failed to download wrapper jar, trying with gradle directly"
        if command -v gradle >/dev/null 2>&1; then
            gradle assembleDebug
            exit 0
        else
            echo "❌ No gradle found. Install gradle 8.4+ or run via GitHub Actions"
            exit 1
        fi
    fi
fi

echo "🔨 Building Debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "📦 APK location: app/build/outputs/apk/debug/app-debug.apk"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo "❌ Build failed"
    exit 1
fi

echo ""
echo "🚀 To build release APK:"
echo "   ./gradlew assembleRelease"
