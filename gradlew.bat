@rem Gradle wrapper launcher for Windows
@echo off
setlocal
set DIR=%~dp0
set CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar

if exist "%CLASSPATH%" (
  java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)

where gradle >nul 2>&1
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo Gradle wrapper JAR is missing and no system Gradle installation was found.
exit /b 1
