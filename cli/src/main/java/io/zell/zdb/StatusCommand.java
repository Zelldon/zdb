/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import io.zell.zdb.state.Experimental;
import io.zell.zdb.state.general.GeneralState;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "status",
    mixinStandardHelpOptions = true,
    description = "Prints general information of the internal state")
public class StatusCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true)
  private Path partitionPath;

  @Override
  public Integer call() {
    final var generalDetails = new GeneralState(partitionPath).generalDetails();
    System.out.println(generalDetails);
    return 0;
  }

  /**
   * Alpha feature: Planned to replace old status call
   *
   * @return the status code of the call
   */
  @Command(name = "details", description = "Print for all column families the detailed statistics")
  public int list() {
    final var jsonString = new Experimental(partitionPath).stateStatisticsAsJsonString();
    System.out.println(jsonString);
    return 0;
  }
}
