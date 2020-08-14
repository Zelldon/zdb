/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.zdb.impl;

import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.Incident;

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
    return statusBuilder.toString();
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
    final String positionString;
    if (exporterState.hasExporters()) {
      positionString = String.valueOf(exporterState.getLowestPosition());
    } else {
      positionString = "No exporters";
    }
    addToStatus("Lowest exported position", positionString);
  }
}
