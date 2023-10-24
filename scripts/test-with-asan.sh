#!/usr/bin/env bash
set -e

# Runs Gradle with address sanitizer enabled. Arguments are passed directly to Gradle.
# If no arguments are specified runs the test task.
# The ASAN detection is known to work with the buildenv-core image or Ubuntu 22.04 with a clang setup.

# ASAN shared library (gcc or clang setup)
if [ -z "$ASAN_LIB_SO" ]; then  # If not supplied (e.g. by CI script), try to locate the lib:
    ASAN_ARCH=$(uname -m) # x86_64 or aarch64
    echo "No ASAN_LIB_SO defined, trying to locate dynamically..."
    # Approach via https://stackoverflow.com/a/54386573/551269
    ASAN_LIB_SO_GCC=$(gcc -print-file-name=libasan.so || true)
    ASAN_LIB_SO_CLANG=$(clang -print-file-name=libclang_rt.asan-${ASAN_ARCH}.so || true)
    # Find in the typical llvm directory (using `tail` for latest version; `head` would be oldest")
    ASAN_LIB_SO_CLANG_LATEST=$(find /usr/lib/llvm-*/ -name libclang_rt.asan-${ASAN_ARCH}.so | tail -1)
    echo "         gcc asan lib: ${ASAN_LIB_SO_GCC}"
    echo "       clang asan lib: ${ASAN_LIB_SO_CLANG}"
    echo "clang latest asan lib: ${ASAN_LIB_SO_CLANG_LATEST}"
    if [ -f "${ASAN_LIB_SO_CLANG_LATEST}" ]; then # prefer this so version matches with llvm-symbolizer below
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

# llvm-symbolizer (clang setup only)
# Rocky Linux 8 (buildenv-core)
if [ -z "$ASAN_SYMBOLIZER_PATH" ]; then
    export ASAN_SYMBOLIZER_PATH="$(find /usr/local/bin/ -name llvm-symbolizer | tail -1 )"
fi
# Ubuntu 22.04
if [ -z "$ASAN_SYMBOLIZER_PATH" ]; then
    export ASAN_SYMBOLIZER_PATH="$(find /usr/lib/llvm-*/ -name llvm-symbolizer | tail -1)"
fi

if [ -z "$ASAN_OPTIONS" ]; then
    export ASAN_OPTIONS="detect_leaks=0"
fi

echo "ASAN_LIB_SO: $ASAN_LIB_SO"
echo "ASAN_SYMBOLIZER_PATH: $ASAN_SYMBOLIZER_PATH"
echo "ASAN_OPTIONS: $ASAN_OPTIONS"
ls -l $ASAN_LIB_SO
ls -l $ASAN_SYMBOLIZER_PATH

if [[ $# -eq 0 ]] ; then
    args=test
else
    args=$@
fi
echo "Starting Gradle for target(s) \"$args\"..."
pwd

LD_PRELOAD=${ASAN_LIB_SO} ./gradlew ${args}
