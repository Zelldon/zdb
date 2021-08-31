/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import io.zell.zdb.state.blacklist.BlacklistState;
import io.zell.zdb.state.incident.IncidentState;
import io.zell.zdb.state.instance.InstanceState;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(
    name = "blacklist",
    mixinStandardHelpOptions = true,
    description = "Print's information about blacklisted workflow instances")
public class BlacklistCommand implements Callable<Integer> {

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

  @Command(name = "list", description = "List all blacklisted workflow instances")
  public int list() {
    final var blacklistedInstances = new BlacklistState(partitionPath).listBlacklistedInstances();
    System.out.println(blacklistedInstances);
    return 0;
  }

  @Command(name = "entity", description = "Show details about blacklisted workflow instance")
  public int entity(
      @Parameters(
              paramLabel = "KEY",
              description = "The key of the blacklisted workflow instance",
              arity = "1")
          final long key) {
    final var instanceDetails = new InstanceState(partitionPath).instanceDetails(key);
    System.out.println(instanceDetails);
    return 0;
  }
}
