/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.zdb.impl;

import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.ElementInstanceState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlacklistInspection {

  public List<String> list(final PartitionState partitionState) {
    final var elementInstanceState =
        partitionState.getZeebeState().getWorkflowState().getElementInstanceState();

    final var blacklistColumnFamily =
        partitionState
            .getZeebeDb()
            .createColumnFamily(
                ZbColumnFamilies.BLACKLIST,
                partitionState.getDbContext(),
                new DbLong(),
                DbNil.INSTANCE);

    final var blacklistedInstances = new ArrayList<String>();

    blacklistColumnFamily.forEach(
        (key, nil) -> {
          final var workflowInstanceKey = key.getValue();

          final var workflowInstance = elementInstanceState.getInstance(workflowInstanceKey);

          final var bpmnProcessId = workflowInstance.getValue().getBpmnProcessId();

          blacklistedInstances.add(toString(workflowInstanceKey, bpmnProcessId));
        });

    return blacklistedInstances;
  }

  public String entity(final PartitionState partitionState, final long key) {
    final var elementInstanceState =
        partitionState.getZeebeState().getWorkflowState().getElementInstanceState();

    final var keyType = new DbLong();
    final var blacklistColumnFamily =
        partitionState
            .getZeebeDb()
            .createColumnFamily(
                ZbColumnFamilies.BLACKLIST, partitionState.getDbContext(), keyType, DbNil.INSTANCE);

    keyType.wrapLong(key);
    final var dbNil = blacklistColumnFamily.get(keyType);

    return Optional.ofNullable(dbNil)
        .map(
            nil -> {
              final var workflowInstance = elementInstanceState.getInstance(key);
              return getBlacklistedWorkflowInstanceAsString(elementInstanceState, workflowInstance);
            })
        .orElse("No entity found for given key " + key);
  }

  private static String getBlacklistedWorkflowInstanceAsString(
      ElementInstanceState elementInstanceState, ElementInstance workflowInstance) {

    final var stringBuilder = new StringBuilder("Blacklisted workflow instance:\n");

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

  private static String toString(final long workflowInstanceKey, final String bpmnProcessId) {
    return String.format(
        "Blacklisted Instance [workflow-instance-key: %d, BPMN-process-id: \"%s\"]",
        workflowInstanceKey, bpmnProcessId);
  }

  private static int calculateIntend(int intend) {
    return intend * 1;
  }
}
