/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
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
package io.zell.zdb.db.readonly.transaction;

import io.camunda.zeebe.db.DbKey;
import io.camunda.zeebe.db.DbValue;
import io.camunda.zeebe.db.impl.ZeebeDbConstants;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.ObjIntConsumer;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class ColumnFamilyContext {

  private static final byte[] ZERO_SIZE_ARRAY = new byte[0];

  // we can also simply use one buffer
  private final ExpandableArrayBuffer keyBuffer = new ExpandableArrayBuffer();
  private final ExpandableArrayBuffer valueBuffer = new ExpandableArrayBuffer();

  private final DirectBuffer keyViewBuffer = new UnsafeBuffer(0, 0);
  private final DirectBuffer valueViewBuffer = new UnsafeBuffer(0, 0);

  private final Queue<ExpandableArrayBuffer> prefixKeyBuffers;
  private int keyLength;
  private final long columnFamilyPrefix;

  ColumnFamilyContext(final long columnFamilyPrefix) {
    this.columnFamilyPrefix = columnFamilyPrefix;
    prefixKeyBuffers = new ArrayDeque<>();
    prefixKeyBuffers.add(new ExpandableArrayBuffer());
    prefixKeyBuffers.add(new ExpandableArrayBuffer());
  }

  public void writeKey(final DbKey key) {
    keyLength = 0;
    keyBuffer.putLong(0, columnFamilyPrefix, ZeebeDbConstants.ZB_DB_BYTE_ORDER);
    keyLength += Long.BYTES;
    key.write(keyBuffer, Long.BYTES);
    keyLength += key.getLength();
  }

  public int getKeyLength() {
    return keyLength;
  }

  public byte[] getKeyBufferArray() {
    return keyBuffer.byteArray();
  }

  public void writeValue(final DbValue value) {
    value.write(valueBuffer, 0);
  }

  public byte[] getValueBufferArray() {
    return valueBuffer.byteArray();
  }

  public void wrapKeyView(final byte[] key) {
    if (key != null) {
      // wrap without the column family key
      keyViewBuffer.wrap(key, Long.BYTES, key.length - Long.BYTES);
    } else {
      keyViewBuffer.wrap(ZERO_SIZE_ARRAY);
    }
  }

  public DirectBuffer getKeyView() {
    return isKeyViewEmpty() ? null : keyViewBuffer;
  }

  public boolean isKeyViewEmpty() {
    return keyViewBuffer.capacity() == ZERO_SIZE_ARRAY.length;
  }

  public void wrapValueView(final byte[] value) {
    if (value != null) {
      valueViewBuffer.wrap(value);
    } else {
      valueViewBuffer.wrap(ZERO_SIZE_ARRAY);
    }
  }

  public DirectBuffer getValueView() {
    return isValueViewEmpty() ? null : valueViewBuffer;
  }

  public boolean isValueViewEmpty() {
    return valueViewBuffer.capacity() == ZERO_SIZE_ARRAY.length;
  }

  public void withPrefixKey(final DbKey key, final ObjIntConsumer<byte[]> prefixKeyConsumer) {
    if (prefixKeyBuffers.peek() == null) {
      throw new IllegalStateException(
          "Currently nested prefix iterations are not supported! This will cause unexpected behavior.");
    }

    final ExpandableArrayBuffer prefixKeyBuffer = prefixKeyBuffers.remove();
    try {
      prefixKeyBuffer.putLong(0, columnFamilyPrefix, ZeebeDbConstants.ZB_DB_BYTE_ORDER);
      key.write(prefixKeyBuffer, Long.BYTES);
      final int prefixLength = Long.BYTES + key.getLength();

      prefixKeyConsumer.accept(prefixKeyBuffer.byteArray(), prefixLength);
    } finally {
      prefixKeyBuffers.add(prefixKeyBuffer);
    }
  }
}
