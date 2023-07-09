/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        RocksDbInternal.getWithHandle.invoke(rocksDB, nativeHandle, readOptionsHandle, key, 0, keyLength);
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
