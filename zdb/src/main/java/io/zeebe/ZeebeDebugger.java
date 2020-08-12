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
    subcommands = {StatusCommand.class, BlackListCommand.class, IncidentCommand.class})
public class ZeebeDebugger implements Callable<Integer> {

  private static CommandLine cli;

  public static void main(String[] args) {
    cli = new CommandLine(new ZeebeDebugger()).setExecutionStrategy(new RunLast());
    final int exitcode = cli.execute(args);
    System.exit(exitcode);
  }

  public static void printUsage(final String subcommand) {
    if (subcommand == null) {
      cli.usage(System.out);
    } else {
      cli.getSubcommands().get(subcommand).usage(System.out);
    }
  }

  @Override
  public Integer call() {
    printUsage(null);
    return 0;
  }
}
