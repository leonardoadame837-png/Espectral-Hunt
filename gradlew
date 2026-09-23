#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -r "$CLASSPATH" ]; then
  exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi

# Keep the repository command consistent when a trusted Gradle installation
# is available on the developer/CI machine.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Gradle wrapper JAR is missing and no system Gradle installation was found." >&2
exit 1
