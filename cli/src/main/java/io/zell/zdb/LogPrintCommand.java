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

  @Spec private CommandSpec spec;

  @Option(
      names = {"-d", "--dot"},
      description = "Print's the complete log in dot format, which can be consumed by graphviz")
  private boolean dotToggle;

  @Override
  public Integer call() {
    final Path partitionPath = spec.findOption("-p").getValue();
    final var logContentReader = new LogContentReader(partitionPath);
    final var logContent = logContentReader.content();
    if (dotToggle) {
      System.out.println(logContent.asDotFile());
    } else {
      System.out.println(logContent);
    }

    return 0;
  }
}
