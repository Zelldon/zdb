package io.zeebe.impl;

import io.zeebe.ZeebeStatus;
import java.nio.file.Path;

public class ZeebeStatusImpl implements ZeebeStatus {

  @Override
  public String status(final Path path) {
    final PartitionState partitionState = new PartitionState(path);
    partitionState.getZeebeState().getLastSuccessfulProcessedRecordPosition();
    return null;
  }
}
