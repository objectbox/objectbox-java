# FlatBuffers
                         
This is a copy of the [FlatBuffers](https://github.com/google/flatbuffers) for Java source code in a custom package
to avoid conflicts with FlatBuffers generated Java code from users of this library.

Current version: `23.5.26` (Note: version in `Constants.java` may be lower).

Copy a different version using the script in `scripts\update-flatbuffers.sh`.
It expects FlatBuffers source files in the `../flatbuffers` directory (e.g. check out
the desired FlatBuffers tag from https://github.com/objectbox/flatbuffers next to this repo).
The Java library is expected in `objectbox-java`, e.g. run the script from the root of this repo.

## Licensing

Flatbuffers is licensed under the Apache License, Version 2.0.
