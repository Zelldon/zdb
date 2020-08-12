package io.zeebe;

import io.zeebe.impl.WorkflowInspection;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(name = "workflow", mixinStandardHelpOptions = true)
public class WorkflowCommand implements Callable<Integer> {

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

  @Command(name = "list", description = "List all workflows")
  public int list() {
    final var partitionState = PartitionState.of(partitionPath);
    final var outputLines = new WorkflowInspection().list(partitionState);
    outputLines.forEach(System.out::println);
    return 0;
  }

  @Command(name = "entity", description = "Show details about a workflow")
  public int entity(
      @Option(
              names = {"-k", "--key"},
              paramLabel = "KEY",
              description = "The key of the workflow",
              required = true)
          final long key) {
    final var partitionState = PartitionState.of(partitionPath);
    final var output = new WorkflowInspection().entity(partitionState, key);
    System.out.println(output);
    return 0;
  }
}
