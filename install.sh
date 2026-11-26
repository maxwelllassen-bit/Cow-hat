#!/usr/bin/env bash
# Hatrix installer (macOS/Linux). Copies the jar into the right mods folder; does not touch
# anything else and never downloads anything itself.
set -euo pipefail

JAR_NAME="hatrix-1.0.0.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$(uname -s)" in
    Darwin) MODS_DIR="$HOME/Library/Application Support/minecraft/mods" ;;
    Linux)  MODS_DIR="$HOME/.minecraft/mods" ;;
    *)
        echo "Unrecognized OS for this script. On Windows, use install-windows.bat instead."
        exit 1
        ;;
esac

if [ ! -f "$SCRIPT_DIR/$JAR_NAME" ]; then
    echo "ERROR: Could not find $JAR_NAME next to this script."
    echo "Make sure install.sh and $JAR_NAME are in the same folder."
    exit 1
fi

mkdir -p "$MODS_DIR"
cp -f "$SCRIPT_DIR/$JAR_NAME" "$MODS_DIR/$JAR_NAME"

echo
echo "Installed to: $MODS_DIR/$JAR_NAME"
echo
echo "This script only copies the mod itself. You still need, separately:"
echo "  - Minecraft 26.3 with Fabric Loader 0.20.1+  (https://fabricmc.net/use/)"
echo "  - Fabric API 0.167.0+26.3, as its own jar in the same mods folder"
echo "  - Java 25"
echo
echo "Once those are in place, launch Minecraft with the \"fabric-loader\" profile."
