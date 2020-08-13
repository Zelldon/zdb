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
package io.zeebe;

import io.zeebe.broker.exporter.stream.ExportersState;
import io.zeebe.db.DbContext;
import io.zeebe.db.ReadOnlyDbFactory;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.ZbColumnFamilies;
import io.zeebe.engine.state.ZeebeState;
import java.nio.file.Path;

public final class PartitionState {

  private final ZeebeDb<ZbColumnFamilies> zeebeDb;
  private final DbContext dbContext;
  private final ZeebeState zeebeState;
  private final ExportersState exporterState;

  private PartitionState(Path path) {
    this.zeebeDb = openZeebeDb(path);
    this.dbContext = zeebeDb.createContext();
    this.zeebeState = new ZeebeState(1, zeebeDb, dbContext);
    this.exporterState = new ExportersState(zeebeDb, dbContext);
  }

  static PartitionState of(Path path) {
    return new PartitionState(path);
  }

  public ZeebeDb<ZbColumnFamilies> getZeebeDb() {
    return zeebeDb;
  }

  public DbContext getDbContext() {
    return dbContext;
  }

  public ZeebeState getZeebeState() {
    return zeebeState;
  }

  public ExportersState getExporterState() {
    return exporterState;
  }

  private static ZeebeDb<ZbColumnFamilies> openZeebeDb(Path directory) {
    return new ReadOnlyDbFactory(ZbColumnFamilies.class).createDb(directory.toFile());
  }
}
