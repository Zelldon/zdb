/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.message;

import static io.zeebe.util.EnsureUtil.ensureGreaterThan;
import static io.zeebe.util.EnsureUtil.ensureNotNullOrEmpty;

import io.zeebe.db.ColumnFamily;
import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.db.impl.DbCompositeKey;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.db.impl.DbString;
import io.zeebe.engine.state.ZbColumnFamilies;
import org.agrona.DirectBuffer;

public final class MessageState {

  /**
   * <pre>message key -> message
   */
  private final ColumnFamily<DbLong, Message> messageColumnFamily;

  private final DbLong messageKey;
  private final Message message;

  /**
   * <pre>name | correlation key | key -> []
   *
   * find message by name and correlation key - the message key ensures the queue ordering
   */
  private final DbString messageName;

  private final DbString correlationKey;
  private final DbCompositeKey<DbCompositeKey<DbString, DbString>, DbLong>
      nameCorrelationMessageKey;
  private final DbCompositeKey<DbString, DbString> nameAndCorrelationKey;
  private final ColumnFamily<DbCompositeKey<DbCompositeKey<DbString, DbString>, DbLong>, DbNil>
      nameCorrelationMessageColumnFamily;

  /**
   * <pre>deadline | key -> []
   *
   * find messages which are before a given timestamp */
  private final DbLong deadline;

  private final DbCompositeKey<DbLong, DbLong> deadlineMessageKey;
  private final ColumnFamily<DbCompositeKey<DbLong, DbLong>, DbNil> deadlineColumnFamily;

  /**
   * <pre>name | correlation key | message id -> []
   *
   * exist a message for a given message name, correlation key and message id */
  private final DbString messageId;

  private final DbCompositeKey<DbCompositeKey<DbString, DbString>, DbString>
      nameCorrelationMessageIdKey;
  private final ColumnFamily<DbCompositeKey<DbCompositeKey<DbString, DbString>, DbString>, DbNil>
      messageIdColumnFamily;

  /**
   * <pre>key | bpmn process id -> []
   *
   * check if a message is correlated to a workflow */
  private final DbCompositeKey<DbLong, DbString> messageBpmnProcessIdKey;

  private final DbString bpmnProcessIdKey;
  private final ColumnFamily<DbCompositeKey<DbLong, DbString>, DbNil> correlatedMessageColumnFamily;

  /**
   * <pre> bpmn process id | correlation key -> []
   *
   * check if a workflow instance is created by this correlation key */
  private final DbCompositeKey<DbString, DbString> bpmnProcessIdCorrelationKey;

  private final ColumnFamily<DbCompositeKey<DbString, DbString>, DbNil>
      activeWorkflowInstancesByCorrelationKeyColumnFamiliy;

  /**
   * <pre> workflow instance key -> correlation key
   *
   * get correlation key by workflow instance key */
  private final DbLong workflowInstanceKey;

  private final ColumnFamily<DbLong, DbString> workflowInstanceCorrelationKeyColumnFamiliy;

  public MessageState(final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    messageKey = new DbLong();
    message = new Message();
    messageColumnFamily =
        zeebeDb.createColumnFamily(ZbColumnFamilies.MESSAGE_KEY, dbContext, messageKey, message);

    messageName = new DbString();
    correlationKey = new DbString();
    nameAndCorrelationKey = new DbCompositeKey<>(messageName, correlationKey);
    nameCorrelationMessageKey = new DbCompositeKey<>(nameAndCorrelationKey, messageKey);
    nameCorrelationMessageColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.MESSAGES, dbContext, nameCorrelationMessageKey, DbNil.INSTANCE);

