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
import io.zeebe.engine.state.instance.Incident;
import java.util.concurrent.atomic.AtomicInteger;
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
   * @return a well formated string which contains all information
   */
  public String status(final PartitionState partitionState) {
    lastProcessedPosition(partitionState);
    lowestExportedPosition(partitionState);
    hasBlacklistedInstances(partitionState);
    hasIncidents(partitionState);
    messages(partitionState);
    return statusBuilder.toString();
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

  private void hasIncidents(final PartitionState partitionState) {
    final var incidentColumnFamily =
        partitionState
            .getZeebeDb()
            .createColumnFamily(
                ZbColumnFamilies.INCIDENTS,
                partitionState.getDbContext(),
                new DbLong(),
                new Incident());

    addToStatus("Incidents", incidentColumnFamily.isEmpty() ? "No" : "Yes");
  }

  private void hasBlacklistedInstances(final PartitionState partitionState) {
    final var blacklistColumnFamily =
        partitionState
            .getZeebeDb()
            .createColumnFamily(
                ZbColumnFamilies.BLACKLIST,
                partitionState.getDbContext(),
                new DbLong(),
                DbNil.INSTANCE);
    addToStatus("Blacklisted instances", blacklistColumnFamily.isEmpty() ? "No" : "Yes");
  }

  private void addToStatus(String key, String value) {
    statusBuilder.append(String.format("%n%s: %s", key, value));
  }

  private void lastProcessedPosition(PartitionState partitionState) {
    addToStatus(
        "Last processed position",
        String.valueOf(partitionState.getZeebeState().getLastSuccessfulProcessedRecordPosition()));
  }

  private void lowestExportedPosition(PartitionState partitionState) {
    final var exporterState = partitionState.getExporterState();
    exporterState.visitPositions(
        (id, position) -> {
          addToStatus(id, "position " + position);
        });

    final String positionString;
    if (exporterState.hasExporters()) {
      positionString = String.valueOf(exporterState.getLowestPosition());
    } else {
      positionString = "No exporters";
    }
    addToStatus("Lowest exported position", positionString);
  }
}
