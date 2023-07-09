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
import io.zell.zdb.state.Experimental;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// THIS IS IN ALPHA STATE
@Command(
    name = "state",
    mixinStandardHelpOptions = true,
    description = "Prints general information of the internal state")
public class StateCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true)
  private Path partitionPath;

  /**
   * Alpha feature: Planned to replace old status call
   *
   * @return the status code of the call
   */
  @Override
  public Integer call() {
    final var jsonString = new Experimental(partitionPath).stateStatisticsAsJsonString();
    System.out.println(jsonString);
    return 0;
  }

  /** Alpha feature: Planned to replace old specific status calls */
  @Command(name = "list", description = "List column families and the values as json")
  public int list(
      @Option(
              names = {"-cf", "--columnFamily"},
              paramLabel = "COLUMNFAMILY",
              description = "The column family name to filter for")
          final String columnFamilyName) {
    // we print incrementally in order to avoid to build up big state in the application
    System.out.print("{\"data\":[");
    final var experimental = new Experimental(partitionPath);
    final var counter = new AtomicInteger(0);
    experimental.visitDBWithJsonValues(
        ((cf, key, valueJson) -> {
          if (noColumnFamilyGiven(columnFamilyName)
              || isMatchingColumnFamily(columnFamilyName, cf)) {
            if (counter.getAndIncrement() >= 1) {
              System.out.print(',');
            }
            System.out.printf(
                "\n{\"cf\":\"%s\",\"key\":\"%s\",\"value\":%s}",
                cf, HexFormat.ofDelimiter(" ").formatHex(key), valueJson);
          }
        }));
    System.out.print("]}");
    return 0;
  }

  private static boolean isMatchingColumnFamily(String columnFamilyName, ZbColumnFamilies cf) {
    return cf.toString().equalsIgnoreCase(columnFamilyName);
  }

  private static boolean noColumnFamilyGiven(String columnFamilyName) {
    return columnFamilyName == null || columnFamilyName.isEmpty();
  }
}
