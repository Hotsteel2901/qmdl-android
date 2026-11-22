#!/usr/bin/env bash
set -euo pipefail

CLI_DIR="${1:-/home/hhg/QQ-Music-DL}"
NDK_DIR="${ANDROID_NDK_HOME:-/home/hhg/android-ndk/android-ndk-r29}"
NDK_BIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/jniLibs"

export CC_aarch64_linux_android="$NDK_BIN/aarch64-linux-android26-clang"
export AR_aarch64_linux_android="$NDK_BIN/llvm-ar"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK_BIN/aarch64-linux-android26-clang"

export CC_x86_64_linux_android="$NDK_BIN/x86_64-linux-android26-clang"
export AR_x86_64_linux_android="$NDK_BIN/llvm-ar"
export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$NDK_BIN/x86_64-linux-android26-clang"

cd "$CLI_DIR"
cargo build --release --target aarch64-linux-android
mkdir -p "$OUT_DIR/arm64-v8a"
cp target/aarch64-linux-android/release/qqmusic-dl "$OUT_DIR/arm64-v8a/libqmdl.so"

cargo build --release --target x86_64-linux-android
mkdir -p "$OUT_DIR/x86_64"
cp target/x86_64-linux-android/release/qqmusic-dl "$OUT_DIR/x86_64/libqmdl.so"

ls -lh "$OUT_DIR"/*/libqmdl.so
