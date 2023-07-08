/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb.journal;

public interface ReadOnlyJournalMetaStore {

  /**
   * Read last flushed index from the metastore. This method might be expensive and blocking as the
   * implementations of this may have to read from a database or file. It is recommended for the
   * callers of this method to cache lastFlushedIndex and call this method only when necessary.
   *
   * @return last flushed index
   */
  long loadLastFlushedIndex();

  /** Returns true if there is no known last flushed index. */
  boolean hasLastFlushedIndex();

  class InMemory implements ReadOnlyJournalMetaStore {
    private volatile long index = -1L;

    @Override
    public long loadLastFlushedIndex() {
      return index;
    }

    @Override
    public boolean hasLastFlushedIndex() {
      return index != -1L;
    }
  }
}
