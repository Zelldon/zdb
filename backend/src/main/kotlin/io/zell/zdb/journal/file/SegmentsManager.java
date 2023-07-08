/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb.journal.file;

import static com.google.common.base.Preconditions.checkNotNull;

import io.camunda.zeebe.journal.CorruptedJournalException;
import io.camunda.zeebe.journal.JournalException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentSkipListMap;

import io.zell.zdb.journal.ReadOnlyJournalMetaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Create new segments. Load existing segments from the disk. Keep track of all segments. */
final class SegmentsManager implements AutoCloseable {

  private static final long FIRST_SEGMENT_ID = 1;
  private static final long INITIAL_INDEX = 1;
  private static final long INITIAL_ASQN = SegmentedReadOnlyJournal.ASQN_IGNORE;

  private static final Logger LOG = LoggerFactory.getLogger(SegmentsManager.class);

  private final NavigableMap<Long, Segment> segments = new ConcurrentSkipListMap<>();
  private final JournalIndex journalIndex;
  private final int maxSegmentSize;
  private final File directory;
  private final String name;
  private final ReadOnlyJournalMetaStore metaStore;

  private volatile Segment currentSegment;

  SegmentsManager(
      final JournalIndex journalIndex,
      final int maxSegmentSize,
      final File directory,
      final String name,
      final ReadOnlyJournalMetaStore metaStore) {
    this.name = checkNotNull(name, "name cannot be null");
    this.journalIndex = journalIndex;
    this.maxSegmentSize = maxSegmentSize;
    this.directory = directory;
    this.metaStore = metaStore;
  }

  @Override
  public void close() {
    segments
        .values()
        .forEach(
            segment -> {
              LOG.debug("Closing segment: {}", segment);
              segment.close();
            });

    currentSegment = null;
  }

  Segment getCurrentSegment() {
    return currentSegment;
  }

  Segment getFirstSegment() {
    final Map.Entry<Long, Segment> segment = segments.firstEntry();
    return segment != null ? segment.getValue() : null;
  }

  Segment getLastSegment() {
    final Map.Entry<Long, Segment> segment = segments.lastEntry();
    return segment != null ? segment.getValue() : null;
  }

  Segment getNextSegment(final long index) {
    final Map.Entry<Long, Segment> nextSegment = segments.higherEntry(index);
    return nextSegment != null ? nextSegment.getValue() : null;
  }

  Segment getSegment(final long index) {
    // Check if the current segment contains the given index first in order to prevent an
    // unnecessary map lookup.
    if (currentSegment != null && index > currentSegment.index()) {
      return currentSegment;
    }

    // If the index is in another segment, get the entry with the next lowest first index.
    final Map.Entry<Long, Segment> segment = segments.floorEntry(index);
    if (segment != null) {
      return segment.getValue();
    }
    return getFirstSegment();
  }

  private long getFirstIndex() {
    final var firstSegment = getFirstSegment();
    return firstSegment != null ? firstSegment.index() : 0;
  }


  /** Loads existing segments from the disk */
  void open() {
    // Load existing log segments from disk.
    for (final Segment segment : loadSegments()) {
      segments.put(segment.descriptor().index(), segment);
    }

    // If a segment doesn't already exist, create an initial segment starting at index 1.
    if (!segments.isEmpty()) {
      currentSegment = segments.lastEntry().getValue();
    } else {
      throw new IllegalStateException("Expected to read segments, but there was nothing to read.");
    }
  }


  Collection<Segment> getTailSegments(final long index) {
    final var segment = getSegment(index);
    if (segment == null) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSortedMap(segments.tailMap(segment.index(), true)).values();
  }

  /**
   * Loads all segments from disk.
   *
   * @return A collection of segments for the log.
   */
  private Collection<Segment> loadSegments() {
    final var lastFlushedIndex = metaStore.loadLastFlushedIndex();

    // Ensure log directories are created.
    //noinspection ResultOfMethodCallIgnored
    directory.mkdirs();
    final List<Segment> segments = new ArrayList<>();

    final List<File> files = getSortedLogSegments();
    Segment previousSegment = null;
    for (int i = 0; i < files.size(); i++) {
      final File file = files.get(i);

      try {
        LOG.debug("Found segment file: {}", file.getName());
        final Segment segment =
            loadExistingSegment(
                file.toPath(),
                previousSegment != null ? previousSegment.getLastWrittenAsqn() : INITIAL_ASQN,
                journalIndex);

        segments.add(segment);
        previousSegment = segment;
      } catch (final CorruptedJournalException e) {
        throw e;
      }
    }

    return segments;
  }

