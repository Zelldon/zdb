/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb;

import io.zell.zdb.state.instance.InstanceState;
import io.zell.zdb.state.process.ProcessState;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(
    name = "process",
    mixinStandardHelpOptions = true,
    description = "Print's information about deployed processes")
public class ProcessCommand implements Callable<Integer> {

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

  @Command(name = "list", description = "List all processes")
  public int list() {
    final var processMetas = new ProcessState(partitionPath).listProcesses();
    System.out.printf("[%ns%n]%n", processMetas);
    return 0;
  }

  @Command(name = "entity", description = "Show details about a process")
  public int entity(
      @Parameters(paramLabel = "KEY", description = "The key of the process", arity = "1")
          final long key) {
    final var processDetails = new ProcessState(partitionPath).processDetails(key);
    System.out.println(processDetails);
    return 0;
  }

  @Command(name = "instances", description = "Show all instances of a process")
  public int instances(
      @Parameters(paramLabel = "KEY", description = "The key of the process", arity = "1")
          final long key) {
    throw new UnsupportedOperationException("Sorry not implemented yet. Feel free to create an PR for it ^.^");
  }
}
