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
| Qt | 6.10.1, host (`macos`/`gcc_64`) **and** `android_arm64_v8a` |
| Qt modules | qtcharts, qtpositioning, qtmultimedia, qtserialport, qtimageformats, qtsensors |
| Android SDK | platform 36, build-tools 36, NDK r27c (27.2.12479018) |
| JDK | 17 or 21 |
| Host tools | CMake ≥ 3.18, Ninja, Python 3 |

Qt can be installed without the online installer:

```sh
pip install aqtinstall
python3 -m aqt install-qt mac desktop 6.10.1 -O ~/Qt \
    -m qtcharts qtpositioning qtmultimedia qtserialport qtimageformats qtsensors
python3 -m aqt install-qt all_os android 6.10.1 android_arm64_v8a -O ~/Qt \
    -m qtcharts qtpositioning qtmultimedia qtserialport qtimageformats qtsensors
```

Install the matching NDK with `sdkmanager "ndk;27.2.12479018"`. After upgrading
Qt or the NDK, rebuild with `util/android-build.sh --clean` to discard the old
CMake toolchain cache.

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

### 16KB page sizes

Android builds use [Qt 6.10.1 or newer](https://www.qt.io/blog/android-15-and-16-support)
and [Qt's matching NDK r27c](https://doc.qt.io/qt-6.10/android.html), with flexible
page sizes enabled and 16KB ELF linker alignment. AGP 8.10.1 packages native
libraries uncompressed. The build script checks every packaged 64-bit `.so`
(including Qt, OpenSSL and `libc++_shared.so`) before signing, and aligns and
verifies the final APK with `zipalign -P 16`.

To inspect a package independently:

```sh
python3 util/check-android-page-size.py --readelf /path/to/ndk/llvm-readelf dist/asterium-arm64-v8a.aab
bundletool dump config --bundle=dist/asterium-arm64-v8a.aab
adb shell getconf PAGE_SIZE
```

For an AAB, also generate APKs with bundletool and verify the delivered APKs
with `zipalign -c -P 16 4`. ELF/ZIP checks do not replace runtime testing on
both 4KB and 16KB devices; follow the
[Android verification guide](https://developer.android.com/guide/practices/page-sizes).
