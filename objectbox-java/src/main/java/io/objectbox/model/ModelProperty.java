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

// automatically generated by the FlatBuffers compiler, do not modify

package io.objectbox.model;

import io.objectbox.flatbuffers.BaseVector;
import io.objectbox.flatbuffers.BooleanVector;
import io.objectbox.flatbuffers.ByteVector;
import io.objectbox.flatbuffers.Constants;
import io.objectbox.flatbuffers.DoubleVector;
import io.objectbox.flatbuffers.FlatBufferBuilder;
import io.objectbox.flatbuffers.FloatVector;
import io.objectbox.flatbuffers.IntVector;
import io.objectbox.flatbuffers.LongVector;
import io.objectbox.flatbuffers.ShortVector;
import io.objectbox.flatbuffers.StringVector;
import io.objectbox.flatbuffers.Struct;
import io.objectbox.flatbuffers.Table;
import io.objectbox.flatbuffers.UnionVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class ModelProperty extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }
  public static ModelProperty getRootAsModelProperty(ByteBuffer _bb) { return getRootAsModelProperty(_bb, new ModelProperty()); }
  public static ModelProperty getRootAsModelProperty(ByteBuffer _bb, ModelProperty obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public ModelProperty __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public io.objectbox.model.IdUid id() { return id(new io.objectbox.model.IdUid()); }
  public io.objectbox.model.IdUid id(io.objectbox.model.IdUid obj) { int o = __offset(4); return o != 0 ? obj.__assign(o + bb_pos, bb) : null; }
  public String name() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public ByteBuffer nameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 1); }
  public int type() { int o = __offset(8); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  /**
   * bit flags: e.g. indexed, not-nullable
   */
  public long flags() { int o = __offset(10); return o != 0 ? (long)bb.getInt(o + bb_pos) & 0xFFFFFFFFL : 0L; }
  public io.objectbox.model.IdUid indexId() { return indexId(new io.objectbox.model.IdUid()); }
  public io.objectbox.model.IdUid indexId(io.objectbox.model.IdUid obj) { int o = __offset(12); return o != 0 ? obj.__assign(o + bb_pos, bb) : null; }
  /**
   * For relations only: name of the target entity (will be replaced by "target entity ID" at the schema level)
   */
  public String targetEntity() { int o = __offset(14); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer targetEntityAsByteBuffer() { return __vector_as_bytebuffer(14, 1); }
  public ByteBuffer targetEntityInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 14, 1); }
  /**
   * This will probably move out of the core model into something binding specific.
   * A virtual property's "target name" typically references an existing field in the entity at the language level
   * of the binding. In contrast to this, the virtual property (via model "name") does not exist in the entity at
   * the language level (thus virtual), but in ObjectBox's core DB.
   * Example: consider a Java entity which has a ToOne (a Java specific relation wrapper) member called "parent".
   * ObjectBox core is unaware of that ToOne, but works with the "parentId" relation property,
   * which in turn does not exist in the Java entity.
   * The mapping between "parentId" and "parent" is done by our JNI binding.
   */
  public String virtualTarget() { int o = __offset(16); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer virtualTargetAsByteBuffer() { return __vector_as_bytebuffer(16, 1); }
  public ByteBuffer virtualTargetInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 16, 1); }
  /**
   * Secondary name ignored by core; e.g. may reference a binding specific name (e.g. Java property)
   */
  public String nameSecondary() { int o = __offset(18); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameSecondaryAsByteBuffer() { return __vector_as_bytebuffer(18, 1); }
  public ByteBuffer nameSecondaryInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 18, 1); }
  /**
   * For value-based indexes, this defines the maximum length of the value stored for indexing
   */
  public long maxIndexValueLength() { int o = __offset(20); return o != 0 ? (long)bb.getInt(o + bb_pos) & 0xFFFFFFFFL : 0L; }
  /**
   * For float vectors properties and nearest neighbor search, you can index the property with HNSW.
   * This is the configuration for the HNSW index, e.g. dimensions and parameters affecting quality/speed tradeoff.
   */
  public io.objectbox.model.HnswParams hnswParams() { return hnswParams(new io.objectbox.model.HnswParams()); }
  public io.objectbox.model.HnswParams hnswParams(io.objectbox.model.HnswParams obj) { int o = __offset(22); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  /**
   * Optional type used in an external system, e.g. another database that ObjectBox syncs with.
   * Note that the supported mappings from ObjectBox types to external types are limited.
   */
  public int externalType() { int o = __offset(24); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  /**
   * Optional name used in an external system, e.g. another database that ObjectBox syncs with.
   */
  public String externalName() { int o = __offset(26); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer externalNameAsByteBuffer() { return __vector_as_bytebuffer(26, 1); }
  public ByteBuffer externalNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 26, 1); }

  public static void startModelProperty(FlatBufferBuilder builder) { builder.startTable(12); }
  public static void addId(FlatBufferBuilder builder, int idOffset) { builder.addStruct(0, idOffset, 0); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(1, nameOffset, 0); }
  public static void addType(FlatBufferBuilder builder, int type) { builder.addShort(2, (short) type, (short) 0); }
  public static void addFlags(FlatBufferBuilder builder, long flags) { builder.addInt(3, (int) flags, (int) 0L); }
  public static void addIndexId(FlatBufferBuilder builder, int indexIdOffset) { builder.addStruct(4, indexIdOffset, 0); }
  public static void addTargetEntity(FlatBufferBuilder builder, int targetEntityOffset) { builder.addOffset(5, targetEntityOffset, 0); }
  public static void addVirtualTarget(FlatBufferBuilder builder, int virtualTargetOffset) { builder.addOffset(6, virtualTargetOffset, 0); }
  public static void addNameSecondary(FlatBufferBuilder builder, int nameSecondaryOffset) { builder.addOffset(7, nameSecondaryOffset, 0); }
  public static void addMaxIndexValueLength(FlatBufferBuilder builder, long maxIndexValueLength) { builder.addInt(8, (int) maxIndexValueLength, (int) 0L); }
  public static void addHnswParams(FlatBufferBuilder builder, int hnswParamsOffset) { builder.addOffset(9, hnswParamsOffset, 0); }
  public static void addExternalType(FlatBufferBuilder builder, int externalType) { builder.addShort(10, (short) externalType, (short) 0); }
  public static void addExternalName(FlatBufferBuilder builder, int externalNameOffset) { builder.addOffset(11, externalNameOffset, 0); }
  public static int endModelProperty(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public ModelProperty get(int j) { return get(new ModelProperty(), j); }
    public ModelProperty get(ModelProperty obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

