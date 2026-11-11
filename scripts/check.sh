#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./gradlew writeDependencyInventory --console=plain
python3 scripts/generate_notices.py
./gradlew testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease --console=plain
