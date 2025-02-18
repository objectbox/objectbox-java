/*
 * Copyright 2024 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.sync.server;

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

/**
 * Configuration to connect to another (remote) cluster peer.
 * If this server is started in cluster mode, it connects to other cluster peers.
 */
@SuppressWarnings("unused")
public final class ClusterPeerConfig extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }
  public static ClusterPeerConfig getRootAsClusterPeerConfig(ByteBuffer _bb) { return getRootAsClusterPeerConfig(_bb, new ClusterPeerConfig()); }
  public static ClusterPeerConfig getRootAsClusterPeerConfig(ByteBuffer _bb, ClusterPeerConfig obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public ClusterPeerConfig __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String url() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer urlAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer urlInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public io.objectbox.sync.Credentials credentials() { return credentials(new io.objectbox.sync.Credentials()); }
  public io.objectbox.sync.Credentials credentials(io.objectbox.sync.Credentials obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createClusterPeerConfig(FlatBufferBuilder builder,
      int urlOffset,
      int credentialsOffset) {
    builder.startTable(2);
    ClusterPeerConfig.addCredentials(builder, credentialsOffset);
    ClusterPeerConfig.addUrl(builder, urlOffset);
    return ClusterPeerConfig.endClusterPeerConfig(builder);
  }

  public static void startClusterPeerConfig(FlatBufferBuilder builder) { builder.startTable(2); }
  public static void addUrl(FlatBufferBuilder builder, int urlOffset) { builder.addOffset(0, urlOffset, 0); }
  public static void addCredentials(FlatBufferBuilder builder, int credentialsOffset) { builder.addOffset(1, credentialsOffset, 0); }
  public static int endClusterPeerConfig(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public ClusterPeerConfig get(int j) { return get(new ClusterPeerConfig(), j); }
    public ClusterPeerConfig get(ClusterPeerConfig obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

