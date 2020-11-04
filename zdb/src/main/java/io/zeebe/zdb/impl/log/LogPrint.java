/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.zdb.impl.log;

import io.atomix.raft.storage.log.entry.InitializeEntry;
import io.atomix.raft.zeebe.ZeebeEntry;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.zeebe.engine.processor.RecordValues;
import io.zeebe.engine.processor.TypedEventImpl;
import io.zeebe.logstreams.impl.log.LoggedEventImpl;
import io.zeebe.logstreams.storage.atomix.ZeebeIndexAdapter;
import io.zeebe.protocol.impl.record.RecordMetadata;
import java.nio.file.Path;
import org.agrona.concurrent.UnsafeBuffer;

public class LogPrint {

  public String print(Path path) {
    final var zeebeLog = ZeebeLog.ofPath(path);

    final var startTime = System.currentTimeMillis();
    final var report = new StringBuilder("Scan log...").append(System.lineSeparator());
    final var scanner = new LogPrint.Scanner(report);

    // internally it scans the log
    zeebeLog.openLog(builder -> builder.withJournalIndexFactory(() -> scanner));

    final var endTime = System.currentTimeMillis();
    report
        .append("Log scanned in ")
        .append(endTime - startTime)
        .append(" ms")
        .append(System.lineSeparator());

    return scanner.getReport();
  }

  private static class Scanner implements JournalIndex {

    private static final RecordValues RECORD_VALUES = new RecordValues();
    private final ZeebeIndexAdapter zeebeIndexAdapter;
    private final StringBuilder report;
    private int scannedEntries = 0;

    Scanner(StringBuilder report) {
      this.zeebeIndexAdapter = ZeebeIndexAdapter.ofDensity(1);
      this.report = report;
    }

    @Override
    public void index(final Indexed indexedEntry, final int position) {
      scannedEntries++;
      if (indexedEntry.type() == InitializeEntry.class) {
        final var entry = (InitializeEntry) indexedEntry.entry();
        addToReport(entry.toString());
      } else if (indexedEntry.type() == ZeebeEntry.class) {
        processEntry((ZeebeEntry) indexedEntry.entry());
      }

      // delegate
      zeebeIndexAdapter.index(indexedEntry, position);
    }

    @Override
    public Position lookup(final long index) {
      return zeebeIndexAdapter.lookup(index);
    }

    @Override
    public void truncate(final long index) {
      zeebeIndexAdapter.truncate(index);
    }

    @Override
    public void compact(final long index) {
      zeebeIndexAdapter.compact(index);
    }

    private void addToReport(String newEntry) {
      report.append(newEntry).append(System.lineSeparator());
    }

    private void processEntry(final ZeebeEntry entry) {
      final var readBuffer = new UnsafeBuffer(entry.data());
      final var loggedEvent = new LoggedEventImpl();
      final var metadata = new RecordMetadata();

      int offset = 0;
      do {
        loggedEvent.wrap(readBuffer, offset);
        loggedEvent.readMetadata(metadata);

        final var unifiedRecordValue =
            RECORD_VALUES.readRecordValue(loggedEvent, metadata.getValueType());
        final var typedEvent = new TypedEventImpl(1);
        typedEvent.wrap(loggedEvent, metadata, unifiedRecordValue);
        addToReport(typedEvent.toJson());

        offset += loggedEvent.getLength();
      } while (offset < readBuffer.capacity());
    }

    public String getReport() {
      report.append(System.lineSeparator()).append("Scanned entries: ").append(scannedEntries);

      return report.toString();
    }
  }
}
