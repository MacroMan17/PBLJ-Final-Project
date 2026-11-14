@echo off
REM ==============================================================================
REM SQL Injection Sandbox & Visualizer - Windows Launch Script
REM ==============================================================================
setlocal

cd /d "%~dp0"

echo ======================================================================
echo ⚡ Starting SQL Injection Sandbox & Visualizer (Java Swing Desktop)
echo ======================================================================

set JAR_FILE=target\sqli-sandbox-1.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo [INFO] Application JAR not found. Building with Maven Wrapper...
    call mvnw.cmd clean package -DskipTests
)

echo [INFO] Launching Application...
java -jar "%JAR_FILE%"
