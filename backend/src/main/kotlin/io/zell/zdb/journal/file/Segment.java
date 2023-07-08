/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zell.zdb.journal.file;

import com.google.common.collect.Sets;
import io.camunda.zeebe.journal.JournalException;
import org.agrona.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;

/**
 * Log segment.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class Segment implements AutoCloseable {

  private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;
  private static final Logger LOG = LoggerFactory.getLogger(Segment.class);

  private final SegmentFile file;
  private final SegmentDescriptor descriptor;
  private final JournalIndex index;
  private final Set<SegmentReader> readers = Sets.newConcurrentHashSet();
  private final MappedByteBuffer buffer;
  private final long lastWrittenAsqn;
  private final long lastIndex;

  // This needs to be volatile in case the flushing is asynchronous
  private volatile boolean open = true;
  // This need to be volatile because both the writer and the readers access it concurrently
  private volatile boolean markedForDeletion = false;

  Segment(
      final SegmentFile file,
      final SegmentDescriptor descriptor,
      final MappedByteBuffer buffer,
      final long lastWrittenAsqn,
      final JournalIndex index) {
    this.file = file;
    this.descriptor = descriptor;
    this.buffer = buffer;
    this.index = index;
    this.lastWrittenAsqn = lastWrittenAsqn;
    lastIndex = descriptor.lastIndex();
  }

  public long getLastWrittenAsqn() {
    return lastWrittenAsqn;
  }

  public long getLastIndex() {
    return lastIndex;
  }

  /**
   * Returns the segment ID.
   *
   * @return The segment ID.
   */
  long id() {
    return descriptor.id();
  }

  /**
   * Returns the segment's starting index.
   *
   * @return The segment's starting index.
   */
  long index() {
    return descriptor.index();
  }


  /**
   * Returns the segment file.
   *
   * @return The segment file.
   */
  SegmentFile file() {
    return file;
  }

  /**
   * Returns the segment descriptor.
   *
   * @return The segment descriptor.
   */
  SegmentDescriptor descriptor() {
    return descriptor;
  }

  /**
   * Creates a new segment reader.
   *
   * @return A new segment reader.
   */
  SegmentReader createReader() {
    checkOpen();
    final SegmentReader reader =
        new SegmentReader(buffer.asReadOnlyBuffer().position(0).order(ENDIANNESS), this, index);
    readers.add(reader);
    return reader;
  }

  /**
   * Removes the reader from this segment.
   *
   * @param reader the closed reader
   */
  void onReaderClosed(final SegmentReader reader) {
    readers.remove(reader);
    // When multiple readers are closed simultaneously, both readers might try to delete the file.
    // This is ok, as safeDelete is idempotent. Hence we keep it simple, and doesn't add more
    // concurrency control.
    if (markedForDeletion && readers.isEmpty()) {
      safeDelete();
    }
  }

  /** Checks whether the segment is open. */
  private void checkOpen() {
    checkState(open, "Segment not open");
  }

  /**
   * Returns a boolean indicating whether the segment is open.
   *
   * @return indicates whether the segment is open
   */
  boolean isOpen() {
    return open;
  }

  /** Closes the segment. */
  @Override
  public void close() {
    open = false;
    readers.forEach(SegmentReader::close);
    IoUtil.unmap(buffer);
  }

  private void safeDelete() {
    if (!readers.isEmpty()) {
      throw new JournalException(
          String.format(
              "Cannot delete segment file. There are %d readers referring to this segment.",
              readers.size()));
    }
    try {
      IoUtil.unmap(buffer);
      Files.deleteIfExists(file.getFileMarkedForDeletion());
    } catch (final IOException e) {
      LOG.warn(
          "Could not delete segment {}. File to delete {}. This can lead to increased disk usage.",
          this,
          file.getFileMarkedForDeletion(),
          e);
    }
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("id", id()).add("index", index()).toString();
  }

}
