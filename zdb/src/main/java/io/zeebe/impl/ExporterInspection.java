/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
