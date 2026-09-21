# Asterium

An offline Android planetarium based on [Stellarium](https://stellarium.org/).
Licensing and attribution: [NOTICE.md](NOTICE.md).

## Requirements

| Dependency | Version |
|---|---|
| Qt | 6.10.1, host and Android |
| Qt modules | qtcharts, qtpositioning, qtmultimedia, qtserialport, qtimageformats, qtsensors |
| Android SDK | Platform 36, build-tools 36 |
| Android NDK | 27.2.12479018 |
| JDK | 17 or 21 |
| Tools | CMake ≥ 3.18, Ninja, Python 3 |

## Build

Install Qt on macOS:

```sh
pip install aqtinstall
python3 -m aqt install-qt mac desktop 6.10.1 -O ~/Qt \
    -m qtcharts qtpositioning qtmultimedia qtserialport qtimageformats qtsensors
python3 -m aqt install-qt all_os android 6.10.1 android_arm64_v8a -O ~/Qt \
    -m qtcharts qtpositioning qtmultimedia qtserialport qtimageformats qtsensors
sdkmanager "ndk;27.2.12479018"
```

Build an APK or Play Store bundle:

```sh
util/android-build.sh
util/android-build.sh --aab
```

Output: `dist/asterium-arm64-v8a.apk` or `.aab`.
Use `--clean` after changing Qt or the NDK; `--no-assets` skips data staging.

Release signing: set `ASTERIUM_KEYSTORE`, `ASTERIUM_KEY_ALIAS`, and
`ASTERIUM_KEYSTORE_PASS`. Otherwise, the script creates a development key.

Release builds use R8 and verify 16 KB native library alignment.
Keep the matching `dist/*-mapping.txt` with each release for crash retracing.
