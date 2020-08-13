/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.impl;

import io.zeebe.EntityInspection;
import io.zeebe.PartitionState;
import io.zeebe.broker.exporter.stream.ExporterPosition;
import io.zeebe.db.ColumnFamily;
import io.zeebe.db.impl.DbString;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.util.buffer.BufferUtil;
import java.util.ArrayList;
import java.util.List;

public final class ExporterInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {

    final ColumnFamily<DbString, ExporterPosition> exporterPositionColumnFamily =
        getExporterPositionColumnFamily(partitionState);

    final var exporters = new ArrayList<String>();

    exporterPositionColumnFamily.forEach(
        (exporterId, exporterPosition) -> {
          final var id = BufferUtil.bufferAsString(exporterId.getBuffer());
          final var position = exporterPosition.get();

          final var exporter = String.format("Exporter[id: \"%s\", position: %d]", id, position);
          exporters.add(exporter);
        });

    return exporters;
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {
    return "nope";
  }

  private ColumnFamily<DbString, ExporterPosition> getExporterPositionColumnFamily(
      final PartitionState partitionState) {
    return partitionState
        .getZeebeDb()
        .createColumnFamily(
            ZbColumnFamilies.EXPORTER,
            partitionState.getDbContext(),
            new DbString(),
            new ExporterPosition());
  }
}
