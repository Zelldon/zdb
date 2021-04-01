/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb;

import io.zeebe.zdb.impl.log.LogPrint;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "print", description = "Print's the complete log to standard out")
public class LogPrintCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Option(
      names = {"-f", "--format"},
      paramLabel = "[json, compact]",
      description = "The output format",
      defaultValue = "json",
      showDefaultValue = Visibility.ALWAYS)
  private Format format;

  @Override
  public Integer call() {
    final Path logPath = spec.findOption("-p").getValue();
    final var output = new LogPrint(format).print(logPath);
    System.out.println(output);
    return 0;
  }

  public enum Format {
    JSON,
    COMPACT;
  }
}
