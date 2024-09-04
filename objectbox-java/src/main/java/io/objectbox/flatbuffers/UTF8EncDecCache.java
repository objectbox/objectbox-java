package io.objectbox.flatbuffers;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

public class UTF8EncDecCache {
    final CharsetEncoder encoder;
    final CharsetDecoder decoder;
    CharSequence lastInput = null;
    ByteBuffer lastOutput = null;

    UTF8EncDecCache() {
        encoder = StandardCharsets.UTF_8.newEncoder();
        decoder = StandardCharsets.UTF_8.newDecoder();
    }
}