    deadline = new DbLong();
    deadlineMessageKey = new DbCompositeKey<>(deadline, messageKey);
    deadlineColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.MESSAGE_DEADLINES, dbContext, deadlineMessageKey, DbNil.INSTANCE);

    messageId = new DbString();
    nameCorrelationMessageIdKey = new DbCompositeKey<>(nameAndCorrelationKey, messageId);
    messageIdColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.MESSAGE_IDS, dbContext, nameCorrelationMessageIdKey, DbNil.INSTANCE);

    bpmnProcessIdKey = new DbString();
    messageBpmnProcessIdKey = new DbCompositeKey<>(messageKey, bpmnProcessIdKey);
    correlatedMessageColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.MESSAGE_CORRELATED,
            dbContext,
            messageBpmnProcessIdKey,
            DbNil.INSTANCE);

    bpmnProcessIdCorrelationKey = new DbCompositeKey<>(bpmnProcessIdKey, correlationKey);
    activeWorkflowInstancesByCorrelationKeyColumnFamiliy =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.MESSAGE_WORKFLOWS_ACTIVE_BY_CORRELATION_KEY,
            dbContext,
            bpmnProcessIdCorrelationKey,
            DbNil.INSTANCE);

    workflowInstanceKey = new DbLong();
    workflowInstanceCorrelationKeyColumnFamiliy =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.MESSAGE_WORKFLOW_INSTANCE_CORRELATION_KEYS,
            dbContext,
            workflowInstanceKey,
            correlationKey);
  }

  public boolean existMessageCorrelation(final long messageKey, final DirectBuffer bpmnProcessId) {
    ensureGreaterThan("message key", messageKey, 0);
    ensureNotNullOrEmpty("BPMN process id", bpmnProcessId);

    this.messageKey.wrapLong(messageKey);
    bpmnProcessIdKey.wrapBuffer(bpmnProcessId);

    return correlatedMessageColumnFamily.exists(messageBpmnProcessIdKey);
  }

  public boolean existActiveWorkflowInstance(
      final DirectBuffer bpmnProcessId, final DirectBuffer correlationKey) {
    ensureNotNullOrEmpty("BPMN process id", bpmnProcessId);
    ensureNotNullOrEmpty("correlation key", correlationKey);

    bpmnProcessIdKey.wrapBuffer(bpmnProcessId);
    this.correlationKey.wrapBuffer(correlationKey);
    return activeWorkflowInstancesByCorrelationKeyColumnFamiliy.exists(bpmnProcessIdCorrelationKey);
  }

  public DirectBuffer getWorkflowInstanceCorrelationKey(final long workflowInstanceKey) {
    ensureGreaterThan("workflow instance key", workflowInstanceKey, 0);

    this.workflowInstanceKey.wrapLong(workflowInstanceKey);
    final var correlationKey =
        workflowInstanceCorrelationKeyColumnFamiliy.get(this.workflowInstanceKey);

    return correlationKey != null ? correlationKey.getBuffer() : null;
  }

  public void visitMessages(
      final DirectBuffer name, final DirectBuffer correlationKey, final MessageVisitor visitor) {

    messageName.wrapBuffer(name);
    this.correlationKey.wrapBuffer(correlationKey);

    nameCorrelationMessageColumnFamily.whileEqualPrefix(
        nameAndCorrelationKey,
        (compositeKey, nil) -> {
          final long messageKey = compositeKey.getSecond().getValue();
          final Message message = getMessage(messageKey);
          return visitor.visit(message);
        });
  }

  public Message getMessage(final long messageKey) {
    this.messageKey.wrapLong(messageKey);
    return messageColumnFamily.get(this.messageKey);
  }

  public void visitMessagesWithDeadlineBefore(final long timestamp, final MessageVisitor visitor) {
    deadlineColumnFamily.whileTrue(
        ((compositeKey, zbNil) -> {
          final long deadline = compositeKey.getFirst().getValue();
          if (deadline <= timestamp) {
            final long messageKey = compositeKey.getSecond().getValue();
            final Message message = getMessage(messageKey);
            return visitor.visit(message);
          }
          return false;
        }));
  }

  public boolean exist(
      final DirectBuffer name, final DirectBuffer correlationKey, final DirectBuffer messageId) {
    messageName.wrapBuffer(name);
    this.correlationKey.wrapBuffer(correlationKey);
    this.messageId.wrapBuffer(messageId);

    return messageIdColumnFamily.exists(nameCorrelationMessageIdKey);
  }

  @FunctionalInterface
  public interface MessageVisitor {
    boolean visit(Message message);
  }
}
