#!/usr/bin/env bash
#
# Asterium - a touch-first planetarium, forked from Stellarium.
# Copyright (C) 2026 the Asterium authors
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA  02110-1335, USA.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ABI="arm64-v8a"
CLEAN=0
WITH_ASSETS=1
BUNDLE=0
while [[ $# -gt 0 ]]; do
    case "$1" in
        --abi) ABI="$2"; shift 2 ;;
        --clean) CLEAN=1; shift ;;
        --no-assets) WITH_ASSETS=0; shift ;;
        --aab) BUNDLE=1; shift ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

VERSION_CODE="${ASTERIUM_VERSION_CODE:-$(git -C "$ROOT" rev-list --count HEAD 2>/dev/null || echo 1)}"

: "${ANDROID_SDK_ROOT:=$HOME/Library/Android/sdk}"
: "${ANDROID_NDK_VERSION:=27.2.12479018}"
: "${QT_VERSION:=6.10.1}"
: "${QT_ROOT:=$HOME/Qt/$QT_VERSION}"
: "${JAVA_HOME:=$HOME/Library/Java/JavaVirtualMachines/temurin-21.0.3/Contents/Home}"

case "$ABI" in
    arm64-v8a)   QT_TARGET_DIR="$QT_ROOT/android_arm64_v8a" ;;
    armeabi-v7a) QT_TARGET_DIR="$QT_ROOT/android_armv7" ;;
    x86_64)      QT_TARGET_DIR="$QT_ROOT/android_x86_64" ;;
    *) echo "unsupported ABI: $ABI" >&2; exit 2 ;;
esac

QT_HOST_DIR="$QT_ROOT/macos"
[[ -d "$QT_HOST_DIR" ]] || QT_HOST_DIR="$QT_ROOT/gcc_64"

export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT" JAVA_HOME
export ANDROID_NDK_ROOT="$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION"
export PATH="$JAVA_HOME/bin:$PATH"

BUILD_DIR="$ROOT/build-android"
STAGE_DIR="$ROOT/build-android-stage"
ASSETS_DIR="$ROOT/android/assets"
PACK_DIR="$ROOT/android/skydata/src/main/assets"
PACK_ASSETS=(nebulae skycultures)
BUILD_TOOLS="$(ls -d "$ANDROID_SDK_ROOT"/build-tools/* | sort -V | tail -1)"

for required in "$QT_TARGET_DIR/bin/qt-cmake" "$ANDROID_NDK_ROOT" "$BUILD_TOOLS" "$JAVA_HOME"; do
    [[ -e "$required" ]] || { echo "missing: $required" >&2; exit 1; }
done

