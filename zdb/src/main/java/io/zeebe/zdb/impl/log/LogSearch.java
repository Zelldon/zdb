/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb.impl.log;

import io.atomix.raft.storage.log.RaftLogReader;
import io.atomix.raft.zeebe.ZeebeEntry;
import io.atomix.storage.journal.JournalReader.Mode;
import io.zeebe.engine.processor.RecordValues;
import io.zeebe.engine.processor.TypedEventImpl;
import io.zeebe.logstreams.impl.log.LoggedEventImpl;
import io.zeebe.protocol.impl.record.RecordMetadata;
import java.nio.file.Path;
import org.agrona.concurrent.UnsafeBuffer;

public final class LogSearch {

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_RED = "\u001B[31m";

  public String searchForPosition(Path path, long position) {
    final var zeebeLog = ZeebeLog.ofPath(path);
    final var resourceDir = path.toFile();

    final var report =
        new StringBuilder("Searching log ").append(resourceDir).append(System.lineSeparator());

    // internally it scans the log
    final var raftLog = zeebeLog.openLog();
    final var raftLogReader = raftLog.openReader(-1, Mode.ALL);

    final var startTime = System.currentTimeMillis();
    new PositionSearcher(raftLogReader, report).searchPosition(position);
    final var endTime = System.currentTimeMillis();
    report
        .append("Searched log in ")
        .append(endTime - startTime)
        .append(" ms")
        .append(System.lineSeparator());

    return report.toString();
  }

  public String searchForIndex(Path path, long index) {
    final var zeebeLog = ZeebeLog.ofPath(path);
    final var resourceDir = path.toFile();

    final var report =
        new StringBuilder("Searching log ").append(resourceDir).append(System.lineSeparator());

    // internally it scans the log
    final var raftLog = zeebeLog.openLog();
    final var raftLogReader = raftLog.openReader(-1, Mode.ALL);

    final var startTime = System.currentTimeMillis();
    new IndexSearcher(raftLogReader, report).searchIndex(index);
    final var endTime = System.currentTimeMillis();
    report
        .append("Searched log in ")
        .append(endTime - startTime)
        .append(" ms")
        .append(System.lineSeparator());

    return report.toString();
  }

  private static class IndexSearcher {

    private final RaftLogReader logReader;
    private final StringBuilder report;

    private int scannedEntries = 0;
    private long lastIndex;

    IndexSearcher(final RaftLogReader logReader, final StringBuilder report) {
      this.logReader = logReader;
      this.report = report;
    }

    void searchIndex(final long searchIndex) {
      logReader.reset();

      while (logReader.hasNext()) {
        final var indexedEntry = logReader.next();
        final var currentIndex = indexedEntry.index();

        if (searchIndex < currentIndex) {
          // doesnt exist in log
          report
              .append(ANSI_RED)
              .append("Searched index '")
              .append(searchIndex)
              .append("' doesn't exist in log.")
              .append(ANSI_RESET)
              .append(System.lineSeparator())
              .append("First index in the log is '")
              .append(currentIndex)
              .append("'.")
              .append(System.lineSeparator());
          return;
        }

        if (searchIndex == currentIndex) {
          // found
          report
              .append(ANSI_GREEN)
              .append("Found entry with index '")
              .append(searchIndex)
              .append("'")
              .append(ANSI_RESET)
              .append(System.lineSeparator())
              .append("Indexed Entry: ")
              .append(indexedEntry.toString())
              .append(System.lineSeparator());
          return;
        }

        lastIndex = currentIndex;
        scannedEntries++;
      }

      report
          .append(ANSI_RED)
          .append("Reached end of the log, scanned ")
          .append(scannedEntries)
          .append(" indexed entries.")
          .append(System.lineSeparator())
          .append("Didn't found given index '")
          .append(searchIndex)
          .append("', last index was '")
          .append(lastIndex)
          .append("'.")
          .append(ANSI_RESET)
          .append(System.lineSeparator());
    }
  }

  private static class PositionSearcher {
    private static final RecordValues RECORD_VALUES = new RecordValues();

    private final RaftLogReader logReader;
    private final StringBuilder report;

    private long lastRecordPosition = Long.MIN_VALUE;
    private int scannedEntries = 0;

    PositionSearcher(final RaftLogReader logReader, final StringBuilder report) {
      this.logReader = logReader;
      this.report = report;
    }

    void searchPosition(final long searchPosition) {
      logReader.reset();

      while (logReader.hasNext()) {
        final var indexedEntry = logReader.next();
        final var currentIndex = indexedEntry.index();

        if (indexedEntry.type() == ZeebeEntry.class) {
          final var entry = (ZeebeEntry) indexedEntry.entry();
          final var highestPosition = entry.highestPosition();

          if (highestPosition >= searchPosition) {
            final var lowestPosition = entry.lowestPosition();
            if (lowestPosition <= searchPosition) {
              report
                  .append("Searched for record position '")
                  .append(searchPosition)
                  .append("' and found entry on index ")
                  .append(currentIndex)
                  .append(" with lowestPosition ")
                  .append(lowestPosition)
                  .append(" and highestPosition ")
                  .append(highestPosition)
                  .append(System.lineSeparator());
              processEntry(searchPosition, entry);
            } else {
              report
                  .append(ANSI_RED)
                  .append("Were not able to find given record position '")
                  .append(searchPosition)
                  .append("'")
                  .append(ANSI_RESET)
                  .append(System.lineSeparator())
                  .append("Last seen entry on index ")
                  .append(currentIndex)
                  .append(" contained as lowest position ")
                  .append(lowestPosition)
                  .append(" and as highest record position ")
                  .append(highestPosition)
                  .append(System.lineSeparator());
            }
            return;
          }

          lastRecordPosition = highestPosition;
        }

        scannedEntries++;
      }

      report
          .append(ANSI_RED)
          .append("Reached end of the log, scanned ")
          .append(scannedEntries)
          .append(" indexed entries.")
          .append(System.lineSeparator())
          .append("Didn't found given record position '")
          .append(searchPosition)
          .append("', last record position was '")
          .append(lastRecordPosition)
          .append("'.")
          .append(ANSI_RESET)
          .append(System.lineSeparator());
    }

    private void processEntry(final long searchPosition, final ZeebeEntry entry) {

      var lastPosition = 0L;
      final var readBuffer = new UnsafeBuffer(entry.data());
      final var loggedEvent = new LoggedEventImpl();
      final var metadata = new RecordMetadata();

      int offset = 0;
      do {
        loggedEvent.wrap(readBuffer, offset);
        final var position = loggedEvent.getPosition();
        if (searchPosition == position) {
          loggedEvent.readMetadata(metadata);

          final var unifiedRecordValue =
              RECORD_VALUES.readRecordValue(loggedEvent, metadata.getValueType());
          final var typedEvent = new TypedEventImpl(1);
          typedEvent.wrap(loggedEvent, metadata, unifiedRecordValue);
          report.append("Found: ").append(typedEvent.toJson()).append(System.lineSeparator());
          return;
        }
        lastPosition = position;
        offset += loggedEvent.getLength();
      } while (offset < readBuffer.capacity());

      report
          .append(ANSI_RED)
          .append("Was not able to find the given position in the index entry!")
          .append("Last position was ")
          .append(lastPosition)
          .append(ANSI_RESET)
          .append(System.lineSeparator());
    }
  }
}
