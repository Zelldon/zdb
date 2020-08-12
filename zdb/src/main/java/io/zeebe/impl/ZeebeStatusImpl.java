package io.zeebe.impl;

import io.zeebe.PartitionState;
import io.zeebe.ZeebeStatus;
import java.util.HashMap;
import java.util.Map;

public class ZeebeStatusImpl implements ZeebeStatus {

  private static Map<String, String> statuses = new HashMap<>();

  @Override
  public String status(final PartitionState partitionState) {
    lastProcessedPosition(partitionState);
    lastExportedPosition(partitionState);
    return getStatusString();
  }

  private void lastProcessedPosition(PartitionState partitionState) {
    statuses.put(
        "last processed position",
        String.valueOf(partitionState.getZeebeState().getLastSuccessfulProcessedRecordPosition()));
  }

  private void lastExportedPosition(PartitionState partitionState) {
    statuses.put(
        "lowest exported position",
        String.valueOf(partitionState.getExporterState().getLowestPosition()));
  }

  private String getStatusString() {
    final var stringBuilder = new StringBuilder();
    statuses.forEach((k, v) -> stringBuilder.append(String.format("%n%s: %s", k, v)));
    return stringBuilder.toString();
  }
}
