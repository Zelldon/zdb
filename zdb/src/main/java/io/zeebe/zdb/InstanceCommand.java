/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb;

import io.zeebe.zdb.impl.InstanceInspection;
import io.zeebe.zdb.impl.PartitionState;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(
    name = "instance",
    mixinStandardHelpOptions = true,
    description = "Print's information about running instances")
public class InstanceCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true,
      scope = ScopeType.INHERIT)
  private Path partitionPath;

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return 0;
  }

  @Command(name = "entity", description = "Show details about an  workflow instance")
  public int entity(
      @Parameters(paramLabel = "KEY", description = "The key of the workflow instance", arity = "1")
          final long key) {
    final var partitionState = PartitionState.of(partitionPath);
    final var output = new InstanceInspection().entity(partitionState, key);
    System.out.println(output);
    return 0;
  }
}
