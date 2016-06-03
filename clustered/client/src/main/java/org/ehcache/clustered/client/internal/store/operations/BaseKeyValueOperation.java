/*
 * Copyright Terracotta, Inc.
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

package org.ehcache.clustered.client.internal.store.operations;

import org.ehcache.clustered.client.internal.store.operations.codecs.CodecException;
import org.ehcache.spi.serialization.Serializer;

import java.nio.ByteBuffer;

abstract class BaseKeyValueOperation<K, V> implements Operation<K, V> {

  private final K key;
  private final V value;
  private final long expirationTimeStamp;

  BaseKeyValueOperation(K key, V value, long expirationTimeStamp) {
    if(key == null) {
      throw new NullPointerException("Key can not be null");
    }
    if(value == null) {
      throw new NullPointerException("Value can not be null");
    }
    this.key = key;
    this.value = value;
    this.expirationTimeStamp = expirationTimeStamp;
  }

  BaseKeyValueOperation(ByteBuffer buffer, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    OperationCode opCode = OperationCode.valueOf(buffer.get());
    if (opCode != getOpCode()) {
      throw new IllegalArgumentException("Invalid operation: " + opCode);
    }
    this.expirationTimeStamp = buffer.getLong();
    int keySize = buffer.getInt();
    buffer.limit(buffer.position() + keySize);
    ByteBuffer keyBlob = buffer.slice();
    buffer.position(buffer.limit());
    buffer.limit(buffer.capacity());
    try {
      this.key = keySerializer.read(keyBlob);
      this.value = valueSerializer.read(buffer.slice());
    } catch (ClassNotFoundException e) {
      throw new CodecException(e);
    }
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  /**
   * Here we need to encode two objects of unknown size: the key and the value.
   * Encoding should be done in such a way that the key and value can be read
   * separately while decoding the bytes.
   * So the way it is done here is by writing the size of the payload along with
   * the payload. That is, the size of the key payload is written before the key
   * itself. The value payload is written after that.
   *
   * While decoding, the size is read first and then reading the same number of
   * bytes will get you the key payload. Whatever that is left is the value payload.
   */
  @Override
  public ByteBuffer encode(final Serializer<K> keySerializer, final Serializer<V> valueSerializer) {
    ByteBuffer keyBuf = keySerializer.serialize(key);
    ByteBuffer valueBuf = valueSerializer.serialize(value);

    int size = BYTE_SIZE_BYTES +   // Operation type
               INT_SIZE_BYTES +    // Size of the key payload
               LONG_SIZE_BYTES +   // Size of expiration time stamp
               keyBuf.remaining() + // the key payload itself
               valueBuf.remaining();  // the value payload

    ByteBuffer buffer = ByteBuffer.allocate(size);

    buffer.put(getOpCode().getValue());
    buffer.putLong(this.expirationTimeStamp);
    buffer.putInt(keyBuf.remaining());
    buffer.put(keyBuf);
    buffer.put(valueBuf);
    buffer.flip();
    return buffer;
  }

  @Override
  public String toString() {
    return "{" + getOpCode() + "# key: " + key + ", value: " + value + "}";
  }

  @Override
  public boolean equals(final Object obj) {
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof BaseKeyValueOperation)) {
      return false;
    }

    BaseKeyValueOperation other = (BaseKeyValueOperation) obj;
    if(this.getOpCode() != other.getOpCode()) {
      return false;
    }
    if(!this.getKey().equals(other.getKey())) {
      return false;
    }
    if(!this.getValue().equals(other.getValue())) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = getOpCode().hashCode();
    hash = hash * 31 + key.hashCode();
    hash = hash * 31 + value.hashCode();
    return hash;
  }
}
