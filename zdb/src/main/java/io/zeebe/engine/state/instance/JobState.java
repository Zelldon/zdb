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
import io.zeebe.db.impl.DbCompositeKey;
import io.zeebe.db.impl.DbLong;
import io.zeebe.db.impl.DbNil;
import io.zeebe.db.impl.DbString;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.protocol.impl.record.value.job.JobRecord;
import io.zeebe.util.EnsureUtil;
import io.zeebe.util.buffer.BufferUtil;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;

public final class JobState {

  // key => job record value
  // we need two separate wrapper to not interfere with get and put
  // see https://github.com/zeebe-io/zeebe/issues/1914
  private final JobRecordValue jobRecordToRead = new JobRecordValue();

  private final DbLong jobKey;
  private final ColumnFamily<DbLong, JobRecordValue> jobsColumnFamily;

  // key => job state
  private final JobStateValue jobState = new JobStateValue();
  private final ColumnFamily<DbLong, JobStateValue> statesJobColumnFamily;

  // type => [key]
  private final DbString jobTypeKey;
  private final DbCompositeKey<DbString, DbLong> typeJobKey;
  private final ColumnFamily<DbCompositeKey<DbString, DbLong>, DbNil> activatableColumnFamily;

  // timeout => key
  private final DbLong deadlineKey;
  private final DbCompositeKey<DbLong, DbLong> deadlineJobKey;
  private final ColumnFamily<DbCompositeKey<DbLong, DbLong>, DbNil> deadlinesColumnFamily;

  private Consumer<String> onJobsAvailableCallback;

  public JobState(final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    jobKey = new DbLong();
    jobsColumnFamily =
        zeebeDb.createColumnFamily(ZbColumnFamilies.JOBS, dbContext, jobKey, jobRecordToRead);

    statesJobColumnFamily =
        zeebeDb.createColumnFamily(ZbColumnFamilies.JOB_STATES, dbContext, jobKey, jobState);

    jobTypeKey = new DbString();
    typeJobKey = new DbCompositeKey<>(jobTypeKey, jobKey);
    activatableColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.JOB_ACTIVATABLE, dbContext, typeJobKey, DbNil.INSTANCE);

    deadlineKey = new DbLong();
    deadlineJobKey = new DbCompositeKey<>(deadlineKey, jobKey);
    deadlinesColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.JOB_DEADLINES, dbContext, deadlineJobKey, DbNil.INSTANCE);
  }

  private void validateParameters(final DirectBuffer type) {
    EnsureUtil.ensureNotNullOrEmpty("type", type);
  }

  public void forEachTimedOutEntry(
      final long upperBound, final BiFunction<Long, JobRecord, Boolean> callback) {

    deadlinesColumnFamily.whileTrue(
        (compositeKey, zbNil) -> {
          final long deadline = compositeKey.getFirst().getValue();
          final boolean isDue = deadline < upperBound;
          if (isDue) {
            final long jobKey = compositeKey.getSecond().getValue();
            return visitJob(jobKey, callback, () -> deadlinesColumnFamily.delete(compositeKey));
          }
          return false;
        });
  }

  public boolean exists(final long jobKey) {
    this.jobKey.wrapLong(jobKey);
    return jobsColumnFamily.exists(this.jobKey);
  }

  public State getState(final long key) {
    jobKey.wrapLong(key);

    final JobStateValue storedState = statesJobColumnFamily.get(jobKey);

    if (storedState == null) {
      return State.NOT_FOUND;
    }

    return storedState.getState();
  }

  public boolean isInState(final long key, final State state) {
    return getState(key) == state;
  }

  public void forEachActivatableJobs(
      final DirectBuffer type, final BiFunction<Long, JobRecord, Boolean> callback) {
    jobTypeKey.wrapBuffer(type);

    activatableColumnFamily.whileEqualPrefix(
        jobTypeKey,
        ((compositeKey, zbNil) -> {
          final long jobKey = compositeKey.getSecond().getValue();
          return visitJob(jobKey, callback, () -> activatableColumnFamily.delete(compositeKey));
        }));
  }

  boolean visitJob(
      final long jobKey,
      final BiFunction<Long, JobRecord, Boolean> callback,
      final Runnable cleanupRunnable) {
    final JobRecord job = getJob(jobKey);
    if (job == null) {
      System.err.printf("Expected to find job with key %d, but no job found%n", jobKey);
      cleanupRunnable.run();
      return true; // we want to continue with the iteration
    }
    return callback.apply(jobKey, job);
  }

  public JobRecord getJob(final long key) {
    jobKey.wrapLong(key);
    final JobRecordValue jobState = jobsColumnFamily.get(jobKey);
    return jobState == null ? null : jobState.getRecord();
  }

  public void setJobsAvailableCallback(final Consumer<String> onJobsAvailableCallback) {
    this.onJobsAvailableCallback = onJobsAvailableCallback;
  }

  private void notifyJobAvailable(final DirectBuffer jobType) {
    if (onJobsAvailableCallback != null) {
      onJobsAvailableCallback.accept(BufferUtil.bufferAsString(jobType));
    }
  }

  public enum State {
    ACTIVATABLE((byte) 0),
    ACTIVATED((byte) 1),
    FAILED((byte) 2),
    NOT_FOUND((byte) 3),
    ERROR_THROWN((byte) 4);

    byte value;

    State(final byte value) {
      this.value = value;
    }
  }
}
