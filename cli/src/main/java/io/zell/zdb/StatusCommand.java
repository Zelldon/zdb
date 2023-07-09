/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
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

import io.zell.zdb.state.Experimental;
import io.zell.zdb.state.general.GeneralState;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "status",
    mixinStandardHelpOptions = true,
    description = "Prints general information of the internal state")
public class StatusCommand implements Callable<Integer> {

  @Option(
      names = {"-p", "--path"},
      paramLabel = "PARTITION_PATH",
      description = "The path to the partition data (either runtime or snapshot in partition dir)",
      required = true)
  private Path partitionPath;

  @Override
  public Integer call() {
    final var generalDetails = new GeneralState(partitionPath).generalDetails();
    System.out.println(generalDetails);
    return 0;
  }

  /**
   * Alpha feature: Planned to replace old status call
   *
   * @return the status code of the call
   */
  @Command(name = "details", description = "Print for all column families the detailed statistics")
  public int list() {
    final var jsonString = new Experimental(partitionPath).stateStatisticsAsJsonString();
    System.out.println(jsonString);
    return 0;
  }
}
