package io.zeebe;

import io.zeebe.impl.BlacklistInspection;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "blacklist", mixinStandardHelpOptions = true)
public class BlackListCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true,
      scope = ScopeType.INHERIT)
  private Path partitionPath;

  @Override
  public Integer call() {
    ZeebeDebugger.printUsage("blacklist");
    return 0;
  }

  @Command(name = "list", description = "List all blacklisted workflow instances")
  public int list() {
    final var partitionState = PartitionState.of(partitionPath);
    final var outputLines = new BlacklistInspection().list(partitionState);
    outputLines.forEach(System.out::println);
    return 0;
  }

  @Command(name = "entry", description = "Shows details about blacklisted workflow instance")
  public int entry(
      @Option(
              names = {"-k", "--key"},
              description = "The key of the blacklisted workflow instance",
              required = true)
          final long key) {
    final var partitionState = PartitionState.of(partitionPath);
    final var output = new BlacklistInspection().entity(partitionState, key);
    System.out.println(output);
    return 0;
  }
}
