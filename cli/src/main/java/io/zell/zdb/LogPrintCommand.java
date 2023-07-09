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

import io.zell.zdb.log.LogContentReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "print", description = "Print's the complete log to standard out")
public class LogPrintCommand implements Callable<Integer> {

  public enum Format {
    JSON,
    DOT,
  }

  @Spec private CommandSpec spec;

  @Option(
      names = {"-f", "--format"},
      description =
          "Print's the complete log in the specified format, defaults to json. Possible values: [ ${COMPLETION-CANDIDATES} ]",
      defaultValue = "JSON")
  private Format format;

  @Override
  public Integer call() {
    final Path partitionPath = spec.findOption("-p").getValue();
    final var logContentReader = new LogContentReader(partitionPath);
    final var logContent = logContentReader.content();
    if (format == Format.DOT) {
      System.out.println(logContent.asDotFile());
    } else {
      System.out.println(logContent);
    }

    return 0;
  }
}
