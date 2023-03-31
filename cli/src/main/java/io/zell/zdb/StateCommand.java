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
import java.util.concurrent.Callable;
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
  public int list() {
    final var experimental = new Experimental(partitionPath);
    experimental.visitDBWithJsonValues(
        ((cf, key, valueJson) -> System.out.printf("%s,%s,%s", cf, new String(key), valueJson)));
    return 0;
  }
}
