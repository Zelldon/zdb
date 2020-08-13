package io.zeebe;

import io.zeebe.impl.BlacklistInspection;
import io.zeebe.impl.LogScanner;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(name = "log", mixinStandardHelpOptions = true)
public class LogCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Option(
      names = {"-p", "--path"},
      paramLabel = "LOG_PATH",
      description = "The path to the partition log data, should end with the partition id.",
      required = true,
      scope = ScopeType.INHERIT)
  private Path partitionPath;

  @Command(name = "status", description = "Print's the status of the log")
  public int status() {
    final var output = new LogScanner().scan(partitionPath);
    System.out.println(output);
    return 0;
  }

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return 0;
  }
}
