/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb;

import io.zeebe.zdb.ZeebeDebugger.VersionProvider;
import java.util.concurrent.Callable;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.RunLast;

@Command(
    name = "zdb",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    description = "Zeebe debug and inspection tool",
    subcommands = {
      GenerateCompletion.class, // to generate auto completion
      StatusCommand.class,
      BlacklistCommand.class,
      IncidentCommand.class,
      WorkflowCommand.class,
      LogCommand.class,
      InstanceCommand.class
    })
public class ZeebeDebugger implements Callable<Integer> {
  private static CommandLine cli;

  /**
   * Disables the error stream to prevent IllegalAccess warnings to be logged.
   * https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
   */
  public static void disableWarning() {

    System.err.close();
    System.setErr(System.out);
  }

  public static void main(String[] args) {
    disableWarning();
    cli =
        new CommandLine(new ZeebeDebugger())
            .setExecutionStrategy(new RunLast())
            .setCaseInsensitiveEnumValuesAllowed(true);
    final int exitcode = cli.execute(args);
    System.exit(exitcode);
  }

  @Override
  public Integer call() {
    cli.usage(System.out);
    return 0;
  }

  static class VersionProvider implements IVersionProvider {
    public String[] getVersion() {
      return new String[] {"zdv v" + ZeebeDebugger.class.getPackage().getImplementationVersion()};
    }
  }
}
