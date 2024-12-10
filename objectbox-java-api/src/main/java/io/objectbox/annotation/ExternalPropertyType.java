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
     * A vector (array) of Int128 values.
     */
    INT_128_VECTOR,
    /**
     * A vector (array) of Int128 values.
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
