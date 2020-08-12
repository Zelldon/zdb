package io.zeebe.impl;

import io.atomix.raft.partition.impl.RaftNamespaces;
import io.atomix.raft.storage.log.RaftLog;
import io.atomix.raft.zeebe.ZeebeLogAppender;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.JournalReader.Mode;
import io.zeebe.logstreams.impl.log.LogStreamBuilderImpl;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.logstreams.log.LogStreamReader;
import io.zeebe.logstreams.storage.atomix.AtomixLogStorage;
import io.zeebe.logstreams.storage.atomix.ZeebeIndexAdapter;
import io.zeebe.util.sched.Actor;
import io.zeebe.util.sched.ActorScheduler;
import io.zeebe.util.sched.ActorScheduler.ActorSchedulerBuilder;
import io.zeebe.util.sched.future.ActorFuture;
import io.zeebe.util.sched.future.CompletableActorFuture;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

public final class LogScanner {

  public String scan(Path pathToLog) {
    // setup
    final var actorScheduler = new ActorSchedulerBuilder().build();
    actorScheduler.start();

    final var logScanActor = new LogScanActor(actorScheduler, pathToLog);
    actorScheduler.submitActor(logScanActor).join();

    return logScanActor.scan().join();
  }

  private static class LogScanActor extends Actor {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String PARTITION_NAME_FORMAT = "raft-partition-partition-%d";

    private final Path path;
    private final int partitionId;
    private final String partitionName;
    private final ActorScheduler actorScheduler;
    private ActorFuture<LogStream> logStreamFuture;

    LogScanActor(final ActorScheduler actorScheduler, final Path path) {
      this.actorScheduler = actorScheduler;
      this.path = path;

      try {
        this.partitionId = Integer.parseInt(path.getFileName().toString());
        this.partitionName = String.format(PARTITION_NAME_FORMAT, partitionId);
      } catch (NumberFormatException nfe) {
        final var errorMsg =
            String.format(
                "Expected to extract partition as integer from path, but path was '%s'.", path);
        throw new IllegalArgumentException(errorMsg, nfe);
      }
    }

    @Override
    protected void onActorStarting() {
      final var resourceDir = path.toFile();

      final var startTime = System.currentTimeMillis();
      final var raftLog =
          RaftLog.builder()
              .withDirectory(resourceDir)
              .withName(partitionName)
              .withNamespace(RaftNamespaces.RAFT_STORAGE)
              .withMaxEntrySize(4 * 1024 * 1024)
              .withMaxSegmentSize(512 * 1024 * 1024)
              .withStorageLevel(StorageLevel.DISK)
              .build();

      final var endtime = System.currentTimeMillis();
      System.out.println("Log opened in " + (endtime - startTime) + " ms");

      final var atomixLogStorage =
          new AtomixLogStorage(
              ZeebeIndexAdapter.ofDensity(1),
              (idx, mode) -> raftLog.openReader(idx, Mode.ALL),
              () -> Optional.of(new NoopAppender()));

      logStreamFuture =
          new LogStreamBuilderImpl()
              .withActorScheduler(actorScheduler)
              .withLogStorage(atomixLogStorage)
              .withLogName(partitionName)
              .withPartitionId(partitionId)
              .buildAsync();
    }

    CompletableActorFuture<String> scan() {
      final var future = new CompletableActorFuture<String>();

      actor.call(
          () ->
              logStreamFuture.onComplete(
                  (logStream, t) -> {
                    if (t == null) {
                      logStream
                          .newLogStreamReader()
                          .onComplete(
                              (reader, t2) -> {
                                if (t2 == null) {
                                  future.complete(scanLog(reader));
                                } else {
                                  future.completeExceptionally(t2);
                                }
                              });
                    } else {
                      future.completeExceptionally(t);
                    }
                  }));
      return future;
    }

    private String scanLog(LogStreamReader reader) {
      System.out.println("Scan log...");
      reader.seekToFirstEvent();

      final var validationContext = new ValidationContext();

      while (reader.hasNext()) {
        final var next = reader.next();
        final var position = next.getPosition();

        validationContext.onNextPosition(position);
      }

      System.out.println("Scan finished");

      return validationContext.finishValidation();
    }

    private static class ValidationContext {

      long low = Long.MAX_VALUE;
      long high = Long.MIN_VALUE;
      long lastPosition = 0;
      int eventCount = 0;
      boolean inconsistentLog = false;

      void onNextPosition(long position) {

        if (lastPosition > position) {
          inconsistentLog = true;
          onInconsistentLog(low, high, lastPosition, eventCount, position);
        }

        if (position < low) {
          low = position;
        } else if (position > high) {
          high = position;
        }

        lastPosition = position;
        eventCount++;
      }

      String finishValidation() {
        final var stringBuilder = new StringBuilder();
        if (inconsistentLog) {
          stringBuilder.append(ANSI_RED + "LOG IS INCONSISTENT!" + ANSI_RESET);
        } else {
          stringBuilder.append(ANSI_GREEN + "LOG IS CONSISTENT." + ANSI_RESET);
        }

        stringBuilder.append("Last position: ").append(lastPosition).append(System.lineSeparator());
        stringBuilder.append("Lowest position: ").append(low).append(System.lineSeparator());
        stringBuilder.append("Highest position: ").append(high).append(System.lineSeparator());
        stringBuilder.append("Event count: ").append(eventCount).append(System.lineSeparator());

        return stringBuilder.toString();
      }
//
//      private static void onInconsistentLog(
//          long low, long high, long lastPosition, int eventCount, long position) {
//        System.out.println("===============");
//        System.out.println("At idx " + eventCount);
//        System.out.print("Current position " + position);
//        System.out.print(
//            " (Segment id " + (position >> 32) + " segment offset " + (int) position + ')');
//        System.out.println();
//        System.out.print("Is smaller then this last position " + lastPosition);
//        System.out.print(
//            " (Segment id " + (lastPosition >> 32) + " segment offset " + (int) lastPosition + ')');
//        System.out.println();
//        System.out.println("Current lowest " + low + " current highest " + high);
//        System.out.println("===============");
//      }
    }
  }

  private static class NoopAppender implements ZeebeLogAppender {
    @Override
    public void appendEntry(
        long l, long l1, ByteBuffer byteBuffer, AppendListener appendListener) {}
  }
}
