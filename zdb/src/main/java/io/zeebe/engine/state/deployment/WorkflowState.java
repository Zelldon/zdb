/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.state.deployment;

import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.instance.ElementInstanceState;

public final class WorkflowState {

  private final ElementInstanceState elementInstanceState;

  public WorkflowState(final ZeebeDb<ZbColumnFamilies> zeebeDb, final DbContext dbContext) {
    elementInstanceState = new ElementInstanceState(zeebeDb, dbContext);
  }

  public ElementInstanceState getElementInstanceState() {
    return elementInstanceState;
  }
}
