/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.zdb.impl;

import static io.zeebe.util.buffer.BufferUtil.bufferAsString;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.impl.DbLong;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.deployment.PersistedWorkflow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WorkflowInspection {

  public List<String> list(final PartitionState partitionState) {
    final List<String> workflows = new ArrayList<>();
    getColumnFamily(partitionState)
        .forEach(
            workflow ->
                workflows.add(
                    String.format(
                        "Workflow[key: %d, BPMN-process-id: \"%s\", version: %d]",
                        workflow.getKey(),
                        bufferAsString(workflow.getBpmnProcessId()),
                        workflow.getVersion())));
    return workflows;
  }

  public String entity(final PartitionState partitionState, final long key) {
    final var workflowKey = new DbLong();
    workflowKey.wrapLong(key);
    return Optional.ofNullable(getColumnFamily(partitionState).get(workflowKey))
        .map(
            workflow ->
                String.format(
                    "Workflow[key: %d, BPMN-process-id: \"%s\", version: %d, resource-name: \"%s\", resource: \"%s\"]",
                    workflow.getKey(),
                    bufferAsString(workflow.getBpmnProcessId()),
                    workflow.getVersion(),
                    bufferAsString(workflow.getResourceName()),
                    bufferAsString(workflow.getResource())))
        .orElse("No workflow found with key: " + key);
  }

  private ColumnFamily<DbLong, PersistedWorkflow> getColumnFamily(
      final PartitionState partitionState) {
    return partitionState
        .getZeebeDb()
        .createColumnFamily(
            ZbColumnFamilies.WORKFLOW_CACHE,
            partitionState.getDbContext(),
            new DbLong(),
            new PersistedWorkflow());
  }
}
