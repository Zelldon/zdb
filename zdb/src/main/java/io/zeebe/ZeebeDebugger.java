/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe;

import java.util.concurrent.Callable;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.RunLast;

@Command(
    name = "zdb",
    mixinStandardHelpOptions = true,
    version = ZeebeDebugger.ZDB_VERSION,
    description = "Zeebe debug and inspection tool",
    subcommands = {
      GenerateCompletion.class, // to generate auto completion
      StatusCommand.class,
      BlacklistCommand.class,
      IncidentCommand.class,
      WorkflowCommand.class,
      LogCommand.class
    })
public class ZeebeDebugger implements Callable<Integer> {

  protected static final String ZDB_VERSION = "zdb 0.1";

  private static CommandLine cli;

  public static void main(String[] args) {
    cli = new CommandLine(new ZeebeDebugger()).setExecutionStrategy(new RunLast());
    final int exitcode = cli.execute(args);
    System.exit(exitcode);
  }

  @Override
  public Integer call() {
    cli.usage(System.out);
    return 0;
  }
}
