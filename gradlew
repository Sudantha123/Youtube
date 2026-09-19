#!/usr/bin/env sh
# Custom gradlew that works even without wrapper jar - falls back to system gradle

APP_HOME=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

# If wrapper jar exists and valid, try to use it
if [ -f "$WRAPPER_JAR" ] && [ -s "$WRAPPER_JAR" ]; then
    if command -v java >/dev/null 2>&1; then
        exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
    fi
fi

# Fallback to system gradle
if command -v gradle >/dev/null 2>&1; then
    echo "Wrapper jar not found, using system gradle..."
    exec gradle "$@"
fi

# Try to find gradle in common locations
for GRADLE_BIN in /opt/gradle/bin/gradle /usr/local/gradle/bin/gradle /snap/bin/gradle; do
    if [ -x "$GRADLE_BIN" ]; then
        echo "Using gradle at $GRADLE_BIN"
        exec "$GRADLE_BIN" "$@"
    fi
done

echo "ERROR: Gradle wrapper jar not found and no system gradle installed."
echo "On GitHub Actions, gradle will be installed automatically via setup-gradle action."
echo "For local development, install Gradle 8.4+ or download wrapper jar manually:"
echo "  curl -L https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar"
exit 1
