package io.zeebe;

import io.zeebe.impl.LogScanner;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(name = "scan", mixinStandardHelpOptions = true)
public class LogScanCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Option(
      names = {"-p", "--path"},
      paramLabel = "LOG_PATH",
      description = "The path to the partition log data, should end with the partition id.",
      required = true,
      scope = ScopeType.INHERIT)
  private Path partitionPath;

  @Override
  public Integer call() {
    final var output = new LogScanner().scan(partitionPath);
    System.out.println(output);
    return 0;
  }
}
