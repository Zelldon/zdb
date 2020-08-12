package io.zeebe;

import io.zeebe.impl.BlacklistInspection;
import io.zeebe.impl.ExporterInspection;
import io.zeebe.impl.IncidentInspection;
import io.zeebe.impl.WorkflowInspection;
import io.zeebe.impl.ZeebeStatusImpl;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeebeDebugerMain {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeDebugerMain.class);

  private static final Map<String, EntityInspection> COMMAND_MAP =
      Map.of("blacklist", new BlacklistInspection(),
          "incident", new IncidentInspection(),
          "exporter", new ExporterInspection(),
          "workflow", new WorkflowInspection());

  public static void main(String[] args) throws Exception {
    LOGGER.info("Zeebe Debug and inspection tool");

    // parse given parameters - exit with error code if necessary
    final var partitionPath = Path.of(args[0]);

    // call corresponding command
    final var partitionState = PartitionState.of(partitionPath);
    final var result = new ZeebeStatusImpl().status(partitionState);

    // print result
    LOGGER.info(result);
  }
}
