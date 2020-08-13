/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
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
package io.zeebe.db;

import static io.zeebe.util.buffer.BufferUtil.startsWith;

import io.zeebe.db.impl.rocksdb.Loggers;
import io.zeebe.db.impl.rocksdb.transaction.RocksDbInternal;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.RocksObject;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;

public class ZeebeReadOnlyDB<ColumnFamilyNames extends Enum<ColumnFamilyNames>>
    implements ZeebeDb<ColumnFamilyNames> {

  private static final Logger LOG = Loggers.DB_LOGGER;
  private static final String ERROR_MESSAGE_CLOSE_RESOURCE =
      "Expected to close RocksDB resource successfully, but exception was thrown. Will continue to close remaining resources.";
  private final RocksDB readOnlyDB;
  private final List<AutoCloseable> closables;
  private final EnumMap<ColumnFamilyNames, Long> columnFamilyMap;
  private final Long2ObjectHashMap<ColumnFamilyHandle> handelToEnumMap;
  private final ReadOptions prefixReadOptions;
  private final ReadOptions defaultReadOptions;
  private final WriteOptions defaultWriteOptions;

  protected ZeebeReadOnlyDB(
      final RocksDB optimisticTransactionDB,
      final EnumMap<ColumnFamilyNames, Long> columnFamilyMap,
      final Long2ObjectHashMap<ColumnFamilyHandle> handelToEnumMap,
      final List<AutoCloseable> closables) {
    this.readOnlyDB = optimisticTransactionDB;
    this.columnFamilyMap = columnFamilyMap;
    this.handelToEnumMap = handelToEnumMap;
    this.closables = closables;

    prefixReadOptions = new ReadOptions().setPrefixSameAsStart(true).setTotalOrderSeek(false);
    closables.add(prefixReadOptions);
    defaultReadOptions = new ReadOptions();
    closables.add(defaultReadOptions);
    defaultWriteOptions = new WriteOptions();
    closables.add(defaultWriteOptions);
  }

  public static <ColumnFamilyNames extends Enum<ColumnFamilyNames>>
      ZeebeReadOnlyDB<ColumnFamilyNames> openTransactionalDb(
          final DBOptions options,
          final String path,
          final List<ColumnFamilyDescriptor> columnFamilyDescriptors,
          final List<AutoCloseable> closables,
          final Class<ColumnFamilyNames> columnFamilyTypeClass)
          throws RocksDBException {
    final EnumMap<ColumnFamilyNames, Long> columnFamilyMap = new EnumMap<>(columnFamilyTypeClass);

    final List<ColumnFamilyHandle> handles = new ArrayList<>();
    final RocksDB optimisticTransactionDB =
        RocksDB.openReadOnly(options, path, columnFamilyDescriptors, handles);
    closables.add(optimisticTransactionDB);

    final ColumnFamilyNames[] enumConstants = columnFamilyTypeClass.getEnumConstants();
    final Long2ObjectHashMap<ColumnFamilyHandle> handleToEnumMap = new Long2ObjectHashMap<>();
    for (int i = 0; i < handles.size(); i++) {
      final ColumnFamilyHandle columnFamilyHandle = handles.get(i);
      closables.add(columnFamilyHandle);
      columnFamilyMap.put(enumConstants[i], getNativeHandle(columnFamilyHandle));
      handleToEnumMap.put(getNativeHandle(handles.get(i)), handles.get(i));
    }

    return new ZeebeReadOnlyDB<>(
        optimisticTransactionDB, columnFamilyMap, handleToEnumMap, closables);
  }

  private static long getNativeHandle(final RocksObject object) {
    try {
      final Field nativeHandle = RocksObject.class.getDeclaredField("nativeHandle_");
      nativeHandle.setAccessible(true);
      return nativeHandle.getLong(object);
    } catch (final IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(
          "Unexpected error occurred trying to access private nativeHandle_ field", e);
    }
  }

  long getColumnFamilyHandle(final ColumnFamilyNames columnFamily) {
    return columnFamilyMap.get(columnFamily);
  }

  @Override
  public <KeyType extends DbKey, ValueType extends DbValue>
      ColumnFamily<KeyType, ValueType> createColumnFamily(
          final ColumnFamilyNames columnFamily,
          final DbContext context,
          final KeyType keyInstance,
          final ValueType valueInstance) {
    return new ReadOnlyColumnFamily<>(this, columnFamily, context, keyInstance, valueInstance);
  }

  @Override
  public void createSnapshot(final File snapshotDir) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<String> getProperty(
      final ColumnFamilyNames columnFamilyName, final String propertyName) {

    final var handle = handelToEnumMap.get(columnFamilyMap.get(columnFamilyName));

    String propertyValue = null;
    try {
      propertyValue = readOnlyDB.getProperty(handle, propertyName);
    } catch (final RocksDBException rde) {
      LOG.debug(rde.getMessage(), rde);
    }
    return Optional.ofNullable(propertyValue);
  }

  ////////////////////////////////////////////////////////////////////
  //////////////////////////// GET ///////////////////////////////////
  ////////////////////////////////////////////////////////////////////

  @Override
  public DbContext createContext() {
    return new ReadOnlyContext(readOnlyDB);
  }

  protected void put(
      final long columnFamilyHandle,
      final DbContext context,
      final DbKey key,
      final DbValue value) {
    throw new UnsupportedOperationException();
  }

  protected DirectBuffer get(
      final long columnFamilyHandle, final DbContext context, final DbKey key) {
    context.writeKey(key);
    final int keyLength = key.getLength();
    return getValue(columnFamilyHandle, context, keyLength);
  }

  private DirectBuffer getValue(
      final long columnFamilyHandle, final DbContext context, final int keyLength) {

    final byte[] value;
    try {
      value =
          readOnlyDB.get(
              handelToEnumMap.get(columnFamilyHandle),
              defaultReadOptions,
              context.getKeyBufferArray(),
              0,
              keyLength);
      context.wrapValueView(value);
      return context.getValueView();
    } catch (RocksDBException e) {
      e.printStackTrace();
    }

    return null;
  }

  ////////////////////////////////////////////////////////////////////
  //////////////////////////// ITERATION /////////////////////////////
  ////////////////////////////////////////////////////////////////////

  protected boolean exists(
      final long columnFamilyHandle, final DbContext context, final DbKey key) {
    context.wrapValueView(new byte[0]);

    context.writeKey(key);
    getValue(columnFamilyHandle, context, key.getLength());

    return !context.isValueViewEmpty();
  }

  protected void delete(final long columnFamilyHandle, final DbContext context, final DbKey key) {
    throw new UnsupportedOperationException();
  }

  ////////////////////////////////////////////////////////////////////
  //////////////////////////// ITERATION /////////////////////////////
  ////////////////////////////////////////////////////////////////////

  RocksIterator newIterator(
      final long columnFamilyHandle, final DbContext context, final ReadOptions options) {
    final ColumnFamilyHandle handle = handelToEnumMap.get(columnFamilyHandle);
    return context.newIterator(options, handle);
  }

  public <ValueType extends DbValue> void foreach(
      final long columnFamilyHandle,
      final DbContext context,
      final ValueType iteratorValue,
      final Consumer<ValueType> consumer) {
    foreach(
        columnFamilyHandle,
        context,
        (keyBuffer, valueBuffer) -> {
          iteratorValue.wrap(valueBuffer, 0, valueBuffer.capacity());
          consumer.accept(iteratorValue);
        });
  }

  public <KeyType extends DbKey, ValueType extends DbValue> void foreach(
      final long columnFamilyHandle,
      final DbContext context,
      final KeyType iteratorKey,
      final ValueType iteratorValue,
      final BiConsumer<KeyType, ValueType> consumer) {
    foreach(
        columnFamilyHandle,
        context,
        (keyBuffer, valueBuffer) -> {
          iteratorKey.wrap(keyBuffer, 0, keyBuffer.capacity());
          iteratorValue.wrap(valueBuffer, 0, valueBuffer.capacity());
          consumer.accept(iteratorKey, iteratorValue);
        });
  }

  private void foreach(
      final long columnFamilyHandle,
      final DbContext context,
      final BiConsumer<DirectBuffer, DirectBuffer> keyValuePairConsumer) {

    try (final RocksIterator iterator =
        newIterator(columnFamilyHandle, context, defaultReadOptions)) {
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        context.wrapKeyView(iterator.key());
        context.wrapValueView(iterator.value());
        keyValuePairConsumer.accept(context.getKeyView(), context.getValueView());
      }
    }
  }

  public <KeyType extends DbKey, ValueType extends DbValue> void whileTrue(
      final long columnFamilyHandle,
      final DbContext context,
      final KeyType keyInstance,
      final ValueType valueInstance,
      final KeyValuePairVisitor<KeyType, ValueType> visitor) {

    try (final RocksIterator iterator =
        newIterator(columnFamilyHandle, context, defaultReadOptions)) {
      boolean shouldVisitNext = true;
      for (iterator.seekToFirst(); iterator.isValid() && shouldVisitNext; iterator.next()) {
        shouldVisitNext = visit(context, keyInstance, valueInstance, visitor, iterator);
      }
    }
  }

  protected <KeyType extends DbKey, ValueType extends DbValue> void whileEqualPrefix(
      final long columnFamilyHandle,
      final DbContext context,
      final DbKey prefix,
      final KeyType keyInstance,
      final ValueType valueInstance,
      final BiConsumer<KeyType, ValueType> visitor) {
    whileEqualPrefix(
        columnFamilyHandle,
        context,
        prefix,
        keyInstance,
        valueInstance,
        (k, v) -> {
          visitor.accept(k, v);
          return true;
        });
  }

  /**
   * NOTE: it doesn't seem possible in Java RocksDB to set a flexible prefix extractor on iterators
   * at the moment, so using prefixes seem to be mostly related to skipping files that do not
   * contain keys with the given prefix (which is useful anyway), but it will still iterate over all
   * keys contained in those files, so we still need to make sure the key actually matches the
   * prefix.
   *
   * <p>While iterating over subsequent keys we have to validate it.
   */
  protected <KeyType extends DbKey, ValueType extends DbValue> void whileEqualPrefix(
      final long columnFamilyHandle,
      final DbContext context,
      final DbKey prefix,
      final KeyType keyInstance,
      final ValueType valueInstance,
      final KeyValuePairVisitor<KeyType, ValueType> visitor) {
    context.withPrefixKeyBuffer(
        prefixKeyBuffer -> {
          try (final RocksIterator iterator =
              newIterator(columnFamilyHandle, context, prefixReadOptions)) {
            prefix.write(prefixKeyBuffer, 0);
            final int prefixLength = prefix.getLength();

            boolean shouldVisitNext = true;

            for (RocksDbInternal.seek(
                    iterator, getNativeHandle(iterator), prefixKeyBuffer.byteArray(), prefixLength);
                iterator.isValid() && shouldVisitNext;
                iterator.next()) {
              final byte[] keyBytes = iterator.key();
              if (!startsWith(
                  prefixKeyBuffer.byteArray(),
                  0,
                  prefix.getLength(),
                  keyBytes,
                  0,
                  keyBytes.length)) {
                break;
              }

              shouldVisitNext = visit(context, keyInstance, valueInstance, visitor, iterator);
            }
          }
        });
  }

  private <KeyType extends DbKey, ValueType extends DbValue> boolean visit(
      final DbContext context,
      final KeyType keyInstance,
      final ValueType valueInstance,
      final KeyValuePairVisitor<KeyType, ValueType> iteratorConsumer,
      final RocksIterator iterator) {
    context.wrapKeyView(iterator.key());
    context.wrapValueView(iterator.value());

    final DirectBuffer keyViewBuffer = context.getKeyView();
    keyInstance.wrap(keyViewBuffer, 0, keyViewBuffer.capacity());
    final DirectBuffer valueViewBuffer = context.getValueView();
    valueInstance.wrap(valueViewBuffer, 0, valueViewBuffer.capacity());

    return iteratorConsumer.visit(keyInstance, valueInstance);
  }

  public boolean isEmpty(final long columnFamilyHandle, final DbContext context) {
    final AtomicBoolean isEmpty = new AtomicBoolean(false);

    try (final RocksIterator iterator =
        newIterator(columnFamilyHandle, context, defaultReadOptions)) {
      iterator.seekToFirst();
      final boolean hasEntry = iterator.isValid();
      isEmpty.set(!hasEntry);
    }

    return isEmpty.get();
  }

  @Override
  public void close() {
    // Correct order of closing
    // 1. transaction
    // 2. options
    // 3. column family handles
    // 4. database
    // 5. db options
    // 6. column family options
    // https://github.com/facebook/rocksdb/wiki/RocksJava-Basics#opening-a-database-with-column-families
    Collections.reverse(closables);
    closables.forEach(
        closable -> {
          try {
            closable.close();
          } catch (final Exception e) {
            LOG.error(ERROR_MESSAGE_CLOSE_RESOURCE, e);
          }
        });
  }
}
