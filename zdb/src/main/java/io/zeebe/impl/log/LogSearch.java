package io.zeebe.impl.log;

import io.atomix.raft.storage.log.RaftLogReader;
import io.atomix.raft.zeebe.ZeebeEntry;
import io.atomix.storage.journal.JournalReader.Mode;
import java.nio.file.Path;

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
              report.append("Index ").append(currentIndex).append(System.lineSeparator());
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
  }
}
