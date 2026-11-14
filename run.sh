#!/usr/bin/env bash
# ==============================================================================
# SQL Injection Sandbox & Visualizer - Launch Script
# ==============================================================================
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "======================================================================"
echo "⚡ Starting SQL Injection Sandbox & Visualizer (Java Swing Desktop)"
echo "======================================================================"

JAR_FILE="target/sqli-sandbox-1.0.0-jar-with-dependencies.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "[INFO] Application JAR not found. Building with Maven Wrapper..."
    ./mvnw clean package -DskipTests
fi

echo "[INFO] Launching Application..."
java -jar "$JAR_FILE"
