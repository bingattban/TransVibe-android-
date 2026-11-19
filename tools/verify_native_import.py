#!/usr/bin/env python3
"""Fail-closed verifier for a GitHub-built Android ARM64 Whisper .so.

The builder's manifest is not trusted merely because it is JSON: the calling
workflow must also authenticate the source run as a successful main-branch run
in the exact TransVibe-so- repository.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import struct
import subprocess

SYMBOL_PREFIX = "Java_com_transvibe_app_nativebridge_WhisperNativeBridge_"
METHODS = ("nativeIsLinked", "nativeInit", "nativeCancel", "nativeTranscribe", "nativeFree")
ALLOWED_NEEDED = {"libc.so", "libm.so", "libdl.so", "liblog.so", "libandroid.so"}

def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def import_verified(incoming: Path, app_root: Path, readelf: str = "readelf"):
    manifest_path = incoming / "native-manifest.json"
    payload = incoming / "app/libs/arm64-v8a/libtransvibe_whisper.so"
    cpp = app_root / "src/main/cpp/whisper_jni.cpp"
    kotlin = app_root / "src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt"
    assert manifest_path.is_file() and payload.is_file() and cpp.is_file() and kotlin.is_file(), "Incomplete source/build"
    manifest = json.loads(manifest_path.read_text(encoding="utf8"))
    for key, expected in {
        "schema_version": 2,
        "builder_repository": "bingattban/TransVibe-so-",
        "app_repository": "bingattban/TransVibe-android-",
        "engine": "whisper.cpp",
        "first_model": "ggml-tiny.bin",
        "abi": "arm64-v8a",
        "min_sdk": 26
    }.items():
        assert manifest.get(key) == expected, f"Manifest mismatch: {key}"
    assert re.fullmatch(r"[0-9a-f]{40}", str(manifest.get("app_source_commit", "")))
    assert re.fullmatch(r"[0-9a-f]{40}", str(manifest.get("whisper_commit", "")))
    assert digest(cpp) == manifest.get("jni_source_sha256"), "JNI source changed; rebuild library"
    assert digest(kotlin) == manifest.get("kotlin_source_sha256"), "Kotlin source changed; rebuild library"
    assert digest(payload) == manifest.get("binary_sha256"), "Binary SHA-256 mismatch"
    b = payload.read_bytes()
    assert len(b) > 65536 and b[:6] == b"\x7fELF\x02\x01", "Not a valid ELF64 little-endian"
    assert struct.unpack_from("<H", b, 16)[0] == 3, "Not an ELF shared object"
    assert struct.unpack_from("<H", b, 18)[0] == 183, "Not Android AArch64"
    syms = subprocess.check_output([readelf, "-Ws", str(payload)], text=True)
    for name in METHODS:
        assert SYMBOL_PREFIX + name in syms, f"Missing exported JNI method {name}"
    info = subprocess.check_output([readelf, "-lW", str(payload)], text=True)
    loads = [int(row.split()[-1],16) for row in info.splitlines() if re.match(r"\s*LOAD\s+",row)]
    assert loads and all(x >= 0x4000 for x in loads), "LOAD segment misalignment"
    dylibs = subprocess.check_output([readelf, "-dW", str(payload)], text=True)
    needed = set(re.findall(r"\(NEEDED\).*?\[([^]]+)\]",dylibs))
    assert needed <= ALLOWED_NEEDED, f"Unexpected .so dependencies: {needed-ALLOWED_NEEDED}"
    destination = app_root / "libs/arm64-v8a"
    destination.mkdir(parents=True, exist_ok=True)
    # Only copy after every integrity check passes.
    shutil.copy2(payload, destination / "libtransvibe_whisper.so")
    (app_root / "libs/native-provenance.json").write_text(
        json.dumps(manifest,indent=2,sort_keys=True)+"\n",encoding="utf8"
    )
    print("PASS: validated first-model native import", manifest["binary_sha256"])

if __name__ == "__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--incoming",type=Path,required=True)
    p.add_argument("--app-root",type=Path,default=Path("app"))
    p.add_argument("--readelf",default="readelf")
    args=p.parse_args()
    import_verified(args.incoming,args.app_root,args.readelf)
