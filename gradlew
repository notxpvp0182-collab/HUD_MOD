#!/bin/sh

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DEFAULT_JVM_OPTS=""

warn()  { echo "$*" >&2; }
die()   { echo; echo "$*"; echo; exit 1; }

cygwin=false; msys=false; darwin=false; nonstop=false
case "$(uname)" in
  CYGWIN*)      cygwin=true ;;
  Darwin*)      darwin=true ;;
  MSYS*|MINGW*) msys=true ;;
  NONSTOP*)     nonstop=true ;;
esac

app_path=$0
while [ -h "$app_path" ]; do
  ls=$(ls -ld "$app_path")
  link=${ls#*' -> '}
  case $link in
    /*) app_path=$link ;;
    *)  app_path=${app_path%"${app_path##*/}"}$link ;;
  esac
done
APP_HOME=$(cd "${app_path%"${app_path##*/}"}." && pwd -P) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  [ -x "$JAVACMD" ] || die "ERROR: JAVA_HOME is invalid: $JAVA_HOME"
else
  JAVACMD=java
  which java >/dev/null 2>&1 || die "ERROR: java not found in PATH."
fi

if ! "$cygwin" && ! "$darwin" && ! "$nonstop"; then
  MAX_FD=$(ulimit -H -n 2>/dev/null) && ulimit -n "$MAX_FD" 2>/dev/null
fi

exec "$JAVACMD" \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
