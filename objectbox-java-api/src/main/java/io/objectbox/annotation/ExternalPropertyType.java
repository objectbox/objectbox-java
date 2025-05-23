/*
 * Copyright 2025 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.annotation;


/**
 * A property type of an external system (e.g. another database) that has no default mapping to an ObjectBox type.
 * <p>
 * Use with {@link ExternalType @ExternalType}.
 */
public enum ExternalPropertyType {

    /**
     * Representing type: ByteVector
     * <p>
     * Encoding: 1:1 binary representation, little endian (16 bytes)
     */
    INT_128,
    /**
     * A UUID (Universally Unique Identifier) as defined by RFC 9562.
     * <p>
     * ObjectBox uses the UUIDv7 scheme (timestamp + random) to create new UUIDs. UUIDv7 is a good choice for database
     * keys as it's mostly sequential and encodes a timestamp. However, if keys are used externally, consider
     * {@link #UUID_V4} for better privacy by not exposing any time information.
     * <p>
     * Representing type: ByteVector
     * <p>
     * Encoding: 1:1 binary representation (16 bytes)
     */
    UUID,
    /**
     * IEEE 754 decimal128 type, e.g. supported by MongoDB.
     * <p>
     * Representing type: ByteVector
     * <p>
     * Encoding: 1:1 binary representation (16 bytes)
     */
    DECIMAL_128,
    /**
     * UUID represented as a string of 36 characters, e.g. "019571b4-80e3-7516-a5c1-5f1053d23fff".
     * <p>
     * For efficient storage, consider the {@link #UUID} type instead, which occupies only 16 bytes (20 bytes less).
     * This type may still be a convenient alternative as the string type is widely supported and more human-readable.
     * In accordance to standards, new UUIDs generated by ObjectBox use lowercase hexadecimal digits.
     * <p>
     * Representing type: String
     */
    UUID_STRING,
    /**
     * A UUID (Universally Unique Identifier) as defined by RFC 9562.
     * <p>
     * ObjectBox uses the UUIDv4 scheme (completely random) to create new UUIDs.
     * <p>
     * Representing type: ByteVector
     * <p>
     * Encoding: 1:1 binary representation (16 bytes)
     */
    UUID_V4,
    /**
     * Like {@link #UUID_STRING}, but using the UUIDv4 scheme (completely random) to create new UUID.
     * <p>
     * Representing type: String
     */
    UUID_V4_STRING,
    /**
     * A key/value map; e.g. corresponds to a JSON object or a MongoDB document (although not keeping the key order).
     * Unlike the Flex type, this must contain a map value (e.g. not a vector or a scalar).
     * <p>
     * Representing type: Flex
     * <p>
     * Encoding: Flex
     */
    FLEX_MAP,
    /**
     * A vector (aka list or array) of flexible elements; e.g. corresponds to a JSON array or a MongoDB array. Unlike
     * the Flex type, this must contain a vector value (e.g. not a map or a scalar).
     * <p>
     * Representing type: Flex
     * <p>
     * Encoding: Flex
     */
    FLEX_VECTOR,
    /**
     * Placeholder (not yet used) for a JSON document.
     * <p>
     * Representing type: String
     */
    JSON,
    /**
     * Placeholder (not yet used) for a BSON document.
     * <p>
     * Representing type: ByteVector
     */
    BSON,
    /**
     * JavaScript source code.
     * <p>
     * Representing type: String
     */
    JAVASCRIPT,
    /**
     * A JSON string that is converted to a native "complex" representation in the external system.
     * <p>
     * For example in MongoDB, embedded/nested documents are converted to a JSON string in ObjectBox and vice versa.
     * This allows a quick and simple way to work with non-normalized data from MongoDB in ObjectBox. Alternatively, you
     * can use {@link #FLEX_MAP} and {@link #FLEX_VECTOR} to map to language primitives (e.g. maps with string keys; not
     * supported by all ObjectBox languages yet).
     * <p>
     * For MongoDB, (nested) documents and arrays are supported.
     * <p>
     * Note that this is very close to the internal representation, e.g. the key order is preserved (unlike Flex).
     * <p>
     * Representing type: String
     */
    JSON_TO_NATIVE,
    /**
     * A vector (array) of Int128 values.
     */
    INT_128_VECTOR,
    /**
     * A vector (array) of Uuid values.
     */
    UUID_VECTOR,
    /**
     * The 12-byte ObjectId type in MongoDB.
     * <p>
     * Representing type: ByteVector
     * <p>
     * Encoding: 1:1 binary representation (12 bytes)
     */
    MONGO_ID,
    /**
     * A vector (array) of MongoId values.
     */
    MONGO_ID_VECTOR,
    /**
     * Representing type: Long
     * <p>
     * Encoding: Two unsigned 32-bit integers merged into a 64-bit integer.
     */
    MONGO_TIMESTAMP,
    /**
     * Representing type: ByteVector
     * <p>
     * Encoding: 3 zero bytes (reserved, functions as padding), fourth byte is the sub-type, followed by the binary
     * data.
     */
    MONGO_BINARY,
    /**
     * Representing type: string vector with 2 elements (index 0: pattern, index 1: options)
     * <p>
     * Encoding: 1:1 string representation
     */
    MONGO_REGEX

}
