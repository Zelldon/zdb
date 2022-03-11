/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import io.zell.zdb.log.LogContentReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "print", description = "Print's the complete log to standard out")
public class LogPrintCommand implements Callable<Integer> {

  public enum Format {
    JSON,
    DOT,
  }

  @Spec private CommandSpec spec;

  @Option(
      names = {"-f", "--format"},
      description =
          "Print's the complete log in the specified format, defaults to json. Possible values: [ ${COMPLETION-CANDIDATES} ]",
      defaultValue = "JSON")
  private Format format;

  @Override
  public Integer call() {
    final Path partitionPath = spec.findOption("-p").getValue();
    final var logContentReader = new LogContentReader(partitionPath);
    final var logContent = logContentReader.content();
    if (format == Format.DOT) {
      System.out.println(logContent.asDotFile());
    } else {
      System.out.println(logContent);
    }

    return 0;
  }
}
