#!/bin/sh

##############################################################################
# Gradle start up script for POSIX
##############################################################################

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -r "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR not found: $CLASSPATH" >&2
  exit 1
fi

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
