#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="${BUILD_DIR:-build/classes}"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java' | sort)
java -cp "$BUILD_DIR" lab.RequestProcessorTest
