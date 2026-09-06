#!/usr/bin/env python3

import argparse
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tempfile
import zipfile


def check_package(package, readelf):
    checked = 0
    failures = []
    with zipfile.ZipFile(package) as archive, tempfile.TemporaryDirectory() as temp:
        libraries = []
        for entry in archive.infolist():
            parts = PurePosixPath(entry.filename).parts
            if len(parts) >= 3 and parts[-3] == "lib" and parts[-1].endswith(".so"):
                libraries.append(entry)
        if not libraries:
            raise ValueError("package contains no native libraries")
        for entry in libraries:
            abi = PurePosixPath(entry.filename).parts[-2]
            if abi in ("armeabi-v7a", "x86"):
                continue
            library = Path(temp) / "library.so"
            with archive.open(entry) as source, library.open("wb") as target:
                shutil.copyfileobj(source, target)
            result = subprocess.run(
                [readelf, "--program-headers", "--wide", str(library)],
                capture_output=True, text=True, check=True,
            )
            segments = [line.split() for line in result.stdout.splitlines()
                        if line.split() and line.split()[0] == "LOAD"]
            if not segments:
                failures.append(f"{entry.filename}: no ELF LOAD segments")
                continue
            for segment in segments:
                alignment = int(segment[-1], 16)
                offset, address = int(segment[1], 16), int(segment[2], 16)
                if (alignment < 16384 or alignment & (alignment - 1)
                        or (address - offset) % 16384):
                    failures.append(
                        f"{entry.filename}: LOAD alignment {alignment:#x}, "
                        f"offset {offset:#x}, address {address:#x}"
                    )
                    break
            checked += 1
    if failures:
        raise ValueError("16KB-incompatible native libraries:\n  " + "\n  ".join(failures))
    print(f"16KB ELF alignment verified: {checked} libraries in {package}")


def main():
    parser = argparse.ArgumentParser(
        description="Check 64-bit native libraries in APKs/AABs for 16KB ELF alignment."
    )
    parser.add_argument("--readelf", required=True, help="path to NDK llvm-readelf")
    parser.add_argument("package", type=Path, help="APK or AAB to inspect")
    args = parser.parse_args()
    try:
        check_package(args.package, args.readelf)
    except (OSError, ValueError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
