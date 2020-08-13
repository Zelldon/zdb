/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.instance;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.db.impl.DbLong;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import java.util.function.ObjLongConsumer;

public final class IncidentState {
  public static final int MISSING_INCIDENT = -1;

  /** incident key -> incident record */
  private final DbLong incidentKey;

  // we need two separate wrapper to not interfere with get and put
  // see https://github.com/zeebe-io/zeebe/issues/1916
  private final Incident incidentRead = new Incident();
  private final ColumnFamily<DbLong, Incident> incidentColumnFamily;

  /** element instance key -> incident key */
  private final DbLong elementInstanceKey;

  private final ColumnFamily<DbLong, IncidentKey> workflowInstanceIncidentColumnFamily;

  /** job key -> incident key */
  private final DbLong jobKey;

  private final ColumnFamily<DbLong, IncidentKey> jobIncidentColumnFamily;
  private final IncidentKey incidentKeyValue = new IncidentKey();

  public IncidentState(final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    incidentKey = new DbLong();
    incidentColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.INCIDENTS, dbContext, incidentKey, incidentRead);

    elementInstanceKey = new DbLong();
    workflowInstanceIncidentColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.INCIDENT_WORKFLOW_INSTANCES,
            dbContext,
            elementInstanceKey,
            incidentKeyValue);

    jobKey = new DbLong();
    jobIncidentColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.INCIDENT_JOBS, dbContext, jobKey, incidentKeyValue);
  }

  public IncidentRecord getIncidentRecord(final long incidentKey) {
    this.incidentKey.wrapLong(incidentKey);

    final Incident incident = incidentColumnFamily.get(this.incidentKey);
    if (incident != null) {
      return incident.getRecord();
    }
    return null;
  }

  public long getWorkflowInstanceIncidentKey(final long workflowInstanceKey) {
    elementInstanceKey.wrapLong(workflowInstanceKey);

    final IncidentKey incidentKey = workflowInstanceIncidentColumnFamily.get(elementInstanceKey);

    if (incidentKey != null) {
      return incidentKey.get();
    }

    return MISSING_INCIDENT;
  }

  public long getJobIncidentKey(final long jobKey) {
    this.jobKey.wrapLong(jobKey);
    final IncidentKey incidentKey = jobIncidentColumnFamily.get(this.jobKey);

    if (incidentKey != null) {
      return incidentKey.get();
    }
    return MISSING_INCIDENT;
  }

  public boolean isJobIncident(final IncidentRecord record) {
    return record.getJobKey() > 0;
  }

  public void forExistingWorkflowIncident(
      final long elementInstanceKey, final ObjLongConsumer<IncidentRecord> resolver) {
    final long workflowIncidentKey = getWorkflowInstanceIncidentKey(elementInstanceKey);

    final boolean hasIncident = workflowIncidentKey != IncidentState.MISSING_INCIDENT;
    if (hasIncident) {
      final IncidentRecord incidentRecord = getIncidentRecord(workflowIncidentKey);
      resolver.accept(incidentRecord, workflowIncidentKey);
    }
  }
}
