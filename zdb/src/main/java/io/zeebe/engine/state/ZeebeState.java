/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state;

import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.processor.KeyGenerator;
import io.zeebe.engine.state.deployment.WorkflowState;
import io.zeebe.engine.state.instance.IncidentState;
import io.zeebe.engine.state.instance.JobState;
import io.zeebe.engine.state.message.MessageStartEventSubscriptionState;
import io.zeebe.engine.state.message.MessageState;
import io.zeebe.engine.state.message.MessageSubscriptionState;
import io.zeebe.engine.state.message.WorkflowInstanceSubscriptionState;

public class ZeebeState {

  private final KeyState keyState;
  private final WorkflowState workflowState;
  private final JobState jobState;
  private final MessageState messageState;
  private final MessageSubscriptionState messageSubscriptionState;
  private final MessageStartEventSubscriptionState messageStartEventSubscriptionState;
  private final WorkflowInstanceSubscriptionState workflowInstanceSubscriptionState;
  private final IncidentState incidentState;
  private final LastProcessedPositionState lastProcessedPositionState;

  private final int partitionId;

  public ZeebeState(
      final int partitionId, final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    this.partitionId = partitionId;
    keyState = new KeyState(partitionId, zeebeDb, dbContext);
    workflowState = new WorkflowState(zeebeDb, dbContext, keyState);
    jobState = new JobState(zeebeDb, dbContext);
    messageState = new MessageState(zeebeDb, dbContext);
    messageSubscriptionState = new MessageSubscriptionState(zeebeDb, dbContext);
    messageStartEventSubscriptionState = new MessageStartEventSubscriptionState(zeebeDb, dbContext);
    workflowInstanceSubscriptionState = new WorkflowInstanceSubscriptionState(zeebeDb, dbContext);
    incidentState = new IncidentState(zeebeDb, dbContext);
    lastProcessedPositionState = new LastProcessedPositionState(zeebeDb, dbContext);
  }

  public WorkflowState getWorkflowState() {
    return workflowState;
  }

  public JobState getJobState() {
    return jobState;
  }

  public MessageState getMessageState() {
    return messageState;
  }

  public MessageSubscriptionState getMessageSubscriptionState() {
    return messageSubscriptionState;
  }

  public MessageStartEventSubscriptionState getMessageStartEventSubscriptionState() {
    return messageStartEventSubscriptionState;
  }

  public WorkflowInstanceSubscriptionState getWorkflowInstanceSubscriptionState() {
    return workflowInstanceSubscriptionState;
  }

  public IncidentState getIncidentState() {
    return incidentState;
  }

  public KeyGenerator getKeyGenerator() {
    return keyState;
  }

  public long getLastSuccessfulProcessedRecordPosition() {
    return lastProcessedPositionState.getPosition();
  }
}
