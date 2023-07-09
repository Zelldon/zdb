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
package io.zell.zdb.journal.record;

import io.camunda.zeebe.util.Either;
import io.camunda.zeebe.util.buffer.BufferWriter;
import io.camunda.zeebe.util.buffer.DirectBufferWriter;
import java.nio.BufferOverflowException;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public interface JournalRecordSerializer {

  /**
   * Returns the number of bytes required to write a {@link RecordMetadata} to a buffer.
   * @return the expected length of a serialized metadata
   */
  int getMetadataLength();

  /**
   * Reads the {@link RecordMetadata} from the buffer at offset 0. A valid record must exist in the
   * buffer at this position.
   *
   * @param buffer to read
   * @param offset the offset in the buffer at which the metadata will be read from
   * @return a journal record metadata that is read.
   */
  RecordMetadata readMetadata(DirectBuffer buffer, int offset);

  /**
   * Reads the {@link RecordData} from the buffer at offset 0. A valid record must exist in the
   * buffer at this position.
   *
   * @param buffer to read
   * @param offset the offset in the buffer at which the data will be read from
   * @return a journal indexed record that is read.
   */
  RecordData readData(DirectBuffer buffer, int offset);

  /**
   * Returns the length of the serialized {@link RecordMetadata} in the buffer.
   *
   * @param buffer to read
   * @param offset the offset in the buffer at which the metadata will be read from
   * @return the length of the metadata
   */
  int getMetadataLength(DirectBuffer buffer, int offset);
}
