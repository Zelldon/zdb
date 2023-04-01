/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import io.zell.zdb.state.Experimental;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// THIS IS IN ALPHA STATE
@Command(
    name = "state",
    mixinStandardHelpOptions = true,
    description = "Prints general information of the internal state")
public class StateCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true)
  private Path partitionPath;

  /**
   * Alpha feature: Planned to replace old status call
   *
   * @return the status code of the call
   */
  @Override
  public Integer call() {
    final var jsonString = new Experimental(partitionPath).stateStatisticsAsJsonString();
    System.out.println(jsonString);
    return 0;
  }

  /** Alpha feature: Planned to replace old specific status calls */
  @Command(name = "list", description = "List column families and the values as json")
  public int list(
      @Option(
              names = {"-cf", "--columnFamily"},
              paramLabel = "COLUMNFAMILY",
              description = "The column family name to filter for")
          final String columnFamilyName) {
    System.out.print("{\"data\":[");
    final var experimental = new Experimental(partitionPath);
    final var counter = new AtomicInteger(0);
    experimental.visitDBWithJsonValues(
        ((cf, key, valueJson) -> {
          if (columnFamilyName == null
              || columnFamilyName.isEmpty()
              || cf.toString().equals(columnFamilyName)) {
            if (counter.getAndIncrement() >= 1) {
              System.out.printf(",");
            }
            System.out.printf(
                "\n{\"cf\":\"%s\",\"key\":\"%s\",\"value\":%s}",
                cf, HexFormat.ofDelimiter(" ").formatHex(key), valueJson);
          }
        }));
    System.out.print("]}");
    return 0;
  }
}
