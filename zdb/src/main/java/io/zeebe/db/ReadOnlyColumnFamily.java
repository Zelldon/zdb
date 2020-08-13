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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;

class ReadOnlyColumnFamily<
        ColumnFamilyNames extends Enum<ColumnFamilyNames>,
        KeyType extends DbKey,
        ValueType extends DbValue>
    implements ColumnFamily<KeyType, ValueType> {

  private final ZeebeReadOnlyDB<ColumnFamilyNames> transactionDb;
  private final long handle;

  private final DbContext context;

  private final ValueType valueInstance;
  private final KeyType keyInstance;

  ReadOnlyColumnFamily(
      final ZeebeReadOnlyDB<ColumnFamilyNames> transactionDb,
      final ColumnFamilyNames columnFamily,
      final DbContext context,
      final KeyType keyInstance,
      final ValueType valueInstance) {
    this.transactionDb = transactionDb;
    handle = this.transactionDb.getColumnFamilyHandle(columnFamily);
    this.context = context;
    this.keyInstance = keyInstance;
    this.valueInstance = valueInstance;
  }

  @Override
  public void put(final KeyType key, final ValueType value) {
    put(context, key, value);
  }

  @Override
  public void put(final DbContext context, final KeyType key, final ValueType value) {
    throw new UnsupportedOperationException("Cannot write to read only database");
  }

  @Override
  public ValueType get(final KeyType key) {
    return get(context, key);
  }

  @Override
  public ValueType get(final DbContext context, final KeyType key, final ValueType value) {
    final DirectBuffer valueBuffer = transactionDb.get(handle, context, key);
    if (valueBuffer != null) {

      value.wrap(valueBuffer, 0, valueBuffer.capacity());
      return value;
    }
    return null;
  }

  @Override
  public void forEach(final Consumer<ValueType> consumer) {
    forEach(context, consumer);
  }

  @Override
  public void forEach(final BiConsumer<KeyType, ValueType> consumer) {
    forEach(context, consumer);
  }

  @Override
  public void whileTrue(final KeyValuePairVisitor<KeyType, ValueType> visitor) {
    whileTrue(context, visitor);
  }

  @Override
  public void whileTrue(
      final DbContext context,
      final KeyValuePairVisitor<KeyType, ValueType> visitor,
      final KeyType key,
      final ValueType value) {
    transactionDb.whileTrue(handle, context, key, value, visitor);
  }

  @Override
  public void whileEqualPrefix(
      final DbKey keyPrefix, final BiConsumer<KeyType, ValueType> visitor) {
    whileEqualPrefix(context, keyPrefix, visitor);
  }

  @Override
  public void whileEqualPrefix(
      final DbKey keyPrefix, final KeyValuePairVisitor<KeyType, ValueType> visitor) {
    whileEqualPrefix(context, keyPrefix, visitor);
  }

  @Override
  public void delete(final KeyType key) {
    delete(context, key);
  }

  @Override
  public void delete(final DbContext context, final KeyType key) {
    throw new UnsupportedOperationException("Cannot modify read only database");
  }

  @Override
  public boolean exists(final KeyType key) {
    return exists(context, key);
  }

  @Override
  public boolean isEmpty() {
    return isEmpty(context);
  }

  @Override
  public boolean isEmpty(final DbContext context) {
    return transactionDb.isEmpty(handle, context);
  }

  public ValueType get(final DbContext context, final KeyType key) {
    return get(context, key, valueInstance);
  }

  public void forEach(final DbContext context, final Consumer<ValueType> consumer) {
    transactionDb.foreach(handle, context, valueInstance, consumer);
  }

  public void forEach(final DbContext context, final BiConsumer<KeyType, ValueType> consumer) {
    transactionDb.foreach(handle, context, keyInstance, valueInstance, consumer);
  }

  public void whileTrue(
      final DbContext context, final KeyValuePairVisitor<KeyType, ValueType> visitor) {
    whileTrue(context, visitor, keyInstance, valueInstance);
  }

  public void whileEqualPrefix(
      final DbContext context,
      final DbKey keyPrefix,
      final BiConsumer<KeyType, ValueType> visitor) {
    transactionDb.whileEqualPrefix(handle, context, keyPrefix, keyInstance, valueInstance, visitor);
  }

  public void whileEqualPrefix(
      final DbContext context,
      final DbKey keyPrefix,
      final KeyValuePairVisitor<KeyType, ValueType> visitor) {
    transactionDb.whileEqualPrefix(handle, context, keyPrefix, keyInstance, valueInstance, visitor);
  }

  public boolean exists(final DbContext context, final KeyType key) {
    return transactionDb.exists(handle, context, key);
  }
}
