package io.zeebe.impl;

import io.zeebe.EntityInspection;
import io.zeebe.PartitionState;
import io.zeebe.db.ColumnFamily;
import io.zeebe.db.impl.DbLong;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.Incident;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidentInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {

    final var incidents = new ArrayList<String>();

    final ColumnFamily<DbLong, Incident> incidentColumnFamily =
        getIncidentColumnFamily(partitionState);

    incidentColumnFamily.forEach(
        (key, incident) -> {
          final var incidentKey = key.getValue();
          final var incidentRecord = incident.getRecord();

          final var incidentAsString =
              String.format(
                  "Incident[key: %d, workflow-instance-key: %d, BPMN-process-id: \"%s\", error-type: %s]",
                  incidentKey,
                  incidentRecord.getWorkflowInstanceKey(),
                  incidentRecord.getBpmnProcessId(),
                  incidentRecord.getErrorType());

          incidents.add(incidentAsString);
        });

    return incidents;
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {

    final var incidentState = partitionState.getZeebeState().getIncidentState();

    return Optional.ofNullable(incidentState.getIncidentRecord(key))
        .map(
            incidentRecord ->
                String.format(
                    "Incident[key: %d, workflow-instance-key: %d, BPMN-process-id: \"%s\", error-type: %s, error-message: \"%s\"]",
                    key,
                    incidentRecord.getWorkflowInstanceKey(),
                    incidentRecord.getBpmnProcessId(),
                    incidentRecord.getErrorType(),
                    incidentRecord.getErrorMessage()))
        .orElse("No incident found with key: " + key);
  }

  private ColumnFamily<DbLong, Incident> getIncidentColumnFamily(
      final PartitionState partitionState) {
    return partitionState
        .getZeebeDb()
        .createColumnFamily(
            ZbColumnFamilies.INCIDENTS,
            partitionState.getDbContext(),
            new DbLong(),
            new Incident());
  }
}
