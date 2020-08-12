package io.zeebe;

import io.zeebe.impl.ZeebeStatusImpl;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "status", mixinStandardHelpOptions = true)
public class StatusCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "the path to the partition data (either runtime or snapshot in partition dir)",
      required = true)
  private File partitionPath;

  @Override
  public Integer call() {
    final var partitionState = PartitionState.of(partitionPath.toPath());
    final var status = new ZeebeStatusImpl().status(partitionState);
    System.out.println(status);
    return 0;
  }
}
