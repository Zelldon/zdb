package io.zeebe.impl;

import io.zeebe.EntityInspection;
import io.zeebe.PartitionState;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.ElementInstanceState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlacklistInspection implements EntityInspection {

  @Override
  public List<String> list(final PartitionState partitionState) {

    final var elementInstanceState = partitionState.getZeebeState().getWorkflowState()
        .getElementInstanceState();

    final var blacklistColumnFamily = partitionState.getZeebeDb()
        .createColumnFamily(ZbColumnFamilies.BLACKLIST, partitionState.getDbContext(), new DbLong(),
            DbNil.INSTANCE);

    final var blacklistedInstances = new ArrayList<String>();

    blacklistColumnFamily.forEach((key, nil) -> {
          final var workflowInstanceKey = key.getValue();

          final var workflowInstance = elementInstanceState.getInstance(workflowInstanceKey);

          final var bpmnProcessId = workflowInstance.getValue()
              .getBpmnProcessId();

          blacklistedInstances.add(toString(workflowInstanceKey, bpmnProcessId));
        });

    return blacklistedInstances;
  }

  @Override
  public String entity(final PartitionState partitionState, final long key) {
    final var elementInstanceState = partitionState.getZeebeState().getWorkflowState()
        .getElementInstanceState();

    final var keyType = new DbLong();
    final var blacklistColumnFamily = partitionState.getZeebeDb()
        .createColumnFamily(ZbColumnFamilies.BLACKLIST, partitionState.getDbContext(), keyType,
            DbNil.INSTANCE);

    keyType.wrapLong(key);
    final var dbNil = blacklistColumnFamily.get(keyType);

    return Optional.ofNullable(dbNil).map(nil -> {
      final var workflowInstance = elementInstanceState.getInstance(key);
      return getBlacklistedWorkflowInstanceAsString(elementInstanceState, workflowInstance);
    }).orElse("No entity found for given key " + key);
  }

  private static String getBlacklistedWorkflowInstanceAsString(ElementInstanceState elementInstanceState, ElementInstance workflowInstance) {

    final var stringBuilder = new StringBuilder("Blacklisted workflow instance:\n");

    final var workflowInstanceValue = workflowInstance.getValue();
    stringBuilder.append("\nBPMN process id: ").append(workflowInstanceValue.getBpmnProcessId())
                 .append("\nVersion: ").append(workflowInstanceValue.getVersion())
                 .append("\nWorkflowKey: ").append(workflowInstanceValue.getWorkflowKey())
                 .append("\nWorkflowInstanceKey: ").append(workflowInstanceValue.getWorkflowInstanceKey())
                 .append("\nElementId: ").append(workflowInstanceValue.getElementId())
                 .append("\nBpmnElementType: ").append(workflowInstanceValue.getBpmnElementType())
                 .append("\nParentWorkflowInstanceKey: ").append(workflowInstanceValue.getParentWorkflowInstanceKey());

    return addChildrenRecursive(stringBuilder, 1, elementInstanceState, workflowInstance).toString();
  }

  private static StringBuilder addChildrenRecursive(final StringBuilder stringBuilder,
      final int intend,
      final ElementInstanceState elementInstanceState,
      final ElementInstance elementInstance) {
    if (elementInstance.getKey() != elementInstance.getValue().getWorkflowInstanceKey()) {
      addElemtentInstance(stringBuilder, intend, elementInstance);
    }

    final var children = elementInstanceState.getChildren(elementInstance.getKey());
    if (children == null || children.isEmpty()) {
      return stringBuilder;
    }

    stringBuilder.append('\n').append("\t".repeat(intend)).append("Childs:\n");
    for (var child : children) {
      addChildrenRecursive(stringBuilder, intend + 1, elementInstanceState, child)
          .append("\n");
    }

    return stringBuilder;
  }

  private static void addElemtentInstance(StringBuilder stringBuilder, int intend, ElementInstance childElementInstance) {
    final var childElementInstanceValue = childElementInstance.getValue();
    stringBuilder
        .append('\n').append("\t".repeat(intend))
        .append("Key: ").append(childElementInstance.getKey())
        .append('\n').append("\t".repeat(intend))
        .append("WorkflowInstanceKey: ").append(childElementInstanceValue.getWorkflowInstanceKey())
        .append('\n').append("\t".repeat(intend))
        .append("ElementId: ").append(childElementInstanceValue.getElementId())
        .append('\n').append("\t".repeat(intend))
        .append("BpmnElementType: ").append(childElementInstanceValue.getBpmnElementType())
        .append('\n').append("\t".repeat(intend))
        .append("ParentElementInstanceKey: ").append(childElementInstanceValue.getParentElementInstanceKey())
        .append('\n').append("\t".repeat(intend))
        .append("ParentWorkflowInstanceKey: ").append(childElementInstanceValue.getParentWorkflowInstanceKey())
        .append('\n').append("\t".repeat(intend))
        .append("FlowScopeKey: ").append(childElementInstanceValue.getFlowScopeKey());
  }

  private static String toString(final long workflowInstanceKey, final String bpmnProcessId) {
    return String.format(
        "Blacklisted Instance [workflow-instance-key: %d, BPMN-process-id: \"%s\"]",
        workflowInstanceKey,
        bpmnProcessId);
  }


}
