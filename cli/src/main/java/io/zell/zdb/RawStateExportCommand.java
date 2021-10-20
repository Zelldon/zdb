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

@Command(name = "export", description = "Export raw state")
public class RawStateExportCommand implements Callable<Integer> {

  @Spec
  private CommandSpec spec;

  @Command(name = "all", description = "Export all table column families (for which export has been implemented)")
  public int all() {
    final boolean yolo = spec.findOption("-y").getValue();

    if (!yolo) {
      spec.commandLine().usage(System.out);

      RawStateCommand.printWarning();
      return 0;
    }

    elementInstanceKeyColumnFamily();
    elementInstanceParentChildColumnFamily();
    messageKeyColumnFamily();
    messageDeadlineColumnFamily();
    return 0;
  }

  @Command(name = "elementInstanceKeyColumnFamily", description = "Exports the ZbColumnFamilies.ELEMENT_INSTANCE_KEY column family")
  public int elementInstanceKeyColumnFamily() {
    return export(RawState::exportElementInstanceKeyColumnFamily);
  }

  @Command(name = "elementInstanceParentChildColumnFamily", description = "Exports the ZbColumnFamilies.ELEMENT_INSTANCE_PARENT_CHILD column family")
  public int elementInstanceParentChildColumnFamily() {
    return export(RawState::exportElementInstanceParentChildColumnFamily);
  }

  @Command(name = "messageKeyColumnFamily", description = "Exports the ZbColumnFamilies.MESSAGE_KEY column family")
  public int messageKeyColumnFamily() {
    return export(RawState::exportMessageKeyColumnFamily);
  }

  @Command(name = "messageDeadlineColumnFamily", description = "Exports the ZbColumnFamilies.MESSAGE_DEADLINES column family")
  public int messageDeadlineColumnFamily() {
    return export(RawState::exportMessageDeadlineColumnFamily);
  }

  public int export(final Consumer<RawState> methodToCall) {
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
