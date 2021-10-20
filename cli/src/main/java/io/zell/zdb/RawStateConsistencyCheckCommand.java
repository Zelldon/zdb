/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb;

import io.zell.zdb.state.raw.RawState;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "check", description = "Check data integrity and consistency")
public class RawStateConsistencyCheckCommand implements Callable<Integer> {

  @Spec
  private CommandSpec spec;

  @Command(name = "all", description = "Run all implemented consistency checks")
  public int all() {
    final boolean yolo = spec.findOption("-y").getValue();

    if (!yolo) {
      spec.commandLine().usage(System.out);

      RawStateCommand.printWarning();
      return 0;
    }

    elementInstanceKeyColumnFamily();
    elementInstanceParentChildColumnFamily();
    messageDeadlineColumnFamily();
    return 0;
  }

  @Command(name = "elementInstanceKeyColumnFamily", description = "Checks whether the element instance key family has orphaned entries, i.e. entries which point to parents which no longer exist")
  public int elementInstanceKeyColumnFamily() {
    return check(RawState::checkConsistencyElementInstanceKeyColumnFamily);
  }

  @Command(name = "elementInstanceParentChildColumnFamily", description = "Checks whether the element instance parent child table contains entries where either the child or the parent are missing")
  public int elementInstanceParentChildColumnFamily() {
    return check(RawState::checkConsistencyElementInstanceParentChildColumnFamily);
  }

  @Command(name = "messageDeadlineColumnFamily", description = "Checks whether the message deadline column contains invalid entries")
  public int messageDeadlineColumnFamily() {
    return check(RawState::checkConsistencyMessageDeadlineColumnFamily);
  }

  public int check(final Consumer<RawState> methodToCall) {
    final boolean yolo = spec.findOption("-y").getValue();

    if (!yolo) {
      spec.commandLine().usage(System.out);

      RawStateCommand.printWarning();
      return 0;
    }

    final Path partitionPath = spec.findOption("-p").getValue();

    final var rawState = new RawState(partitionPath);

    methodToCall.accept(rawState);
    return 0;
  }

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
