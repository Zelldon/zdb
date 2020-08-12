package io.zeebe;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.RunLast;

@Command(
    name = "zdb",
    mixinStandardHelpOptions = true,
    version = "zdb 0.1",
    description = "Zeebe debug and inspection tool",
    subcommands = StatusCommand.class)
public class ZeebeDebugger implements Callable<Integer> {

  private static CommandLine cli;

  public static void main(String[] args) {
    cli = new CommandLine(new ZeebeDebugger()).setExecutionStrategy(new RunLast());
    final int exitcode = cli.execute(args);
    System.exit(exitcode);
  }

  @Override
  public Integer call() {
    cli.usage(System.out);
    return 0;
  }
}
