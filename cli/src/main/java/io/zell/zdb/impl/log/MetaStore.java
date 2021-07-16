/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
///*
// * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
// * one or more contributor license agreements. See the NOTICE file distributed
// * with this work for additional information regarding copyright ownership.
// * Licensed under the Zeebe Community License 1.1. You may not use this file
// * except in compliance with the Zeebe Community License 1.1.
// */
//package io.zeebe.zdb.impl.log;
//
//import static com.google.common.base.MoreObjects.toStringHelper;
//
//import io.atomix.cluster.MemberId;
//import io.atomix.raft.partition.impl.RaftNamespaces;
//import io.atomix.raft.storage.RaftStorage;
//import io.atomix.raft.storage.system.Configuration;
//import io.atomix.storage.buffer.Buffer;
//import io.atomix.storage.buffer.FileBuffer;
//import io.atomix.utils.serializer.Serializer;
//import java.io.File;
//import java.nio.file.Path;
//
///**
// * Manages persistence of server configurations.
// *
// * <p>The server metastore is responsible for persisting server configurations according to the
// * configured {@link RaftStorage#storageLevel() storage level}. Each server persists their current
// * {@link #loadTerm() term} and last {@link #loadVote() vote} as is dictated by the Raft consensus
// * algorithm. Additionally, the metastore is responsible for storing the last know server {@link
// * io.atomix.raft.storage.system.Configuration}, including cluster membership.
// */
//public class MetaStore implements AutoCloseable {
//
//  private final Serializer serializer;
//  private final FileBuffer metadataBuffer;
//  private final Buffer configurationBuffer;
//
//  public MetaStore(final Path path) {
//    this.serializer = Serializer.using(RaftNamespaces.RAFT_STORAGE);
//
//    // Note that for raft safety, irrespective of the storage level, <term, vote> metadata is always
//    // persisted on disk.
//    var files = path.toFile().listFiles((file, fileName) -> fileName.endsWith(".meta"));
//
//    if (files == null || files.length != 1) {
//      throw new IllegalArgumentException("Expected .meta file in path " + path.toFile().getName());
//    }
//
//    final var metaFile = files[0];
//    metadataBuffer = FileBuffer.allocate(metaFile, 12);
//
//    files = path.toFile().listFiles((file, fileName) -> fileName.endsWith(".conf"));
//
//    if (files == null || files.length != 1) {
//      throw new IllegalArgumentException("Expected .conf file in path " + path.toFile().getName());
//    }
//
//    final File confFile = files[0];
//    configurationBuffer = FileBuffer.allocate(confFile, 32);
//  }
//
//  /**
//   * Loads the stored server term.
//   *
//   * @return The stored server term.
//   */
//  public synchronized long loadTerm() {
//    return metadataBuffer.readLong(0);
//  }
//
//  /**
//   * Loads the last vote for the server.
//   *
//   * @return The last vote for the server.
//   */
//  public synchronized MemberId loadVote() {
//    final String id = metadataBuffer.readString(8);
//    return id != null ? MemberId.from(id) : null;
//  }
//
//  /**
//   * Loads the current cluster configuration.
//   *
//   * @return The current cluster configuration.
//   */
//  public synchronized Configuration loadConfiguration() {
//    if (configurationBuffer.position(0).readByte() == 1) {
//      final int bytesLength = configurationBuffer.readInt();
//      if (bytesLength == 0) {
//        return null;
//      }
//      return serializer.decode(configurationBuffer.readBytes(bytesLength));
//    }
//    return null;
//  }
//
//  @Override
//  public synchronized void close() {
//    metadataBuffer.close();
//    configurationBuffer.close();
//  }
//
//  @Override
//  public String toString() {
//    return toStringHelper(this).toString();
//  }
//}
