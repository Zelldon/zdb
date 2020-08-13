package io.zeebe;

import io.zeebe.impl.ZeebeStatusImpl;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "status",
    mixinStandardHelpOptions = true,
    description = "Prints general information of the internal state")
public class StatusCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true)
  private Path partitionPath;

  @Override
  public Integer call() {
    final var partitionState = PartitionState.of(partitionPath);
    final var status = new ZeebeStatusImpl().status(partitionState);
    System.out.println(status);
    return 0;
  }
}