READELF_CANDIDATES=("$ANDROID_NDK_ROOT"/toolchains/llvm/prebuilt/*/bin/llvm-readelf)
READELF="${READELF_CANDIDATES[0]}"
[[ -x "$READELF" ]] || { echo "missing llvm-readelf in $ANDROID_NDK_ROOT" >&2; exit 1; }

if [[ $CLEAN -eq 0 && -f "$BUILD_DIR/CMakeCache.txt" ]]; then
    for setting in "ANDROID_NDK=$ANDROID_NDK_ROOT" \
                   "Qt6_DIR=$QT_TARGET_DIR/lib/cmake/Qt6" \
                   "ANDROID_ABI=$ABI"; do
        cached="$(sed -n "s|^${setting%%=*}:[^=]*=||p" "$BUILD_DIR/CMakeCache.txt")"
        if [[ "$cached" != "${setting#*=}" ]]; then
            echo "Android toolchain changed; rerun with --clean." >&2
            exit 1
        fi
    done
fi

hard_rm() {
    local target="$1" attempt
    for attempt in 1 2 3; do
        rm -rf "$target" 2>/dev/null && return 0
        [[ -e "$target" ]] || return 0
        sleep 1
    done
    rm -rf "$target"
}

if [[ $CLEAN -eq 1 ]]; then
    hard_rm "$BUILD_DIR"; hard_rm "$STAGE_DIR"; hard_rm "$ASSETS_DIR"
fi

echo "==> configure ($ABI, Qt $QT_VERSION)"
"$QT_TARGET_DIR/bin/qt-cmake" -S "$ROOT" -B "$BUILD_DIR" -G Ninja \
    -DQT_HOST_PATH="$QT_HOST_DIR" \
    -DQt6LinguistTools_DIR="$QT_HOST_DIR/lib/cmake/Qt6LinguistTools" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_NDK="$ANDROID_NDK_ROOT" \
    -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
    -DASTERIUM_VERSION_CODE="$VERSION_CODE" \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_TESTING=0 \
    -DENABLE_QTWEBENGINE=0 \
    -DENABLE_SHOWMYSKY=0 \
    -DENABLE_SPEECH=0 \
    -DENABLE_MEDIA=0 \
    -DENABLE_CCACHE=0 \
    -DENABLE_PCH=0

echo "==> compile"
cmake --build "$BUILD_DIR" --target asterium translations \
      -j"$(sysctl -n hw.ncpu 2>/dev/null || nproc)"

if [[ $WITH_ASSETS -eq 1 ]]; then
    echo "==> stage data tree"
    hard_rm "$STAGE_DIR"
    cmake --install "$BUILD_DIR" --prefix "$STAGE_DIR" >/dev/null

    DATA_SRC="$STAGE_DIR/share/stellarium"
    [[ -f "$DATA_SRC/data/ssystem_major.ini" ]] || {
        echo "staged data tree looks wrong: no data/ssystem_major.ini" >&2; exit 1; }

    echo "==> sync assets"
    mkdir -p "$ASSETS_DIR"
    rsync -a --delete --delete-excluded --exclude 'territory.geojson' \
          --exclude '/scenery3d' --exclude '/webroot' \
          --exclude '/nebulae/*/catalog.txt' \
          --exclude '/data/stellarium.ico' --exclude '/data/stellarium-gray.ico' \
          "$DATA_SRC/" "$ASSETS_DIR/"
    echo "    assets: $(du -sh "$ASSETS_DIR" | cut -f1)"
fi

cp "$ROOT/CREDITS.md" "$ASSETS_DIR/CREDITS.md"

cp "$ROOT/COPYING" "$ROOT/COPYING.GPL3" "$ROOT/COPYING.LGPL3" \
   "$ROOT/COPYING.LGPL21" "$ROOT/COPYING.APACHE2" "$ASSETS_DIR/"

echo "==> sync gui icons"
mkdir -p "$ASSETS_DIR/gui"
cp "$ROOT"/data/gui/bbt*.png "$ROOT"/data/gui/bt*.png "$ASSETS_DIR/gui/"
cp "$ROOT"/data/gui/miscWorldMap.jpg "$ASSETS_DIR/gui/"
for glyph in ocular:Ocular crosshairs:Crosshairs binocular:Binocular \
             sensor:Sensor telrad:Telrad; do
    for state in on off; do
        cp "$ROOT/plugins/Oculars/resources/bt_${glyph%%:*}_${state}.png" \
           "$ASSETS_DIR/gui/bt${glyph##*:}-${state}.png"
    done
done

echo "==> extract interface strings"
python3 "$ROOT/util/extract-ui-strings.py" "$ROOT/android/java" "$ASSETS_DIR/gui/i18n.json"

[[ -f "$ASSETS_DIR/data/ssystem_major.ini" ]] || {
    echo "android/assets is missing data/ssystem_major.ini - run without --no-assets" >&2
    exit 1
}

hard_rm "$PACK_DIR"
if [[ $BUNDLE -eq 1 ]]; then
    echo "==> route assets to skydata pack"
    mkdir -p "$PACK_DIR"
    for pack_asset in "${PACK_ASSETS[@]}"; do
        [[ -d "$ASSETS_DIR/$pack_asset" ]] || {
            echo "missing $ASSETS_DIR/$pack_asset - run without --no-assets" >&2; exit 1; }
        mv "$ASSETS_DIR/$pack_asset" "$PACK_DIR/$pack_asset"
    done
    echo "    pack: $(du -sh "$PACK_DIR" | cut -f1), base: $(du -sh "$ASSETS_DIR" | cut -f1)"
fi

echo "==> package"
hard_rm "$BUILD_DIR/src/android-build/libs"
hard_rm "$BUILD_DIR/src/android-build/assets"
hard_rm "$BUILD_DIR/src/android-build/skydata"
OUTPUTS="$BUILD_DIR/src/android-build/build/outputs"
if [[ $BUNDLE -eq 1 ]]; then
    hard_rm "$OUTPUTS/bundle"
    cmake --build "$BUILD_DIR" --target asterium_make_aab
    UNSIGNED="$(ls "$OUTPUTS"/bundle/release/*.aab 2>/dev/null | head -1)"
else
    cmake --build "$BUILD_DIR" --target asterium_make_apk
    UNSIGNED="$OUTPUTS/apk/release/android-build-release-unsigned.apk"
fi
[[ -f "$UNSIGNED" ]] || { echo "no package produced under $OUTPUTS" >&2; exit 1; }

echo "==> verify native libraries (16KB ELF alignment)"
python3 "$ROOT/util/check-android-page-size.py" --readelf "$READELF" "$UNSIGNED"

KEYSTORE="${ASTERIUM_KEYSTORE:-$ROOT/.keystore/asterium-dev.keystore}"
KEY_ALIAS="${ASTERIUM_KEY_ALIAS:-asterium}"
STORE_PASS="${ASTERIUM_KEYSTORE_PASS:-asterium}"

if [[ ! -f "$KEYSTORE" ]]; then
    echo "==> generating development keystore at $KEYSTORE"
    mkdir -p "$(dirname "$KEYSTORE")"
    "$JAVA_HOME/bin/keytool" -genkeypair -v \
        -keystore "$KEYSTORE" -alias "$KEY_ALIAS" \
        -keyalg RSA -keysize 4096 -validity 10000 \
        -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
        -dname "CN=Asterium Development, OU=Asterium, O=Asterium, C=US" >/dev/null
fi

OUT_DIR="$ROOT/dist"
mkdir -p "$OUT_DIR"

if [[ $BUNDLE -eq 1 ]]; then
    SIGNED="$OUT_DIR/asterium-$ABI.aab"
    echo "==> sign"
    rm -f "$SIGNED"
    "$JAVA_HOME/bin/jarsigner" -keystore "$KEYSTORE" \
        -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
        -sigalg SHA256withRSA -digestalg SHA-256 \
        -signedjar "$SIGNED" "$UNSIGNED" "$KEY_ALIAS" >/dev/null
    "$JAVA_HOME/bin/jarsigner" -verify "$SIGNED" >/dev/null
    LABEL="AAB"
else
    SIGNED="$OUT_DIR/asterium-$ABI.apk"
    echo "==> zipalign + sign"
    "$BUILD_TOOLS/zipalign" -f -P 16 4 "$UNSIGNED" "$SIGNED.tmp"
    "$BUILD_TOOLS/apksigner" sign \
        --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
        --ks-pass "pass:$STORE_PASS" --key-pass "pass:$STORE_PASS" \
        --out "$SIGNED" "$SIGNED.tmp"
    rm -f "$SIGNED.tmp" "$SIGNED.idsig"

    "$BUILD_TOOLS/apksigner" verify --print-certs "$SIGNED" >/dev/null
    "$BUILD_TOOLS/zipalign" -c -P 16 4 "$SIGNED"
    LABEL="APK"
fi

echo
echo "$LABEL: $SIGNED ($(du -h "$SIGNED" | cut -f1), versionCode $VERSION_CODE)"
