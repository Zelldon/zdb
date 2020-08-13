/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.deployment;

import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.processor.KeyGenerator;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.ElementInstanceState;
import java.util.Collection;
import org.agrona.DirectBuffer;

public final class WorkflowState {

  private final WorkflowPersistenceCache workflowPersistenceCache;
  private final ElementInstanceState elementInstanceState;

  public WorkflowState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb,
      final DbContext dbContext,
      final KeyGenerator keyGenerator) {
    workflowPersistenceCache = new WorkflowPersistenceCache(zeebeDb, dbContext);
    elementInstanceState = new ElementInstanceState(zeebeDb, dbContext);
  }

  public PersistedWorkflow getWorkflowByKey(final long workflowKey) {
    return workflowPersistenceCache.getWorkflowByKey(workflowKey);
  }

  public Collection<PersistedWorkflow> getWorkflows() {

    return workflowPersistenceCache.getWorkflows();
  }

  public Collection<PersistedWorkflow> getWorkflowsByBpmnProcessId(final DirectBuffer processId) {
    return workflowPersistenceCache.getWorkflowsByBpmnProcessId(processId);
  }

  public ElementInstanceState getElementInstanceState() {
    return elementInstanceState;
  }
}
