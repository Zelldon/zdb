/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
///*
// * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
// * one or more contributor license agreements. See the NOTICE file distributed
// * with this work for additional information regarding copyright ownership.
// * Licensed under the Zeebe Community License 1.1. You may not use this file
// * except in compliance with the Zeebe Community License 1.1.
// */
//package io.zeebe.zdb.impl.log;
//
//import io.atomix.raft.storage.log.RaftLogReader;
//import io.atomix.raft.zeebe.ZeebeEntry;
//import io.atomix.storage.journal.JournalReader.Mode;
//import java.nio.file.Path;
//
//public final class LogConsistencyCheck {
//
//  private static final String ANSI_RESET = "\u001B[0m";
//  private static final String ANSI_GREEN = "\u001B[32m";
//  private static final String ANSI_RED = "\u001B[31m";
//
//  public String consistencyCheck(Path path) {
//    final var zeebeLog = ZeebeLog.ofPath(path);
//    final var resourceDir = path.toFile();
//
//    final var report =
//        new StringBuilder("Opening log ").append(resourceDir).append(System.lineSeparator());
//
//    // internally it scans the log
//    final var raftLog = zeebeLog.openLog();
//    final var raftLogReader = raftLog.openReader(-1, Mode.ALL);
//
//    final var startTime = System.currentTimeMillis();
//    new Scanner(raftLogReader, report).consistencyCheck();
//    final var endTime = System.currentTimeMillis();
//    report
//        .append("Log checked in ")
//        .append(endTime - startTime)
//        .append(" ms")
//        .append(System.lineSeparator());
//
//    return report.toString();
//  }
//
//  private static class Scanner {
//
//    private final RaftLogReader logReader;
//    private final StringBuilder report;
//
//    private long lastIndex = Long.MIN_VALUE;
//    private long lastRecordPosition = Long.MIN_VALUE;
//    private long lowestRecordPosition = Long.MAX_VALUE;
//    private boolean inconsistentLog = false;
//    private int scannedEntries = 0;
//
//    Scanner(final RaftLogReader logReader, final StringBuilder report) {
//      this.logReader = logReader;
//      this.report = report;
//    }
//
//    void consistencyCheck() {
//      logReader.reset();
//
//      while (logReader.hasNext()) {
//        final var indexedEntry = logReader.next();
//
//        final var currentIndex = indexedEntry.index();
//        processIndex(currentIndex);
//
//        if (indexedEntry.type() == ZeebeEntry.class) {
//          processZeebeEntry((ZeebeEntry) indexedEntry.entry(), currentIndex);
//        }
//
//        scannedEntries++;
//      }
//
//      addReport();
//    }
//
//    private void processIndex(final long currentIndex) {
//      if (lastIndex > currentIndex
//          || (((lastIndex + 1) != currentIndex) && (lastIndex != Long.MIN_VALUE))) {
//        inconsistentLog = true;
//        report
//            .append("Log is inconsistent at index ")
//            .append(currentIndex)
//            .append(" last index was ")
//            .append(lastIndex)
//            .append(System.lineSeparator());
//      } else {
//        lastIndex = currentIndex;
//      }
//    }
//
//    private void processZeebeEntry(final ZeebeEntry zeebeEntry, final long currentIndex) {
//      final var highestPosition = zeebeEntry.highestPosition();
//      final var lowestPosition = zeebeEntry.lowestPosition();
//
//      if (lowestPosition > highestPosition) {
//        report
//            .append("Inconsistent ZeebeEntry lowestPosition")
//            .append(lowestPosition)
//            .append(" is higher than highestPosition ")
//            .append(highestPosition)
//            .append(" at index")
//            .append(currentIndex)
//            .append(System.lineSeparator());
//      }
//
//      if (lastRecordPosition > lowestPosition) {
//        inconsistentLog = true;
//        report
//            .append("Inconsistent log lastRecordPosition '")
//            .append(lastRecordPosition)
//            .append("' is higher than next lowestRecordPosition '")
//            .append(lowestPosition)
//            .append("' at index: '")
//            .append(currentIndex)
//            .append("'")
//            .append(System.lineSeparator());
//      }
//
//      lastRecordPosition = highestPosition;
//      if (lowestRecordPosition > lowestPosition) {
//        lowestRecordPosition = lowestPosition;
//      }
//    }
//
//    private void addReport() {
//      report
//          .append(
//              inconsistentLog
//                  ? ANSI_RED + "LOG IS INCONSISTENT!" + ANSI_RESET
//                  : ANSI_GREEN + "LOG IS CONSISTENT." + ANSI_RESET)
//          .append(System.lineSeparator())
//          .append("Scanned entries: ")
//          .append(scannedEntries)
//          .append(System.lineSeparator());
//    }
//  }
//}
