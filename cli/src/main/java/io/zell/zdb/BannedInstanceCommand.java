/*
 * Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
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
package io.zell.zdb;

import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.zell.zdb.state.ZeebeDbReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

@Command(
    name = "banned",
    aliases = "blacklist",
    mixinStandardHelpOptions = true,
    description = "Print's information about banned process instances")
public class BannedInstanceCommand implements Callable<Integer> {

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

  @Command(name = "list", description = "List all banned process instances")
  public int list() {
    new JsonPrinter()
        .surround(
            (printer) -> {
              final var zeebeDbReader = new ZeebeDbReader(partitionPath);
              zeebeDbReader.visitDBWithPrefix(
                  ZbColumnFamilies.BANNED_INSTANCE,
                  (key, valueJson) -> {
                    printer.accept(valueJson);
                  });
            });
    return 0;
  }

  @Command(name = "entity", description = "Show details about banned process instance")
  public int entity(
      @Parameters(
              paramLabel = "KEY",
              description = "The key of the banned process instance",
              arity = "1")
          final long key) {
    final var zeebeDbReader = new ZeebeDbReader(partitionPath);
    final var valueAsJson = zeebeDbReader.getValueAsJson(ZbColumnFamilies.BANNED_INSTANCE, key);
    System.out.println(valueAsJson);
    return 0;
  }
}
