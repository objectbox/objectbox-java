#!/usr/bin/env bash
set -e

if [ -z "$ASAN_LIB_SO" ]; then
    export ASAN_LIB_SO="$(find /usr/lib/llvm-7/ -name libclang_rt.asan-x86_64.so | head -1)"
fi

if [ -z "$ASAN_SYMBOLIZER_PATH" ]; then
    export ASAN_SYMBOLIZER_PATH="$(find /usr/lib/llvm-7 -name llvm-symbolizer | head -1 )"
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