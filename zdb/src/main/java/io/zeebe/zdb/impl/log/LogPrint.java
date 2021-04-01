/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb.impl.log;

import io.atomix.raft.storage.log.entry.InitializeEntry;
import io.atomix.raft.zeebe.ZeebeEntry;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.zeebe.engine.processor.RecordValues;
import io.zeebe.engine.processor.TypedEventImpl;
import io.zeebe.engine.processor.TypedEventRegistry;
import io.zeebe.logstreams.impl.log.LoggedEventImpl;
import io.zeebe.logstreams.storage.atomix.ZeebeIndexAdapter;
import io.zeebe.protocol.impl.record.RecordMetadata;
import io.zeebe.protocol.record.Record;
import io.zeebe.util.ReflectUtil;
import io.zeebe.zdb.LogPrintCommand.Format;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;

public class LogPrint {

  private final Format format;

  public LogPrint(final Format format) {
    this.format = format;
  }

  public String print(Path path) {
    final var zeebeLog = ZeebeLog.ofPath(path);

    final var startTime = System.currentTimeMillis();
    final var report = new StringBuilder("Scan log...").append(System.lineSeparator());
    final var scanner = new LogPrint.Scanner(report, format);

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
    private final Format format;
    private List<Record<?>> records = new ArrayList<>();

    Scanner(StringBuilder report, final Format format) {
      this.zeebeIndexAdapter = ZeebeIndexAdapter.ofDensity(1);
      this.report = report;
      this.format = format;
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

      int offset = 0;
      do {
        final var loggedEvent = new LoggedEventImpl();
        final var metadata = new RecordMetadata();

        loggedEvent.wrap(readBuffer, offset);
        loggedEvent.readMetadata(metadata);

        final var unifiedRecordValue =
            ReflectUtil.newInstance(TypedEventRegistry.EVENT_REGISTRY.get(metadata.getValueType()));
        loggedEvent.readValue(unifiedRecordValue);

        final var typedEvent = new TypedEventImpl(1);
        typedEvent.wrap(loggedEvent, metadata, unifiedRecordValue);

        switch (format) {
          case JSON:
            addToReport(typedEvent.toJson());
            break;
          case COMPACT:
            records.add(typedEvent);
            break;
          default:
            throw new IllegalArgumentException("Unsupported format type " + format.name());
        }

        offset += loggedEvent.getLength();
      } while (offset < readBuffer.capacity());
    }

    public String getReport() {
      report.append(System.lineSeparator()).append("Scanned entries: ").append(scannedEntries);

      switch (format) {
        case JSON:
          return report.toString();
        case COMPACT:
          return new CompactRecordLogger(records).format();
        default:
          throw new IllegalArgumentException("Unsupported format type " + format.name());
      }
    }
  }
}
