package io.zeebe.impl;

import io.atomix.raft.partition.impl.RaftNamespaces;
import io.atomix.raft.storage.log.RaftLog;
import io.atomix.raft.storage.log.entry.InitializeEntry;
import io.atomix.raft.zeebe.ZeebeEntry;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.zeebe.logstreams.storage.atomix.ZeebeIndexAdapter;
import java.nio.file.Path;

public final class LogScanner {

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String PARTITION_NAME_FORMAT = "raft-partition-partition-%d";

  public String scan(Path path) {
    final int partitionId;
    final String partitionName;

    try {
      partitionId = Integer.parseInt(path.getFileName().toString());
      partitionName = String.format(PARTITION_NAME_FORMAT, partitionId);
    } catch (NumberFormatException nfe) {
      final var errorMsg =
          String.format(
              "Expected to extract partition as integer from path, but path was '%s'.", path);
      throw new IllegalArgumentException(errorMsg, nfe);
    }

    final var resourceDir = path.toFile();

    final var startTime = System.currentTimeMillis();
    final var report = new StringBuilder("Scan log...").append(System.lineSeparator());
    final var scanner = new Scanner(report);

    // internally it scans the log
    RaftLog.builder()
        .withDirectory(resourceDir)
        .withName(partitionName)
        .withNamespace(RaftNamespaces.RAFT_STORAGE)
        .withMaxEntrySize(4 * 1024 * 1024)
        .withMaxSegmentSize(512 * 1024 * 1024)
        .withStorageLevel(StorageLevel.DISK)
        .withJournalIndexFactory(() -> scanner)
        .build();

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
    private long lastIndex = Long.MIN_VALUE;
    private long lastRecordPosition = Long.MIN_VALUE;
    private long lowestRecordPosition = Long.MAX_VALUE;
    private String lastInitEntry = "none";
    private boolean inconsistentLog = false;
    private double avgEntrySize = 0.0f;
    private double maxEntrySize = 0.0f;
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
        processInitialEntry(indexedEntry);
      } else if (indexedEntry.type() == ZeebeEntry.class) {
        processZeebeEntry(indexedEntry, currentIndex);
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

    private void processZeebeEntry(final Indexed indexedEntry, final long currentIndex) {
      final var zeebeEntry = (ZeebeEntry) indexedEntry.entry();

      final var highestPosition = zeebeEntry.highestPosition();
      final var lowestPosition = zeebeEntry.lowestPosition();

      if (lowestPosition > highestPosition) {
        report
            .append("Inconsistent ZeebeEntry lowestPosition")
            .append(lowestPosition)
            .append(" is higher than highestPosition ")
            .append(highestPosition)
            .append(" at index")
            .append(currentIndex)
            .append(System.lineSeparator());
      }

      if (lastRecordPosition > lowestPosition) {
        report
            .append("Inconsistent log lastRecordPosition")
            .append(lastRecordPosition)
            .append(" is higher than next lowestRecordPosition ")
            .append(lowestPosition)
            .append(" at index")
            .append(currentIndex)
            .append(System.lineSeparator());
      }

      lastRecordPosition = highestPosition;
      if (lowestRecordPosition > lowestPosition) {
        lowestRecordPosition = lowestPosition;
      }
    }

    private void processInitialEntry(final Indexed indexedEntry) {
      final var entry = (InitializeEntry) indexedEntry.entry();
      lastInitEntry = entry.toString();
    }

    private void processEntrySize(final Indexed indexedEntry) {
      final var currentSize = indexedEntry.size();
      if (currentSize > maxEntrySize) {
        maxEntrySize = currentSize;
      }
      avgEntrySize += currentSize;
      scannedEntries++;
    }

    private void processIndex(final long currentIndex) {
      if (lastIndex > currentIndex
          || (((lastIndex + 1) != currentIndex) && (lastIndex != Long.MIN_VALUE))) {
        inconsistentLog = true;
        report
            .append("Log is inconsistent at index ")
            .append(currentIndex)
            .append(" last index was ")
            .append(lastIndex)
            .append(System.lineSeparator());
      } else {
        lastIndex = currentIndex;
      }
    }

    public String getReport() {
      report
          .append(
              inconsistentLog
                  ? ANSI_RED + "LOG IS INCONSISTENT!" + ANSI_RESET
                  : ANSI_GREEN + "LOG IS CONSISTENT." + ANSI_RESET)
          .append(System.lineSeparator())
          .append("Scanned entries: ")
          .append(scannedEntries)
          .append(System.lineSeparator())
          .append("Maximum entry size: ")
          .append(maxEntrySize)
          .append(System.lineSeparator())
          .append("Avg entry size: ")
          .append(avgEntrySize / (double) scannedEntries)
          .append(System.lineSeparator())
          .append("LowestRecordPosition: ")
          .append(lowestRecordPosition)
          .append(System.lineSeparator())
          .append("LastRecordPosition: ")
          .append(lastRecordPosition)
          .append(System.lineSeparator())
          .append("LastIndex: ")
          .append(lastIndex)
          .append(System.lineSeparator())
          .append("LastInitialEntry: ")
          .append(lastInitEntry)
          .append(System.lineSeparator());

      return report.toString();
    }
  }
}