  private SegmentDescriptor readDescriptor(final Path file) {
    final var fileName = file.getFileName().toString();

    try (final FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      final var fileSize = Files.size(file);
      final byte version = readVersion(channel, fileName);
      final int length = SegmentDescriptor.getEncodingLengthForVersion(version);
      if (fileSize < length) {
        throw new CorruptedJournalException(
                String.format(
                        "Expected segment '%s' with version %d to be at least %d bytes long but it only has %d.",
                        fileName, version, length, fileSize));
      }

      final ByteBuffer buffer = ByteBuffer.allocate(length);
      final int readBytes = channel.read(buffer, 0);

      if (readBytes != -1 && readBytes < length) {
        throw new JournalException(
                String.format(
                        "Expected to read %d bytes of segment '%s' with %d version but only read %d bytes.",
                        length, fileName, version, readBytes));
      }

      buffer.flip();
      return new SegmentDescriptor(buffer);
    } catch (final IndexOutOfBoundsException e) {
      throw new JournalException(
              String.format(
                      "Expected to read descriptor of segment '%s', but nothing was read.", fileName),
              e);
    } catch (final IOException e) {
      throw new JournalException(e);
    }
  }

  private byte readVersion(final FileChannel channel, final String fileName) throws IOException {
    final ByteBuffer buffer = ByteBuffer.allocate(1);
    final int readBytes = channel.read(buffer);

    if (readBytes == 0) {
      throw new JournalException(
              String.format(
                      "Expected to read the version byte from segment '%s' but nothing was read.",
                      fileName));
    } else if (readBytes == -1) {
      throw new CorruptedJournalException(
              String.format(
                      "Expected to read the version byte from segment '%s' but got EOF instead.",
                      fileName));
    }

    return buffer.get(0);
  }
  private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

  private MappedByteBuffer mapSegment(final FileChannel channel, final long segmentSize)
          throws IOException {
    final var mappedSegment = channel.map(FileChannel.MapMode.READ_WRITE, 0, segmentSize);
    mappedSegment.order(ENDIANNESS);

    return mappedSegment;
  }
  Segment loadExistingSegment(
          final Path segmentFile, final long lastWrittenAsqn, final JournalIndex journalIndex) {
    final var descriptor = readDescriptor(segmentFile);
    final MappedByteBuffer mappedSegment;

    try (final var channel =
                 FileChannel.open(segmentFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      mappedSegment = mapSegment(channel, descriptor.maxSegmentSize());
    } catch (final IOException e) {
      throw new JournalException(
              String.format("Failed to load existing segment %s", segmentFile), e);
    }

    return loadSegment(segmentFile, mappedSegment, descriptor, lastWrittenAsqn, journalIndex);
  }

  /* ---- Internal methods ------ */
  private Segment loadSegment(
          final Path file,
          final MappedByteBuffer buffer,
          final SegmentDescriptor descriptor,
          final long lastWrittenAsqn,
          final JournalIndex journalIndex) {
    final SegmentFile segmentFile = new SegmentFile(file.toFile());
    return new Segment(segmentFile, descriptor, buffer, lastWrittenAsqn, journalIndex);
  }


  /** Returns an array of valid log segments sorted by their id which may be empty but not null. */
  private List<File> getSortedLogSegments() {
    final File[] files =
        directory.listFiles(file -> file.isFile() && SegmentFile.isSegmentFile(name, file));

    if (files == null) {
      throw new IllegalStateException(
          String.format(
              "Could not list files in directory '%s'. Either the path doesn't point to a directory or an I/O error occurred.",
              directory));
    }

    Arrays.sort(files, Comparator.comparingInt(f -> SegmentFile.getSegmentIdFromPath(f.getName())));

    return Arrays.asList(files);
  }
}
