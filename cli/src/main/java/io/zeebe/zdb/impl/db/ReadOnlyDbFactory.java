/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.zeebe.zdb.impl.db;

import io.zeebe.db.ZeebeDb;
import io.zeebe.db.ZeebeDbFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompactionPriority;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class ReadOnlyDbFactory<ColumnFamilyType extends Enum<ColumnFamilyType>>
    implements ZeebeDbFactory<ColumnFamilyType> {

  static {
    RocksDB.loadLibrary();
  }

  final Class<ColumnFamilyType> columnFamilyTypeClass;

  public ReadOnlyDbFactory(final Class<ColumnFamilyType> columnFamilyTypeClass) {
    this.columnFamilyTypeClass = columnFamilyTypeClass;
  }

  @Override
  public ZeebeDb<ColumnFamilyType> createDb(final File pathName) {
    return open(
        pathName,
        Arrays.stream(columnFamilyTypeClass.getEnumConstants())
            .map(c -> c.name().toLowerCase().getBytes())
            .collect(Collectors.toList()));
  }

  private ZeebeReadOnlyDB<ColumnFamilyType> open(
      final File dbDirectory, final List<byte[]> columnFamilyNames) {

    final ZeebeReadOnlyDB<ColumnFamilyType> db;
    try {
      final List<AutoCloseable> closeables = new ArrayList<>();

      // column family options have to be closed as last
      final ColumnFamilyOptions columnFamilyOptions = createColumnFamilyOptions();
      closeables.add(columnFamilyOptions);

      final List<ColumnFamilyDescriptor> columnFamilyDescriptors =
          createFamilyDescriptors(columnFamilyNames, columnFamilyOptions);
      final DBOptions dbOptions =
          new DBOptions()
              .setCreateMissingColumnFamilies(true)
              .setErrorIfExists(false)
              .setCreateIfMissing(false)
              .setParanoidChecks(true);
      closeables.add(dbOptions);

      db =
          ZeebeReadOnlyDB.openTransactionalDb(
              dbOptions,
              dbDirectory.getAbsolutePath(),
              columnFamilyDescriptors,
              closeables,
              columnFamilyTypeClass);

    } catch (final RocksDBException e) {
      throw new IllegalStateException("Unexpected error occurred trying to open the database", e);
    }
    return db;
  }

  private List<ColumnFamilyDescriptor> createFamilyDescriptors(
      final List<byte[]> columnFamilyNames, final ColumnFamilyOptions columnFamilyOptions) {
    final List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();

    if (columnFamilyNames != null && !columnFamilyNames.isEmpty()) {
      for (final byte[] name : columnFamilyNames) {
        final ColumnFamilyDescriptor columnFamilyDescriptor =
            new ColumnFamilyDescriptor(name, columnFamilyOptions);
        columnFamilyDescriptors.add(columnFamilyDescriptor);
      }
    }
    return columnFamilyDescriptors;
  }

  private static ColumnFamilyOptions createColumnFamilyOptions() {
    // Options which are used on all column families
    return new ColumnFamilyOptions()
        .setCompactionPriority(CompactionPriority.OldestSmallestSeqFirst);
  }
}
