package io.zeebe.impl.log;

import io.atomix.raft.partition.impl.RaftNamespaces;
import io.atomix.raft.storage.log.RaftLog;
import io.atomix.raft.storage.log.RaftLog.Builder;
import io.atomix.storage.StorageLevel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.UnaryOperator;

final class ZeebeLog {

  private static final String PARTITION_NAME_FORMAT = "raft-partition-partition-%d";
  private static final String ERROR_MSG_PATH_NOT_VALID =
      "Expected that given path '%s' point's to a partition log directory, but doesnt exist.";

  private String partitionName;
  private final File resourceDir;

  private ZeebeLog(Path path) {
    if (!path.toFile().exists()) {
      final var errorMsg = String.format(ERROR_MSG_PATH_NOT_VALID, path);
      throw new IllegalArgumentException(errorMsg);
    }

    if (!path.toFile().isDirectory()) {
      final var errorMsg = String.format(ERROR_MSG_PATH_NOT_VALID, path);
      throw new IllegalArgumentException(errorMsg);
    }

    resourceDir = path.toFile();
    try {
      final int partitionId = Integer.parseInt(path.getFileName().toString());
      partitionName = String.format(PARTITION_NAME_FORMAT, partitionId);
    } catch (NumberFormatException nfe) {
      final var errorMsg =
          String.format(
              "Expected to extract partition as integer from path, but path was '%s'.", path);
      throw new IllegalArgumentException(errorMsg, nfe);
    }
  }

  public static ZeebeLog ofPath(Path path) {
    return new ZeebeLog(path);
  }

  public RaftLog openLog() {
    return openLog(UnaryOperator.identity());
  }

  public RaftLog openLog(UnaryOperator<Builder> configurator) {
    try {
      final var builder =
          RaftLog.builder()
              .withDirectory(resourceDir)
              .withName(partitionName)
              .withNamespace(RaftNamespaces.RAFT_STORAGE)
              .withMaxEntrySize(4 * 1024 * 1024)
              .withMaxSegmentSize(512 * 1024 * 1024)
              .withStorageLevel(StorageLevel.DISK);

      return configurator.apply(builder).build();
    } catch (Exception ex) {
      final var collect =
          Arrays.stream(Objects.requireNonNull(resourceDir.listFiles()))
              .filter(File::isFile)
              .filter(
                  file -> {
                    try {
                      return new BufferedReader(new FileReader(file)).readLine() == null;
                    } catch (IOException e) {
                      e.printStackTrace();
                      return false;
                    }
                  })
              .toArray();
      System.err.println("Find empty files " + Arrays.toString(collect));
      throw ex;
    }
  }
}
