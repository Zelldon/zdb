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

import io.camunda.zeebe.protocol.record.value.ProcessInstanceRelated;
import io.zell.zdb.log.ApplicationRecord;
import io.zell.zdb.log.LogContentReader;
import io.zell.zdb.log.PersistedRecord;
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

  @Option(
      names = {"--from", "--fromPosition"},
      description = "Print's the complete log from the given position.",
      defaultValue = "0")
  private long fromPosition;

  @Option(
      names = {"--to", "--toPosition"},
      description = "Print's the complete log to the given position.",
      defaultValue = Long.MAX_VALUE + "")
  private long toPosition;

  @Option(
      names = {"--instanceKey"},
      description = "Print's the log only containing records with the given process instance key.",
      defaultValue = "0")
  private long instanceKey;

  @Override
  public Integer call() {
    final Path partitionPath = spec.findOption("-p").getValue();
    final var logContentReader = new LogContentReader(partitionPath);
    if (format == Format.DOT) {
      // for backwards compatibility
      final var logContent = logContentReader.readAll();
      System.out.println(logContent.asDotFile());
    } else {
      printJson(logContentReader);
    }

    return 0;
  }

  private void printJson(LogContentReader logContentReader) {
    System.out.println("[");
    logContentReader.seekToPosition(fromPosition);
    var separator = "";
    while (logContentReader.hasNext()) {
      final var record = logContentReader.next();
      if (record instanceof ApplicationRecord engineRecord) {
        if (engineRecord.getLowestPosition() > toPosition) {
          break;
        }
      }

      if (shouldPrintRecord(record)) {
        System.out.print(separator + record);
        separator = ",";
      }
    }
    System.out.println("]");
  }

  private boolean shouldPrintRecord(PersistedRecord record) {
    if (instanceKey == 0) {
      return true;
    } else {
      if (record instanceof ApplicationRecord engineRecord) {
        final var entries = engineRecord.getEntries();
        boolean printRecord = false;
        for (var entry : entries) {
          if (entry.getValue() instanceof ProcessInstanceRelated instanceRelated) {
            if (instanceRelated.getProcessInstanceKey() == instanceKey) {
              printRecord = true;
              break;
            }
          }
        }
        return printRecord;
      } else {
        return false;
      }
    }
  }
}
