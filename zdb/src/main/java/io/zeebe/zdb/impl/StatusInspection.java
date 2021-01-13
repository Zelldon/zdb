/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.zdb.impl;

import io.zeebe.db.DbKey;
import io.zeebe.db.impl.DbCompositeKey;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.db.impl.DbString;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.Incident;
import io.zeebe.engine.state.instance.VariableInstance;
import java.util.concurrent.atomic.AtomicLong;

public class StatusInspection {

  private final StringBuilder statusBuilder = new StringBuilder();

  /**
   * Returns an well formated string which contains all status information about Zeebe, it searchs
   * in the given partition state for the necessary information.
   *
   * <p>Returned information's are:
   *
   * <ul>
   *   <li>lastExportedPosition
   *   <li>lastProcessedPosition
   * </ul>
   *
   * @param partitionState the state of the partition
   * @return a well formatted string which contains all information
   */
  public String status(final PartitionState partitionState) {
    processing(partitionState);
    exporting(partitionState);
    incidentRelated(partitionState);
    messages(partitionState);
    workflowInstances(partitionState);
    variables(partitionState);
    return statusBuilder.toString();
  }

  private void processing(final PartitionState partitionState) {
    addToStatus("Processing", "");
    lastProcessedPosition(partitionState);
  }

  private void exporting(final PartitionState partitionState) {
    addToStatus("Exporting", "");
    lowestExportedPosition(partitionState);
  }

  private void workflowInstances(final PartitionState partitionState) {
    final DbLong elementInstanceKey = new DbLong();

    final ElementInstance elementInstance = new ElementInstance();
    final var elementInstanceColumnFamily =
        partitionState.getZeebeDb().createColumnFamily(
            ZbColumnFamilies.ELEMENT_INSTANCE_KEY, partitionState.getDbContext(), elementInstanceKey, elementInstance);

    final AtomicLong workflowInstanceCounter = new AtomicLong(0);
    final AtomicLong elementInstanceCounter = new AtomicLong(0);
    elementInstanceColumnFamily.forEach(((dbLong, elementInstance1) -> {
      final var parentKey = elementInstance1.getParentKey();
      if (parentKey == -1){
        workflowInstanceCounter.incrementAndGet();
      }
      elementInstanceCounter.incrementAndGet();
    }));
    addToStatus("WorkflowInstances: ", "" + workflowInstanceCounter.get());
    addToStatus("\tElementInstances: ", "" + elementInstanceCounter.get());
  }

  private void variables(final PartitionState partitionState) {

    final DbLong scopeKey = new DbLong();
    final DbString variableName = new DbString();
    final DbCompositeKey<DbLong, DbString> scopeKeyVariableNameKey = new DbCompositeKey<>(scopeKey,
        variableName);
    final var variablesColumnFamily =
        partitionState.getZeebeDb().createColumnFamily(
            ZbColumnFamilies.VARIABLES, partitionState.getDbContext(), scopeKeyVariableNameKey, new VariableInstance());

    final AtomicLong counter = new AtomicLong(0);
    final AtomicLong minSize = new AtomicLong(Long.MAX_VALUE);
    final AtomicLong maxSize = new AtomicLong(Long.MIN_VALUE);
    final AtomicLong avgSize = new AtomicLong(0);
    variablesColumnFamily.forEach(variableInstance -> {
      counter.incrementAndGet();
      final var size = variableInstance.getValue().capacity();

      if (minSize.get() > size) {
        minSize.set(size);
      }

      if (maxSize.get() < size) {
        maxSize.set(size);
      }
      avgSize.addAndGet(size);
    });
    addToStatus("Variables", "" + counter.get());
    addToStatus("\tmin size", "" + minSize.get());
    addToStatus("\tmax size", "" + maxSize.get());
    addToStatus("\tavg size", "" + (avgSize.get() / (float) counter.get()));
  }

  private void messages(final PartitionState partitionState) {
    messageCount(partitionState);
    messageDeadlines(partitionState);
  }

