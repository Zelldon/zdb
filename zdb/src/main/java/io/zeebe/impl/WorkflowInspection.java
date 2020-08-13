/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.impl;

import static io.zeebe.util.buffer.BufferUtil.bufferAsString;

import io.zeebe.EntityInspection;
import io.zeebe.PartitionState;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class WorkflowInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {
    final var workflowState = partitionState.getZeebeState().getWorkflowState();

    return workflowState.getWorkflows().stream()
        .map(
            workflow ->
                String.format(
                    "Workflow[key: %d, BPMN-process-id: \"%s\", version: %d]",
                    workflow.getKey(),
                    bufferAsString(workflow.getBpmnProcessId()),
                    workflow.getVersion()))
        .collect(Collectors.toList());
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {
    final var workflowState = partitionState.getZeebeState().getWorkflowState();

    return Optional.ofNullable(workflowState.getWorkflowByKey(key))
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
}
