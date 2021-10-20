/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(
    name = "rawstate",
    mixinStandardHelpOptions = true,
    subcommands = {RawStateConsistencyCheckCommand.class},
    description = "Allows to inspect the raw state with subcommands")
public class RawStateCommand implements Callable<Integer> {

  @Spec
  private CommandSpec spec;

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true,
      scope = ScopeType.INHERIT)
  private Path partitionPath;

  @Option(
      names = {"-y", "--yolo"},
      paramLabel = "YOLO",
      description = "Set this to true if you understand the risks",
      required = true,
      defaultValue = "false",
      scope = ScopeType.INHERIT)
  private boolean yolo;

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);

    if (!yolo) {
      printWarning();
      return 0;
    }

    return 0;
  }

  public static void printWarning() {
    System.out.println();
    System.out.println(
        "This command accesses the raw state stored in RocksDB. Since RocksDB has no schema, the access is realized by reimplementing the schema used by Zeebe.");
    System.out.println(
        "This duplicated schema-dependent code may go out of sync with Zeebe, at which point you will probably read garbage.");
    System.out.println(
        "Run this command with -y or --yolo if you understand the risks and wish to run it anyway.");
  }
}
