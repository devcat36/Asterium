# Asterium

A touch-first Android planetarium, forked from
[Stellarium](https://stellarium.org/). The APK ships the complete Stellarium
engine and data tree: star and deep-sky catalogues, sky cultures, landscapes
and plug-ins. A fresh install works offline.

The source is GPL-2.0-or-later; Android binaries are distributed under
GPL-3.0. Licence, attribution and the fork record are in
[`NOTICE.md`](NOTICE.md). Asterium is not affiliated with or endorsed by the
Stellarium project. Bug reports and feature requests are welcome as issues. 

## Building

| | |
|---|---|
| Qt | 6.8.3, host (`macos`/`gcc_64`) **and** `android_arm64_v8a` |
| Qt modules | qtcharts, qtpositioning, qtmultimedia, qtserialport, qtimageformats, qtsensors |
| Android SDK | platform 36, build-tools 36, NDK 26.1.10909125 |
| JDK | 17 or 21 |
| Host tools | CMake ≥ 3.18, Ninja, Python 3 |

Qt can be installed without the online installer:

```sh
pip install aqtinstall
python3 -m aqt install-qt mac desktop 6.8.3 -O ~/Qt \
    -m qtcharts qtpositioning qtmultimedia qtserialport qtimageformats qtsensors
python3 -m aqt install-qt all_os android 6.8.3 android_arm64_v8a -O ~/Qt \
    -m qtcharts qtpositioning qtmultimedia qtserialport qtimageformats qtsensors
```

Then:

```sh
util/android-build.sh                 # → dist/asterium-arm64-v8a.apk
util/android-build.sh --aab           # → dist/asterium-arm64-v8a.aab (Play Store)
util/android-build.sh --abi x86_64    # emulator on an Intel host
util/android-build.sh --no-assets     # skip the data sync when iterating on code
util/android-build.sh --clean         # from scratch
```

Signing uses a development keystore at `.keystore/asterium-dev.keystore`,
created on first run. **Set `ASTERIUM_KEYSTORE`, `ASTERIUM_KEY_ALIAS` and
`ASTERIUM_KEYSTORE_PASS` to your own release key before publishing anywhere.**
