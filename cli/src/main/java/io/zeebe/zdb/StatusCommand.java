/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb;

import io.zeebe.zdb.impl.PartitionState;
import io.zeebe.zdb.impl.StatusInspection;
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
    final var partitionState = PartitionState.of(partitionPath);
    final var status = new StatusInspection().status(partitionState);
    System.out.println(status);
    return 0;
  }
}
