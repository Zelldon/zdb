package io.zeebe;

import io.zeebe.broker.exporter.stream.ExportersState;
import io.zeebe.db.DbContext;
import io.zeebe.db.ZeebeDb;
import io.zeebe.engine.state.DefaultZeebeDbFactory;
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
    return DefaultZeebeDbFactory.DEFAULT_DB_FACTORY.createDb(directory.toFile());
  }
}
