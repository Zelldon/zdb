/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.engine.state;

import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.deployment.WorkflowState;
import io.zeebe.engine.state.instance.JobState;

public class ZeebeState {

  private final WorkflowState workflowState;
  private final JobState jobState;
  private final LastProcessedPositionState lastProcessedPositionState;

  public ZeebeState(final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    workflowState = new WorkflowState(zeebeDb, dbContext);
    jobState = new JobState(zeebeDb, dbContext);
    lastProcessedPositionState = new LastProcessedPositionState(zeebeDb, dbContext);
  }

  public WorkflowState getWorkflowState() {
    return workflowState;
  }

  public JobState getJobState() {
    return jobState;
  }

  public long getLastSuccessfulProcessedRecordPosition() {
    return lastProcessedPositionState.getPosition();
  }
}