  private void messageCount(final PartitionState partitionState) {

    final DbLong messageKey = new DbLong();
    final DbString messageName = new DbString();
    final DbString correlationKey = new DbString();
    final DbCompositeKey<DbString, DbString> nameAndCorrelationKey = new DbCompositeKey<>(
        messageName, correlationKey);
    final DbCompositeKey<DbCompositeKey<DbString, DbString>, DbKey> nameCorrelationMessageKey = new DbCompositeKey<>(
        nameAndCorrelationKey, messageKey);
    final var nameCorrelationMessageColumnFamily = partitionState.getZeebeDb().createColumnFamily(
        ZbColumnFamilies.MESSAGES, partitionState.getDbContext(), nameCorrelationMessageKey, DbNil.INSTANCE);

    final AtomicLong counter = new AtomicLong(0);
    nameCorrelationMessageColumnFamily.forEach(nil -> counter.incrementAndGet());
    addToStatus("Messages: ", "" + counter.get());
  }

  private void messageDeadlines(final PartitionState partitionState) {

    final DbLong messageKey = new DbLong();

    final DbLong deadline = new DbLong();
    final DbCompositeKey<DbLong, DbLong> deadlineMessageKey = new DbCompositeKey<>(deadline,
        messageKey);
    final var deadlineColumnFamily =
        partitionState.getZeebeDb().createColumnFamily(
            ZbColumnFamilies.MESSAGE_DEADLINES, partitionState.getDbContext(), deadlineMessageKey, DbNil.INSTANCE);

    final AtomicLong firstDeadline = new AtomicLong(-1);
    final AtomicLong lastDeadline = new AtomicLong(0);
    deadlineColumnFamily.forEach(((dbLongDbLongDbCompositeKey, dbNil) -> {
      final var currentDeadline = dbLongDbLongDbCompositeKey.getFirst().getValue();

      if (firstDeadline.get() == -1)
      {
        firstDeadline.set(currentDeadline);
      }
      lastDeadline.set(currentDeadline);
    }));

    addToStatus("\tCurrent Time: ", "" + System.currentTimeMillis());
    addToStatus("\tMessage next deadline: ", "" + firstDeadline.get());
    addToStatus("\tMessage last deadline: ", "" + lastDeadline.get());
  }


  private void incidentRelated(final PartitionState partitionState) {
    addToStatus("Incident related:", "");

    blacklistedInstanceCount(partitionState);
    incidentCount(partitionState);
  }

  private void incidentCount(final PartitionState partitionState) {
    final var incidentColumnFamily =
        partitionState
            .getZeebeDb()
            .createColumnFamily(
                ZbColumnFamilies.INCIDENTS,
                partitionState.getDbContext(),
                new DbLong(),
                new Incident());

    final AtomicLong counter = new AtomicLong();
    incidentColumnFamily.forEach(n -> counter.incrementAndGet());
    addToStatus("\tIncidents", "" + counter.get());
  }

  private void blacklistedInstanceCount(final PartitionState partitionState) {
    final var blacklistColumnFamily =
        partitionState
            .getZeebeDb()
            .createColumnFamily(
                ZbColumnFamilies.BLACKLIST,
                partitionState.getDbContext(),
                new DbLong(),
                DbNil.INSTANCE);

    final AtomicLong counter = new AtomicLong();
    blacklistColumnFamily.forEach(n -> counter.incrementAndGet());
    addToStatus("\tBlacklisted instances", "" + counter.get());
  }

  private void addToStatus(String key, String value) {
    statusBuilder.append(String.format("%n%s: %s", key, value));
  }

  private void lastProcessedPosition(PartitionState partitionState) {
    addToStatus(
        "\tLast processed position",
        String.valueOf(partitionState.getZeebeState().getLastSuccessfulProcessedRecordPosition()));
  }

  private void lowestExportedPosition(PartitionState partitionState) {
    final var exporterState = partitionState.getExporterState();
    exporterState.visitPositions(
        (id, position) -> addToStatus("\t" + id, "position " + position));

    final String positionString;
    if (exporterState.hasExporters()) {
      positionString = String.valueOf(exporterState.getLowestPosition());
    } else {
      positionString = "No exporters";
    }
    addToStatus("\tLowest exported position", positionString);
  }
}
