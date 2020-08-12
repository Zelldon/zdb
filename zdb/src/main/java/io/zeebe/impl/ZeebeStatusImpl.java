package io.zeebe.impl;

import io.zeebe.PartitionState;
import io.zeebe.ZeebeStatus;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.Incident;

public class ZeebeStatusImpl implements ZeebeStatus {

  private final StringBuilder statusBuilder = new StringBuilder();

  @Override
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
    addToStatus("Blacklisted instances", blacklistColumnFamily.isEmpty()? "No" : "Yes");
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
