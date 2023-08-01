#!/usr/bin/env bash
set -euo pipefail

# Expects FlatBuffers source files in the `../flatbuffers` directory,
# the Java library in `objectbox-java`, e.g. run this from the root of this repo.

script_dir=$(dirname "$(readlink -f "$0")")
cd "${script_dir}/.." # move to project root dir or exit on failure
echo "Running in directory: $(pwd)"

src="../flatbuffers/java/src/main/java/com/google/flatbuffers"
dest="objectbox-java/src/main/java/io/objectbox/flatbuffers"

echo "Copying flatbuffers Java sources"
rm -f ${dest}/*.java
cp -v ${src}/*.java ${dest}/

echo "Updating import statements of Java sources"
find "${dest}" -type f -name "*.java" \
  -exec echo "Processing {}" \; \
  -exec sed -i "s| com.google.flatbuffers| io.objectbox.flatbuffers|g" {} \;
