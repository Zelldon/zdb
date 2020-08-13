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
