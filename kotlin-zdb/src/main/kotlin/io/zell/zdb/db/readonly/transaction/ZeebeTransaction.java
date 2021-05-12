/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zell.zdb.db.readonly.transaction;

import io.camunda.zeebe.db.TransactionOperation;
import io.camunda.zeebe.db.ZeebeDbTransaction;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class ZeebeTransaction implements ZeebeDbTransaction {

  private final long nativeHandle;
  private final RocksDB rocksDB;

  public ZeebeTransaction(
      final RocksDB rocksDB) {
    this.rocksDB = rocksDB;
    try {
      nativeHandle = RocksDbInternal.nativeHandle.getLong(rocksDB);
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public byte[] get(
      final long columnFamilyHandle,
      final long readOptionsHandle,
      final byte[] key,
      final int keyLength)
      throws Exception {
    return (byte[])
        RocksDbInternal.getWithHandle.invoke(nativeHandle, readOptionsHandle, key, keyLength, columnFamilyHandle);
  }

  public RocksIterator newIterator(final ReadOptions options, final ColumnFamilyHandle handle) {
    return rocksDB.newIterator(handle, options);
  }

  @Override
  public void run(final TransactionOperation operations) throws Exception {
    try {
      operations.run();
    } catch (final RocksDBException rdbex) {
      throw rdbex;
    }
  }

  @Override
  public void commit() throws RocksDBException {

  }

  @Override
  public void rollback() throws RocksDBException {

  }

}
