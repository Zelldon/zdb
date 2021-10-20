/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "check", description = "Check data integrity and consistency")
public class RawStateConsistencyCheckCommand implements Callable<Integer> {

  @Spec
  private CommandSpec spec;

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);

    final boolean yolo = spec.findOption("-y").getValue();

    if (!yolo) {

      RawStateCommand.printWarning();
      return 0;
    }

    return 0;
  }
}
