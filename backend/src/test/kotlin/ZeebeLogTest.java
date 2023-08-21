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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.stream.impl.records.TypedRecordImpl;
import io.camunda.zeebe.util.FileUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.log.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.sql.Array;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
public class ZeebeLogTest {

  private static File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());
  private static final BpmnModelInstance PROCESS =
      Bpmn.createExecutableProcess("process")
          .startEvent()
          .parallelGateway("gw")
          .serviceTask("task")
          .zeebeJobType("type")
          .endEvent()
          .moveToLastGateway()
          .serviceTask("incidentTask")
          .zeebeInputExpression("=foo", "bar")
          .zeebeJobType("type")
          .endEvent()
          .done();
  private static final BpmnModelInstance SIMPLE_PROCESS =
      Bpmn.createExecutableProcess("simple")
          .startEvent()
          .endEvent()
          .done();
  private static final String CONTAINER_PATH = "/usr/local/zeebe/data/";
  @Container
  public static ZeebeContainer zeebeContainer = new ZeebeContainer()
      /* run the container with the current user, in order to access the data and delete it later */
      .withCreateContainerCmdModifier(cmd -> cmd.withUser(TestUtils.getRunAsUser()))
      .withFileSystemBind(tempDir.getPath(), CONTAINER_PATH, BindMode.READ_WRITE);
  private static CountDownLatch jobLatch;
  private static final AtomicLong jobKey = new AtomicLong();

  static {
    tempDir.mkdirs();
  }

  @BeforeAll
  public static void setup() throws Exception {
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();

    client.newDeployCommand()
        .addProcessModel(PROCESS, "process.bpmn")
        .addProcessModel(SIMPLE_PROCESS, "simple.bpmn")
        .send()
        .join();

    client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(Map.of("var1", "1", "var2", "12", "var3", "123"))
        .send()
        .join();

    client.newPublishMessageCommand().messageName("msg").correlationKey("123")
        .timeToLive(Duration.ofSeconds(1)).send().join();
    client.newPublishMessageCommand().messageName("msg12").correlationKey("123")
        .timeToLive(Duration.ofHours(1)).send().join();

    // Small hack to ensure we reached the task and job of the previous PI
    // If the process instance we start after, ended we can be sure that
    // we reached also the wait-state in the other PI.
    client
        .newCreateInstanceCommand()
        .bpmnProcessId("simple")
        .latestVersion()
        .withResult()
        .send()
        .join();

    var responseJobKey = 0L;
    do {
        final var activateJobsResponse = client.newActivateJobsCommand().jobType("type")
            .maxJobsToActivate(1).send()
            .join();
        if (activateJobsResponse != null && !activateJobsResponse.getJobs().isEmpty()) {
          responseJobKey = activateJobsResponse.getJobs().get(0).getKey();
        }
    } while (responseJobKey <= 0);

    client.close();
  }

  @AfterAll
  public static void cleanup() throws Exception {
    FileUtil.deleteFolderIfExists(tempDir.toPath());
  }

  @Test
  public void shouldReadStatusFromLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logStatus = new LogStatus(logPath);

    // when
    final var status = logStatus.status();

    // then
    assertThat(status.getHighestIndex()).isEqualTo(13);
    assertThat(status.getScannedEntries()).isEqualTo(13);
    assertThat(status.getHighestTerm()).isEqualTo(1);
    assertThat(status.getHighestRecordPosition()).isEqualTo(60);

    assertThat(status.getLowestIndex()).isEqualTo(1);
    assertThat(status.getLowestRecordPosition()).isEqualTo(1);

    assertThat(status.getMinEntrySizeBytes()).isNotZero();
    assertThat(status.getMinEntrySizeBytes()).isLessThan(status.getMaxEntrySizeBytes());

    assertThat(status.getMaxEntrySizeBytes()).isNotZero();
    assertThat(status.getMaxEntrySizeBytes()).isGreaterThan(status.getMinEntrySizeBytes());

    assertThat(status.getAvgEntrySizeBytes()).isNotZero();
    assertThat(status.getAvgEntrySizeBytes()).isGreaterThan(status.getMinEntrySizeBytes());
    assertThat(status.getAvgEntrySizeBytes()).isLessThan(status.getMaxEntrySizeBytes());

    assertThat(status.toString())
        .contains("lowestRecordPosition")
        .contains("highestRecordPosition")
        .contains("highestTerm")
        .contains("highestIndex")
        .contains("lowestIndex")
        .contains("scannedEntries")
        .contains("maxEntrySizeBytes")
        .contains("minEntrySizeBytes")
        .contains("avgEntrySizeBytes");
  }


  @Test
  public void shouldThrowWhenReadStatusFromNonExistingLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(new File("/tmp/doesntExist"), "1");

    // when - throw
    assertThatThrownBy(() -> new LogStatus(logPath))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Expected to read segments, but there was nothing to read");
  }


  @Test
  public void shouldBuildLogContent() throws JsonProcessingException {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);

    // when
    final var content = logContentReader.readAll();

    // then
    verifyCompleteLog(content.getRecords());

    final var objectMapper = new ObjectMapper();
    final var jsonNode = objectMapper.readTree(content.toString());
    assertThat(jsonNode).isNotNull(); // is valid json
  }

  @Test
  public void shouldReadLogContentWithIterator() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    verifyCompleteLog(records);
  }

  @Test
  public void shouldSkipFirstPartOfLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.seekToPosition(10);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    assertThat(records).hasSize(9);
    // we skip the first raft record
    assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(0);
    assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(9);

    final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
    assertThat(maxIndex).isEqualTo(13);
    final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
    assertThat(minIndex).isEqualTo(5);

    final var maxPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getHighestPosition)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxPosition).isEqualTo(60);
    final var minPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getLowestPosition)
            .min(Long::compareTo)
            .orElseThrow();
    assertThat(minPosition).isEqualTo(7);
  }

  @Test
  public void shouldNotSkipIfNegativeSeek() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.seekToPosition(-1);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    verifyCompleteLog(records);
  }

  @Test
  public void shouldNotSkipIfZeroSeek() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.seekToPosition(0);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    verifyCompleteLog(records);
  }

  @Test
  public void shouldSeekToEndOfLogIfNoExistingSeek() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.seekToPosition(Long.MAX_VALUE);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    assertThat(records).hasSize(1);
    assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(0);
    assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(1);

    final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
    assertThat(maxIndex).isEqualTo(13);
    final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
    assertThat(minIndex).isEqualTo(13);

    final var maxPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getHighestPosition)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxPosition).isEqualTo(60);
    final var minPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getLowestPosition)
            .min(Long::compareTo)
            .orElseThrow();
    assertThat(minPosition).isEqualTo(60);
  }

  @Test
  public void shouldLimitLogToPosition() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.limitToPosition(30);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    assertThat(records).hasSize(5);
    assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(1);
    assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(4);

    final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
    assertThat(maxIndex).isEqualTo(5);
    final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
    assertThat(minIndex).isEqualTo(1);

    final var maxPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getHighestPosition)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxPosition).isEqualTo(34);
    final var minPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getLowestPosition)
            .min(Long::compareTo)
            .orElseThrow();
    assertThat(minPosition).isEqualTo(1);
  }

  @Test
  public void shouldSeekAndLimitLogWithPosition() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.seekToPosition(5);
    logContentReader.limitToPosition(30);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    assertThat(records).hasSize(3);
    assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(0);
    assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(3);

    final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
    assertThat(maxIndex).isEqualTo(5);
    final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
    assertThat(minIndex).isEqualTo(3);

    final var maxPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getHighestPosition)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxPosition).isEqualTo(34);
    final var minPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getLowestPosition)
            .min(Long::compareTo)
            .orElseThrow();
    assertThat(minPosition).isEqualTo(2);
  }

  @Test
  public void shouldFilterWithProcessInstanceKey() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var records = new ArrayList<PersistedRecord>();
    logContentReader.seekToPosition(5);
    logContentReader.limitToPosition(30);

    // when
    logContentReader.forEachRemaining(records::add);

    // then
    assertThat(records).hasSize(3);
    assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(0);
    assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(3);

    final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
    assertThat(maxIndex).isEqualTo(5);
    final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
    assertThat(minIndex).isEqualTo(3);

    final var maxPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getHighestPosition)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxPosition).isEqualTo(34);
    final var minPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getLowestPosition)
            .min(Long::compareTo)
            .orElseThrow();
    assertThat(minPosition).isEqualTo(2);
  }

  private static void verifyCompleteLog(List<PersistedRecord> records) {
    assertThat(records).hasSize(13);
    assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(1);
    assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(12);

    final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
    assertThat(maxIndex).isEqualTo(13);
    final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
    assertThat(minIndex).isEqualTo(1);

    final var maxPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getHighestPosition)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxPosition).isEqualTo(60);
    final var minPosition = records.stream()
            .filter(ApplicationRecord.class::isInstance)
            .map(ApplicationRecord.class::cast)
            .map(ApplicationRecord::getLowestPosition)
            .min(Long::compareTo)
            .orElseThrow();
    assertThat(minPosition).isEqualTo(1);
  }

  @Test
  public void shouldReturnLogContentAsDotFile() throws JsonProcessingException {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);
    final var content = logContentReader.readAll();

    // when
    final var dotFileContent = content.asDotFile();

    // then
    assertThat(dotFileContent).startsWith("digraph log {").endsWith("}");
  }

  @Test
  public void shouldContainNoDuplicatesInLogContent() throws JsonProcessingException {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logContentReader = new LogContentReader(logPath);

    // when
    final var content = logContentReader.readAll();

    // then
    // validate that records are not duplicated in LogContent
    assertThat(content.getRecords())
        .filteredOn(ApplicationRecord.class::isInstance)
        .asInstanceOf(InstanceOfAssertFactories.list(ApplicationRecord.class))
        .flatExtracting(ApplicationRecord::getEntries)
        .extracting(TypedRecordImpl::getPosition)
        .doesNotHaveDuplicates();
  }

  @Test
  public void shouldSearchPositionInLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);
    final var position = 1;

    // when
    final Record<?> record = logSearch.searchPosition(position);

    // then
    assertThat(record).isNotNull();
    assertThat(record.getPosition()).isEqualTo(position);
  }

  @Test
  public void shouldReturnNullOnNegPosition() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);

    // when
    final Record<?> record = logSearch.searchPosition(-1);

    // then
    assertThat(record).isNull();
  }

  @Test
  public void shouldReturnNullOnToBigPosition() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);

    // when
    final Record<?> record = logSearch.searchPosition(Long.MAX_VALUE);

    // then
    assertThat(record).isNull();
  }

  @Test
  public void shouldSearchIndexInLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);
    final var index = 7;

    // when
    final var record = logSearch.searchIndex(index);

    // then
    assertThat(record).isNotNull();
  }

  @Test
  public void shouldNotReturnDuplicatesWhenSearchForIndexInLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);
    final var index = 7;

    // when
    final var record = logSearch.searchIndex(index);

    // then
    // validate that records are not duplicated in LogContent
    assertThat(record)
        .asInstanceOf(InstanceOfAssertFactories.type(ApplicationRecord.class))
        .extracting(ApplicationRecord::getEntries)
        .asInstanceOf(InstanceOfAssertFactories.list(Record.class))
        .extracting(Record::getPosition)
        .doesNotHaveDuplicates();
  }

  @Test
  public void shouldReturnNullOnNegIndex() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);

    // when
    final var logContent = logSearch.searchIndex(-1);

    // then
    assertThat(logContent).isNull();
  }

  @Test
  public void shouldReturnNullOnToBigIndex() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
    var logSearch = new LogSearch(logPath);

    // when
    final var logContent = logSearch.searchIndex(Long.MAX_VALUE);

    // then
    assertThat(logContent).isNull();
  }
}
