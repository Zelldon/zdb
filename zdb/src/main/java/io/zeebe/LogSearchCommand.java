package io.zeebe;

import io.zeebe.impl.log.LogSearch;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "search", mixinStandardHelpOptions = true)
public class LogSearchCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @ArgGroup(exclusive = true, multiplicity = "1")
  private Exclusive exclusive;

  @Override
  public Integer call() {
    final Path logPath = spec.findOption("-p").getValue();
    final String result;
    if (exclusive.index == 0) {
      result = new LogSearch().searchForPosition(logPath, exclusive.position);
    } else {
      result = new LogSearch().searchForIndex(logPath, exclusive.index);
    }
    System.out.println(result);
    return 1;
  }

  static class Exclusive {
    @Option(
        names = {"-pos", "--position"},
        paramLabel = "POSITION",
        description = "The position of a record to search for.",
        required = true)
    private long position;

    @Option(
        names = {"-idx", "--index"},
        paramLabel = "INDEX",
        description = "The index of an entry to search for.",
        required = true)
    private long index;
  }
}
