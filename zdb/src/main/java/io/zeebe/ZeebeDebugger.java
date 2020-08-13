/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe;

import java.util.concurrent.Callable;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.RunLast;

@Command(
    name = "zdb",
    mixinStandardHelpOptions = true,
    version = ZeebeDebugger.ZDB_VERSION,
    description = "Zeebe debug and inspection tool",
    subcommands = {
      GenerateCompletion.class, // to generate auto completion
      StatusCommand.class,
      BlacklistCommand.class,
      IncidentCommand.class,
      WorkflowCommand.class,
      LogCommand.class
    })
public class ZeebeDebugger implements Callable<Integer> {

  protected static final String ZDB_VERSION = "zdb 0.1";

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
