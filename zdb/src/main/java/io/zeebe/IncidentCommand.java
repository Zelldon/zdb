package io.zeebe;

import io.zeebe.impl.IncidentInspection;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(name = "incident", mixinStandardHelpOptions = true)
public class IncidentCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true,
      scope = ScopeType.INHERIT)
  private Path partitionPath;

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return 0;
  }

  @Command(name = "list", description = "List all incidents")
  public int list() {
    final var partitionState = PartitionState.of(partitionPath);
    final var outputLines = new IncidentInspection().list(partitionState);
    outputLines.forEach(System.out::println);
    return 0;
  }

  @Command(name = "entity", description = "Show details about an incident")
  public int entity(
      @Parameters(
              paramLabel = "KEY",
              description = "The key of the incident",
              arity = "1")
          final long key) {
    final var partitionState = PartitionState.of(partitionPath);
    final var output = new IncidentInspection().entity(partitionState, key);
    System.out.println(output);
    return 0;
  }
}
