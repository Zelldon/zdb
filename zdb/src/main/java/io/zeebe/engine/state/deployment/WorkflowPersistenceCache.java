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
import io.zeebe.db.impl.DbCompositeKey;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbString;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.agrona.io.DirectBufferInputStream;

public final class WorkflowPersistenceCache {

  // workflow
  private final ColumnFamily<DbLong, PersistedWorkflow> workflowColumnFamily;
  private final DbLong workflowKey;
  private final PersistedWorkflow persistedWorkflow;

  private final ColumnFamily<DbCompositeKey, PersistedWorkflow> workflowByIdAndVersionColumnFamily;
  private final DbLong workflowVersion;
  private final DbCompositeKey<DbString, DbLong> idAndVersionKey;

  private final ColumnFamily<DbString, LatestWorkflowVersion> latestWorkflowColumnFamily;
  private final DbString workflowId;
  private final LatestWorkflowVersion latestVersion = new LatestWorkflowVersion();

  private final ColumnFamily<DbString, Digest> digestByIdColumnFamily;
  private final Digest digest = new Digest();

  public WorkflowPersistenceCache(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    workflowKey = new DbLong();
    persistedWorkflow = new PersistedWorkflow();
    workflowColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.WORKFLOW_CACHE, dbContext, workflowKey, persistedWorkflow);

    workflowId = new DbString();
    workflowVersion = new DbLong();
    idAndVersionKey = new DbCompositeKey<>(workflowId, workflowVersion);
    workflowByIdAndVersionColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.WORKFLOW_CACHE_BY_ID_AND_VERSION,
            dbContext,
            idAndVersionKey,
            persistedWorkflow);

    latestWorkflowColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.WORKFLOW_CACHE_LATEST_KEY, dbContext, workflowId, latestVersion);

    digestByIdColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.WORKFLOW_CACHE_DIGEST_BY_ID, dbContext, workflowId, digest);
  }

  private BpmnModelInstance readModelInstanceFromBuffer(final DirectBuffer buffer) {
    try (final DirectBufferInputStream stream = new DirectBufferInputStream(buffer)) {
      return Bpmn.readModelFromStream(stream);
    }
  }

  public PersistedWorkflow getLatestWorkflowVersionByProcessId(final DirectBuffer processId) {
    workflowId.wrapBuffer(processId);
    final LatestWorkflowVersion latestVersion = latestWorkflowColumnFamily.get(workflowId);
    final long latestVersion1 = latestVersion != null ? latestVersion.get() : -1;
    workflowVersion.wrapLong(latestVersion1);

    return workflowByIdAndVersionColumnFamily.get(idAndVersionKey);
  }

  public PersistedWorkflow getWorkflowByProcessIdAndVersion(
      final DirectBuffer processId, final int version) {
    workflowId.wrapBuffer(processId);
    workflowVersion.wrapLong(version);

    return workflowByIdAndVersionColumnFamily.get(idAndVersionKey);
  }

  public PersistedWorkflow getWorkflowByKey(final long key) {
    final Map<Long, PersistedWorkflow> workflows = new HashMap<>();
    workflowColumnFamily.forEach(workflow -> workflows.put(workflow.getKey(), workflow));
    return workflows.get(key);
  }

  public Collection<PersistedWorkflow> getWorkflows() {
    final Map<Long, PersistedWorkflow> workflows = new HashMap<>();
    workflowColumnFamily.forEach(workflow -> workflows.put(workflow.getKey(), workflow));
    return workflows.values();
  }

  public Collection<PersistedWorkflow> getWorkflowsByBpmnProcessId(
      final DirectBuffer bpmnProcessId) {
    final List<PersistedWorkflow> workflows = new ArrayList<>();
    workflowColumnFamily.forEach(
        workflow -> {
          if (bpmnProcessId.equals(workflow.getBpmnProcessId())) {
            workflows.add(workflow);
          }
        });
    return workflows;
  }

  public DirectBuffer getLatestVersionDigest(final DirectBuffer processId) {
    workflowId.wrapBuffer(processId);
    final Digest latestDigest = digestByIdColumnFamily.get(workflowId);
    return latestDigest == null || digest.get().byteArray() == null ? null : latestDigest.get();
  }
}
