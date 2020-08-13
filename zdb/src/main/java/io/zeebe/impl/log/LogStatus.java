package io.zeebe.impl.log;

import io.atomix.raft.storage.log.entry.InitializeEntry;
import io.atomix.raft.zeebe.ZeebeEntry;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.zeebe.logstreams.storage.atomix.ZeebeIndexAdapter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LogStatus {

  public String scan(Path path) {
    final var zeebeLog = ZeebeLog.ofPath(path);

    final var startTime = System.currentTimeMillis();
    final var report = new StringBuilder("Scan log...").append(System.lineSeparator());
    final var scanner = new Scanner(report);

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

    private final ZeebeIndexAdapter zeebeIndexAdapter;
    private final StringBuilder report;
    private final List<String> initialEntries = new ArrayList<>();

    private long highestIndex = Long.MIN_VALUE;
    private long lowestIndex = Long.MAX_VALUE;
    private long highestRecordPosition = Long.MIN_VALUE;
    private long lowestRecordPosition = Long.MAX_VALUE;

    private double avgEntrySize = 0.0f;
    private int maxEntrySize = Integer.MIN_VALUE;
    private int minEntrySize = Integer.MAX_VALUE;
    private int scannedEntries = 0;

    Scanner(StringBuilder report) {
      this.zeebeIndexAdapter = ZeebeIndexAdapter.ofDensity(1);
      this.report = report;
    }

    @Override
    public void index(final Indexed indexedEntry, final int position) {
      // check index
      final var currentIndex = indexedEntry.index();
      processIndex(currentIndex);

      // entry size
      processEntrySize(indexedEntry);

      if (indexedEntry.type() == InitializeEntry.class) {
        processInitialEntry((InitializeEntry) indexedEntry.entry());
      } else if (indexedEntry.type() == ZeebeEntry.class) {
        processZeebeEntry((ZeebeEntry) indexedEntry.entry());
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

    private void processZeebeEntry(final ZeebeEntry zeebeEntry) {
      final var highestPosition = zeebeEntry.highestPosition();
      final var lowestPosition = zeebeEntry.lowestPosition();

      if (highestPosition > highestRecordPosition) {
        highestRecordPosition = highestPosition;
      }

      if (lowestPosition < lowestRecordPosition) {
        lowestRecordPosition = lowestPosition;
      }
    }

    private void processInitialEntry(final InitializeEntry initializeEntry) {
      initialEntries.add(initializeEntry.toString());
    }

    private void processEntrySize(final Indexed indexedEntry) {
      final var currentSize = indexedEntry.size();

      if (currentSize > maxEntrySize) {
        maxEntrySize = currentSize;
      }

      if (currentSize < minEntrySize) {
        minEntrySize = currentSize;
      }
      avgEntrySize += currentSize;
      scannedEntries++;
    }

    private void processIndex(final long currentIndex) {
      if (currentIndex > highestIndex) {
        highestIndex = currentIndex;
      }

      if (currentIndex < lowestIndex) {
        lowestIndex = currentIndex;
      }
    }

    public String getReport() {
      report
          .append(System.lineSeparator())
          .append("Scanned entries: ")
          .append(scannedEntries)
          .append(System.lineSeparator())
          .append("Maximum entry size: ")
          .append(maxEntrySize)
          .append(System.lineSeparator())
          .append("Minimum entry size: ")
          .append(minEntrySize)
          .append(System.lineSeparator())
          .append("Avg entry size: ")
          .append(avgEntrySize / (double) scannedEntries)
          .append(System.lineSeparator())
          .append("LowestRecordPosition: ")
          .append(lowestRecordPosition)
          .append(System.lineSeparator())
          .append("HighestRecordPosition: ")
          .append(highestRecordPosition)
          .append(System.lineSeparator())
          .append("HighestIndex: ")
          .append(highestIndex)
          .append(System.lineSeparator())
          .append("LowestIndex: ")
          .append(lowestIndex)
          .append(System.lineSeparator())
          .append("InitialEntries: ")
          .append(
              initialEntries.isEmpty()
                  ? "No initial entries in the log."
                  : Arrays.toString(initialEntries.toArray(new String[0])))
          .append(System.lineSeparator());

      return report.toString();
    }
  }
}
