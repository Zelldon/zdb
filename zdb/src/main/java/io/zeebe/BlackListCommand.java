package io.zeebe;

import io.zeebe.impl.BlacklistInspection;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "blacklist", mixinStandardHelpOptions = true)
public class BlackListCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    ZeebeDebugger.printUsage("blacklist");
    return 0;
  }

  @Command(
      name = "list",
      mixinStandardHelpOptions = true,
      description = "List all blacklisted workflow instances")
  public int list(
      @Option(
              names = {"-p", "--path"},
              paramLabel = "PARTITION_PATH",
              description =
                  "The path to the partition data (either runtime or snapshot in partition dir)",
              required = true)
          Path partitionPath) {
    final var partitionState = PartitionState.of(partitionPath);
    final var outputLines = new BlacklistInspection().list(partitionState);
    outputLines.forEach(System.out::println);
    return 0;
  }
}
