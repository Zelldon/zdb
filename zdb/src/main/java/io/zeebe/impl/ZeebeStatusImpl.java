package io.zeebe.impl;

import io.zeebe.PartitionState;
import io.zeebe.ZeebeStatus;
import io.zeebe.broker.exporter.stream.ExportersState;

public class ZeebeStatusImpl implements ZeebeStatus {

  private final StringBuilder statusBuilder = new StringBuilder();

  @Override
  public String status(final PartitionState partitionState) {
    lastProcessedPosition(partitionState);
    lowestExportedPosition(partitionState);
    return statusBuilder.toString();
  }

  private void addToStatus(String key, String value) {
    statusBuilder.append(String.format("%n%s: %s", key, value));
  }

  private void addToStatus(String status) {
    statusBuilder.append(String.format("%n%s", status));
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

  private void listExporterPositions(ExportersState exportersState) {
    exportersState.visitPositions(
        (id, position) ->
            addToStatus(String.format("Exporter [id: %s position: %s]", id, position)));
  }
}
