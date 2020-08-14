/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.deployment;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.db.impl.DbLong;
import io.zeebe.engine.state.ZbColumnFamilies;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class WorkflowPersistenceCache {

  // workflow
  private final ColumnFamily<DbLong, PersistedWorkflow> workflowColumnFamily;
  private final DbLong workflowKey;
  private final PersistedWorkflow persistedWorkflow;

  public WorkflowPersistenceCache(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    workflowKey = new DbLong();
    persistedWorkflow = new PersistedWorkflow();
    workflowColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.WORKFLOW_CACHE, dbContext, workflowKey, persistedWorkflow);
  }

  public PersistedWorkflow getWorkflowByKey(final long key) {
    workflowKey.wrapLong(key);
    return workflowColumnFamily.get(workflowKey);
  }

  public Collection<PersistedWorkflow> getWorkflows() {
    final Map<Long, PersistedWorkflow> workflows = new HashMap<>();
    workflowColumnFamily.forEach(workflow -> workflows.put(workflow.getKey(), workflow));
    return workflows.values();
  }
}
