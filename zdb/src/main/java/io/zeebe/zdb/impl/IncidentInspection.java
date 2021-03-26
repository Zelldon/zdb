/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb.impl;

import static io.zeebe.util.buffer.BufferUtil.bufferAsArray;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.impl.DbLong;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.Incident;
import io.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidentInspection {

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

  public String entity(final PartitionState partitionState, final long key) {

    final var incidentKey = new DbLong();
    incidentKey.wrapLong(key);

    final var stringBuilder = new StringBuilder();
    return Optional.ofNullable(getIncidentColumnFamily(partitionState).get(incidentKey).getRecord())
        .map(
            incidentRecord -> {
              final ElementInstance elementInstance =
                  partitionState
                      .getZeebeState()
                      .getWorkflowState()
                      .getElementInstanceState()
                      .getInstance(incidentRecord.getElementInstanceKey());
              return stringBuilder
                  .append("\nIncidentKey: ")
                  .append(key)
                  .append("\nWorkflowKey:")
                  .append(incidentRecord.getWorkflowKey())
                  .append("\nWorkflowInstanceKey: ")
                  .append(incidentRecord.getWorkflowInstanceKey())
                  .append("\nBpmnProcessId: ")
                  .append(incidentRecord.getBpmnProcessId())
                  .append("\nErrorType: ")
                  .append(incidentRecord.getErrorType())
                  .append("\nErrorMessage: ")
                  .append(incidentRecord.getErrorMessage())
                  .append("\nElementInstanceKey: ")
                  .append(incidentRecord.getElementInstanceKey())
                  .append("\nElementId: ")
                  .append(incidentRecord.getElementId())
                  .append("\nBpmnElementType: ")
                  .append(
                      elementInstance == null
                          ? "Cannot find element instance"
                          : elementInstance.getValue().getBpmnElementType())
                  .append("\nState: ")
                  .append(
                      elementInstance == null
                          ? "Cannot find element instance"
                          : elementInstance.getState())
                  .append(getIncidentDetails(incidentRecord, partitionState.getZeebeState()))
                  .toString();
            })
        .orElse("No incident found with key: " + key);
  }

  private String getIncidentDetails(IncidentRecord incidentRecord, ZeebeState state) {
    switch (incidentRecord.getErrorType()) {
      case IO_MAPPING_ERROR:
      case CONDITION_ERROR:
        final var variables =
            state
                .getWorkflowState()
                .getElementInstanceState()
                .getVariablesState()
                .getVariablesAsDocument(incidentRecord.getVariableScopeKey());
        return "\nVariables: " + MsgPackConverter.convertToJson(bufferAsArray(variables));
      case JOB_NO_RETRIES:
        final var jobKey = incidentRecord.getJobKey();
        return "\nJobKey: " + jobKey + "\nJobType: " + state.getJobState().getJob(jobKey).getType();

      case UNHANDLED_ERROR_EVENT:
      case CALLED_ELEMENT_ERROR:
      case EXTRACT_VALUE_ERROR:
      case UNKNOWN:
      default:
        return "";
    }
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
