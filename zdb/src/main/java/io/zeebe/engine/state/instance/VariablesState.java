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
import io.zeebe.db.impl.DbString;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.msgpack.spec.MsgPackWriter;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.ObjectHashSet;
import org.agrona.concurrent.UnsafeBuffer;

public class VariablesState {

  public static final int NO_PARENT = -1;

  private final MsgPackWriter writer = new MsgPackWriter();
  private final ExpandableArrayBuffer documentResultBuffer = new ExpandableArrayBuffer();
  private final DirectBuffer resultView = new UnsafeBuffer(0, 0);

  private final ParentScopeKey parentKey = new ParentScopeKey();

  // collecting variables
  private final ObjectHashSet<DirectBuffer> collectedVariables = new ObjectHashSet<>();
  private final ZeebeDb<ZbColumnFamilies> zeebeDb;
  private final DbContext dbContext;

  public VariablesState(final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    this.zeebeDb = zeebeDb;
    this.dbContext = dbContext;
  }

  private long getParent(final long key) {
    final DbLong childKey = new DbLong();
    childKey.wrapLong(key);

    final ColumnFamily<DbLong, ParentScopeKey> childParentColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.ELEMENT_INSTANCE_CHILD_PARENT, dbContext, childKey, parentKey);

    final ParentScopeKey result = childParentColumnFamily.get(childKey);
    return result != null ? result.get() : NO_PARENT;
  }

  public DirectBuffer getVariablesAsDocument(final long scopeKey) {

    collectedVariables.clear();
    writer.wrap(documentResultBuffer, 0);

    writer.reserveMapHeader();

    visitVariables(
        scopeKey,
        name -> !collectedVariables.contains(name.getBuffer()),
        (name, value) -> {
          final DirectBuffer variableNameBuffer = name.getBuffer();
          writer.writeString(variableNameBuffer);
          writer.writeRaw(value.getValue());

          // must create a new name wrapper, because we keep them all in the hashset at the same
          // time
          final MutableDirectBuffer nameView = new UnsafeBuffer(variableNameBuffer);
          collectedVariables.add(nameView);
        },
        () -> false);

    writer.writeReservedMapHeader(0, collectedVariables.size());

    resultView.wrap(documentResultBuffer, 0, writer.getOffset());
    return resultView;
  }

  /**
   * Like {@link #visitVariablesLocal(long, Predicate, BiConsumer, BooleanSupplier)} but walks up
   * the scope hierarchy.
   */
  private void visitVariables(
      final long scopeKey,
      final Predicate<DbString> filter,
      final BiConsumer<DbString, VariableInstance> variableConsumer,
      final BooleanSupplier completionCondition) {
    long currentScope = scopeKey;

    boolean completed;
    do {
      completed = visitVariablesLocal(currentScope, filter, variableConsumer, completionCondition);

      currentScope = getParent(currentScope);

    } while (!completed && currentScope >= 0);
  }

  /**
   * Provides all variables of a scope to the given consumer until a condition is met.
   *
   * @param scopeKey
   * @param variableFilter evaluated with the name of each variable; the variable is consumed only
   *     if the filter returns true
   * @param variableConsumer a consumer that receives variable name and value
   * @param completionCondition evaluated after every consumption; if true, consumption stops.
   * @return true if the completion condition was met
   */
  private boolean visitVariablesLocal(
      final long scopeKey,
      final Predicate<DbString> variableFilter,
      final BiConsumer<DbString, VariableInstance> variableConsumer,
      final BooleanSupplier completionCondition) {

    final DbLong dbScopeKey = new DbLong();
    final DbCompositeKey<DbLong, DbString> scopeKeyVariableNameKey =
        new DbCompositeKey<>(dbScopeKey, new DbString());

    dbScopeKey.wrapLong(scopeKey);

    zeebeDb
        .createColumnFamily(
            ZbColumnFamilies.VARIABLES, dbContext, scopeKeyVariableNameKey, new VariableInstance())
        .whileEqualPrefix(
            dbScopeKey,
            (compositeKey, variable) -> {
              final DbString variableName = compositeKey.getSecond();

              if (variableFilter.test(variableName)) {
                variableConsumer.accept(variableName, variable);
              }

              return !completionCondition.getAsBoolean();
            });
    return false;
  }
}
