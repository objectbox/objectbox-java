#!/usr/bin/env bash
set -e

# Enables running Gradle tasks with JNI libraries built with AddressSanitizer (ASan).
#
# Note: currently only objectbox feature branches build JNI libraries with ASan. If this is used
# with "regularly" built JNI libraries this will run without error, but also NOT detect any issues.
#
# Arguments are passed directly to Gradle. If no arguments are specified runs the 'test' task.
#
# This script supports the following environment variables:
#
# - ASAN_LIB_SO: path to ASan library, if not set tries to detect path
# - ASAN_SYMBOLIZER_PATH: path to llvm-symbolizer, if not set tries to detect path
# - ASAN_OPTIONS: ASan options, if not set configures to not detect leaks
#
# The ASan detection is known to work with the buildenv-core:2024-07-11 image or Ubuntu 24.04 with a clang setup.

# AddressSanitizer shared library (clang or gcc setup)
# https://github.com/google/sanitizers/wiki/AddressSanitizer
if [ -z "$ASAN_LIB_SO" ]; then  # If not supplied (e.g. by CI script), try to locate the lib:
    ASAN_ARCH=$(uname -m) # x86_64 or aarch64
    echo "No ASAN_LIB_SO defined, trying to locate dynamically..."
    # Known to work on Ubuntu 24.04: Find in the typical llvm directory (using `tail` for latest version; `head` would be oldest")
    ASAN_LIB_SO_CLANG_LATEST=$(find /usr/lib/llvm-*/ -name libclang_rt.asan-${ASAN_ARCH}.so | tail -1)
    # Known to work with clang 16 on Rocky Linux 8.10 (path is like /usr/local/lib/clang/16/lib/x86_64-unknown-linux-gnu/libclang_rt.asan.so)
    ASAN_LIB_SO_CLANG=$(clang -print-file-name=libclang_rt.asan.so || true)
    # Approach via https://stackoverflow.com/a/54386573/551269, but use libasan.so.8 instead of libasan.so
    # to not find the linker script, but the actual library (and to avoid parsing it out of the linker script).
    ASAN_LIB_SO_GCC=$(gcc -print-file-name=libasan.so.8 || true)
    echo "clang latest asan lib: ${ASAN_LIB_SO_CLANG_LATEST}"
    echo "       clang asan lib: ${ASAN_LIB_SO_CLANG}"
    echo "         gcc asan lib: ${ASAN_LIB_SO_GCC}"
    # prefer clang version in case clang llvm-symbolizer is used (see below)
    if [ -f "${ASAN_LIB_SO_CLANG_LATEST}" ]; then
        export ASAN_LIB_SO="${ASAN_LIB_SO_CLANG_LATEST}"
    elif [ -f "${ASAN_LIB_SO_CLANG}" ]; then
        export ASAN_LIB_SO="${ASAN_LIB_SO_CLANG}"
    elif [ -f "${ASAN_LIB_SO_GCC}" ]; then
        export ASAN_LIB_SO="${ASAN_LIB_SO_GCC}"
    else
        echo "No asan lib found; please specify via ASAN_LIB_SO"
        exit 1
    fi
fi

# Set up llvm-symbolizer to symbolize a stack trace (clang setup only)
# https://github.com/google/sanitizers/wiki/AddressSanitizerCallStack
# Rocky Linux 8 (buildenv-core)
if [ -z "$ASAN_SYMBOLIZER_PATH" ]; then
    echo "ASAN_SYMBOLIZER_PATH not set, trying to find it in /usr/local/bin/..."
    export ASAN_SYMBOLIZER_PATH="$(find /usr/local/bin/ -name llvm-symbolizer | tail -1 )"
fi
# Ubuntu 22.04
if [ -z "$ASAN_SYMBOLIZER_PATH" ]; then
    echo "ASAN_SYMBOLIZER_PATH not set, trying to find it in /usr/lib/llvm-*/..."
    export ASAN_SYMBOLIZER_PATH="$(find /usr/lib/llvm-*/ -name llvm-symbolizer | tail -1)"
fi

# Turn off leak detection by default
# https://github.com/google/sanitizers/wiki/AddressSanitizerLeakSanitizer
if [ -z "$ASAN_OPTIONS" ]; then
    echo "ASAN_OPTIONS not set, setting default values"
    export ASAN_OPTIONS="detect_leaks=0"
fi

echo ""
echo "ℹ️ test-with-asan.sh final values:"
echo "ASAN_LIB_SO: $ASAN_LIB_SO"
echo "ASAN_SYMBOLIZER_PATH: $ASAN_SYMBOLIZER_PATH"
echo "ASAN_OPTIONS: $ASAN_OPTIONS"
echo "ASAN_LIB_SO resolves to:"
ls -l $ASAN_LIB_SO
echo "ASAN_SYMBOLIZER_PATH resolves to:"
if [ -z "$ASAN_SYMBOLIZER_PATH" ]; then
    echo "WARNING: ASAN_SYMBOLIZER_PATH not set, stack traces will not be symbolized"
else
    ls -l $ASAN_SYMBOLIZER_PATH
fi

if [[ $# -eq 0 ]] ; then
    args=test
else
    args=$@
fi

echo ""
echo "➡️ Running Gradle with arguments \"$args\" in directory $(pwd)..."
LD_PRELOAD=${ASAN_LIB_SO} ./gradlew ${args}
