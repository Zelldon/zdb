/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb.impl;

import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.ElementInstanceState;

public final class InstanceInspection {

  public String entity(final PartitionState partitionState, final long key) {
    final var elementInstanceState =
        partitionState.getZeebeState().getWorkflowState().getElementInstanceState();

    final var workflowInstance = elementInstanceState.getInstance(key);
    return getWorkflowInstance(elementInstanceState, workflowInstance);
  }

  private static String getWorkflowInstance(
      ElementInstanceState elementInstanceState, ElementInstance workflowInstance) {

    final var stringBuilder = new StringBuilder("Workflow instance:");

    final var workflowInstanceValue = workflowInstance.getValue();
    stringBuilder
        .append("\nBPMN process id: ")
        .append(workflowInstanceValue.getBpmnProcessId())
        .append("\nVersion: ")
        .append(workflowInstanceValue.getVersion())
        .append("\nWorkflowKey: ")
        .append(workflowInstanceValue.getWorkflowKey())
        .append("\nWorkflowInstanceKey: ")
        .append(workflowInstanceValue.getWorkflowInstanceKey())
        .append("\nElementId: ")
        .append(workflowInstanceValue.getElementId())
        .append("\nBpmnElementType: ")
        .append(workflowInstanceValue.getBpmnElementType())
        .append("\nParentWorkflowInstanceKey: ")
        .append(workflowInstanceValue.getParentWorkflowInstanceKey())
        .append('\n');

    return addChildrenRecursive(stringBuilder, 1, elementInstanceState, workflowInstance)
        .toString();
  }

  private static StringBuilder addChildrenRecursive(
      final StringBuilder stringBuilder,
      final int intend,
      final ElementInstanceState elementInstanceState,
      final ElementInstance elementInstance) {
    if (elementInstance.getKey() != elementInstance.getValue().getWorkflowInstanceKey()) {
      addElemtentInstance(stringBuilder, intend, elementInstance);
    }

    final var children = elementInstanceState.getChildren(elementInstance.getKey());
    if (children == null || children.isEmpty()) {
      return stringBuilder.append("\n");
    }

    stringBuilder.append(getIntend(intend)).append("\\\n");
    for (var child : children) {
      addChildrenRecursive(stringBuilder, intend + 1, elementInstanceState, child);
    }

    return stringBuilder;
  }

  private static void addElemtentInstance(
      StringBuilder stringBuilder, int intend, ElementInstance childElementInstance) {
    final var childElementInstanceValue = childElementInstance.getValue();

    addChildProperty(stringBuilder, intend, "Key", childElementInstance.getKey());
    addChildProperty(stringBuilder, intend, "ElementId", childElementInstanceValue.getElementId());
    addChildProperty(
        stringBuilder, intend, "BpmnElementType", childElementInstanceValue.getBpmnElementType());
  }

  private static void addChildProperty(
      StringBuilder builder, int intend, String propertyName, Object value) {
    builder
        .append(getIntend(intend))
        .append('|')
        .append('-')
        .append(propertyName)
        .append(": ")
        .append(value)
        .append('\n');
  }

  private static String getIntend(final int intend) {
    return " ".repeat(calculateIntend(intend));
  }

  private static int calculateIntend(int intend) {
    return intend * 1;
  }
}
