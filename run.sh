#!/usr/bin/env bash
set -euo pipefail

# === run.sh — сборка и запуск KnightMove ===
# Используется Maven Wrapper (mvnw) + прямой вызов плагина по groupId:artifactId:version
# Не зависит от Maven-прокси и pluginGroups — всегда работает.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔨 Сборка проекта..."
./mvnw clean compile -q

echo "🐎 Запуск KnightMove..."
./mvnw org.openjfx:javafx-maven-plugin:0.0.8:run
