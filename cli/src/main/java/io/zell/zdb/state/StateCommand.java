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
package io.zell.zdb.state;

import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.zell.zdb.JsonPrinter;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "state",
    mixinStandardHelpOptions = true,
    description = "Prints general information of the internal state")
public class StateCommand implements Callable<Integer> {

  public static final String ENTRY_FORMAT = "\n{\"cf\":\"%s\",\"key\":\"%s\",\"value\":%s}";

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      scope = CommandLine.ScopeType.INHERIT,
      required = true)
  private Path partitionPath;

  @Override
  public Integer call() {
    final var jsonString = new ZeebeDbReader(partitionPath).stateStatisticsAsJsonString();
    System.out.println(jsonString);
    return 0;
  }

  @Command(name = "list", description = "List column families and the values as json")
  public int list(
      @Option(
              names = {"-cf", "--columnFamily"},
              paramLabel = "COLUMNFAMILY",
              description = "The column family name to filter for")
          final String columnFamilyName) {

    new JsonPrinter()
        .surround(
            (printer) -> {
              final var zeebeDbReader = new ZeebeDbReader(partitionPath);
              // we print incrementally in order to avoid to build up big state in the application
              if (noColumnFamilyGiven(columnFamilyName)) {
                zeebeDbReader.visitDBWithJsonValues(
                    ((cf, key, valueJson) ->
                        printer.accept(
                            String.format(
                                ENTRY_FORMAT,
                                cf,
                                HexFormat.ofDelimiter(" ").formatHex(key),
                                valueJson))));
              } else {
                final var cf = ZbColumnFamilies.valueOf(columnFamilyName);
                zeebeDbReader.visitDBWithPrefix(
                    cf,
                    ((key, valueJson) ->
                        printer.accept(
                            String.format(
                                ENTRY_FORMAT,
                                cf,
                                HexFormat.ofDelimiter(" ").formatHex(key),
                                valueJson))));
              }
            });
    return 0;
  }

  private static boolean noColumnFamilyGiven(String columnFamilyName) {
    return columnFamilyName == null || columnFamilyName.isEmpty();
  }
}
