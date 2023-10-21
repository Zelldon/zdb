package io.zell.zdb.v82;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.util.FileUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.TestUtils;
import io.zell.zdb.ZeebeContentCreator;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.log.LogContentReader;
import io.zell.zdb.log.LogSearch;
import io.zell.zdb.log.LogStatus;
import io.zell.zdb.log.LogWriter;
import io.zell.zdb.log.records.ApplicationRecord;
import io.zell.zdb.log.records.PersistedRecord;
import io.zell.zdb.log.records.RaftRecord;
import io.zell.zdb.log.records.Record;
import io.zell.zdb.state.ZeebeDbReader;
import io.zell.zdb.state.process.ProcessState;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
public class Version82Test {

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
    private static final String CONTAINER_PATH = "/usr/local/zeebe/data/";
    @Container
    public static ZeebeContainer zeebeContainer = new ZeebeContainer(DockerImageName.parse("camunda/zeebe:8.2.16"))
            /* run the container with the current user, in order to access the data and delete it later */
            .withCreateContainerCmdModifier(cmd -> cmd.withUser(TestUtils.getRunAsUser()))
            .withEnv("ZEEBE_BROKER_EXPERIMENTAL_ROCKSDB_DISABLEWAL", "false")
            .withFileSystemBind(tempDir.getPath(), CONTAINER_PATH, BindMode.READ_WRITE);
    private static ZeebeContentCreator zeebeContentCreator;

    static {
        tempDir.mkdirs();
    }

    @BeforeAll
    public static void setup() {
        zeebeContentCreator = new ZeebeContentCreator(zeebeContainer.getExternalGatewayAddress(), PROCESS);
        zeebeContentCreator
                .createContent();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        FileUtil.deleteFolderIfExists(tempDir.toPath());
    }

    @Nested
    public class ZeebeLogTest {

        @Test
        public void shouldReadStatusFromLog() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logStatus = new LogStatus(logPath);

            // when
            final var status = logStatus.status();

            // then
            assertThat(status.getHighestIndex()).isEqualTo(13);
            assertThat(status.getHighestTerm()).isEqualTo(1);
            assertThat(status.getHighestRecordPosition()).isEqualTo(60);
            assertThat(status.getLowestIndex()).isEqualTo(1);
            assertThat(status.getLowestRecordPosition()).isEqualTo(1);

            assertThat(status.toString())
                    .contains("lowestRecordPosition")
                    .contains("highestRecordPosition")
                    .contains("highestTerm")
                    .contains("highestIndex")
                    .contains("lowestIndex");
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
        public void shouldLimitViaPositionExclusive() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logContentReader = new LogContentReader(logPath);
            final var records = new ArrayList<PersistedRecord>();
            logContentReader.limitToPosition(1);

            // when
            logContentReader.forEachRemaining(records::add);

            // then
            assertThat(records).hasSize(1);
            assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(1);
            final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
            assertThat(maxIndex).isEqualTo(1);
            final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
            assertThat(minIndex).isEqualTo(1);
        }

        @Test
        public void shouldConvertRecordToColumn() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logContentReader = new LogContentReader(logPath);
            final var records = new ArrayList<PersistedRecord>();
            logContentReader.limitToPosition(2);

            // when
            logContentReader.forEachRemaining(records::add);

            // then
            assertThat(records).hasSize(2);
            assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(1);
            assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(1);

            final var record = (ApplicationRecord) records.get(1);
            // Index Term RecordType ValueType Intent Position SourceRecordPosition

            String columnString = record.asColumnString();
            String[] elements = columnString.trim().split(" ");
            assertThat(elements).hasSize(9); // deployment record skips the last two columns
            assertThat(elements).containsSubsequence("2", "1", "1", "-1");
            // we skip timestamp since it is not reproducible
            assertThat(elements).containsSubsequence("-1", "COMMAND", "DEPLOYMENT", "CREATE");
        }

        @Test
        public void shouldWriteTableHeaderToStreamWhenNoDataFound() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logContentReader = new LogContentReader(logPath);
            final var outputStream = new ByteArrayOutputStream();
            logContentReader.limitToPosition(30);
            logContentReader.seekToPosition(3);
            logContentReader.filterForProcessInstance(2251799813685254L);
            final var logWriter = new LogWriter(outputStream, logContentReader);

            // when
            logWriter.writeAsTable();

            // then
            assertThat(outputStream.toString().trim())
                    .isEqualTo("Index Term Position SourceRecordPosition Timestamp Key RecordType ValueType Intent ProcessInstanceKey BPMNElementType");
        }

        @Test
        public void shouldWriteTableToStream() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logContentReader = new LogContentReader(logPath);
            final var records = new ArrayList<PersistedRecord>();
            final var outputStream = new ByteArrayOutputStream();
            logContentReader.limitToPosition(600);
            logContentReader.seekToPosition(6);
            logContentReader.filterForProcessInstance(2251799813685252L);
            final var logWriter = new LogWriter(outputStream, logContentReader);

            // when
            logWriter.writeAsTable();

            // then
            assertThat(outputStream.toString())
                    .startsWith("Index Term Position SourceRecordPosition Timestamp Key RecordType ValueType Intent ProcessInstanceKey BPMNElementType")
                    // EQUALs check is hard due to the timestamp
                    .contains("2251799813685253 EVENT VARIABLE CREATED 2251799813685252")
                    .contains("EVENT PROCESS_INSTANCE ELEMENT_ACTIVATING 2251799813685252 START_EVENT");
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
            logContentReader.filterForProcessInstance(2251799813685252L);

            // when
            logContentReader.forEachRemaining(records::add);

            // then
            assertThat(records).hasSize(1);
            assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(0);
            assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(1);

            final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
            assertThat(maxIndex).isEqualTo(5);
            final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
            assertThat(minIndex).isEqualTo(5);

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
            assertThat(minPosition).isEqualTo(7);
        }

        @Test
        public void shouldFilterWithNoExistingProcessInstanceKey() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logContentReader = new LogContentReader(logPath);
            final var records = new ArrayList<PersistedRecord>();
            logContentReader.filterForProcessInstance(0xCAFE);

            // when
            logContentReader.forEachRemaining(records::add);

            // then
            assertThat(records).hasSize(0);
        }

        @Test
        public void shouldFilterWithProcessInstanceKeyAndSetBeginAndEndOfLogPosition() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logContentReader = new LogContentReader(logPath);
            final var records = new ArrayList<PersistedRecord>();
            logContentReader.filterForProcessInstance(2251799813685252L);
            logContentReader.seekToPosition(5);
            logContentReader.limitToPosition(30);

            // when
            logContentReader.forEachRemaining(records::add);

            // then
            assertThat(records).hasSize(1);
            assertThat(records.stream().filter(RaftRecord.class::isInstance).count()).isEqualTo(0);
            assertThat(records.stream().filter(ApplicationRecord.class::isInstance).count()).isEqualTo(1);

            final var maxIndex = records.stream().map(PersistedRecord::index).max(Long::compareTo).get();
            assertThat(maxIndex).isEqualTo(5);
            final var minIndex = records.stream().map(PersistedRecord::index).min(Long::compareTo).get();
            assertThat(minIndex).isEqualTo(5);

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
            assertThat(minPosition).isEqualTo(7);
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
                    .extracting(Record::getPosition)
                    .doesNotHaveDuplicates();
        }

        @Test
        public void shouldSearchPositionInLog() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logSearch = new LogSearch(logPath);
            final var position = 1;

            // when
            final io.zell.zdb.log.records.Record record = logSearch.searchPosition(position);

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
            final io.zell.zdb.log.records.Record record = logSearch.searchPosition(-1);

            // then
            assertThat(record).isNull();
        }

        @Test
        public void shouldReturnNullOnToBigPosition() {
            // given
            final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");
            var logSearch = new LogSearch(logPath);

            // when
            final io.zell.zdb.log.records.Record record = logSearch.searchPosition(Long.MAX_VALUE);

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
                    .asInstanceOf(InstanceOfAssertFactories.list(io.zell.zdb.log.records.Record.class))
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

    @Nested
    public class ZeebeStateTest {

        @Test
        public void shouldCreateStatsForCompleteState() {
            // given
            final var experimental = new ZeebeDbReader(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));

            // when
            final var cfMap = experimental.stateStatistics();

            // then
            assertThat(cfMap).containsEntry(ZbColumnFamilies.JOBS, 1)
                    .containsEntry(ZbColumnFamilies.VARIABLES, 3)
                    .containsEntry(ZbColumnFamilies.INCIDENTS, 1)
                    .containsEntry(ZbColumnFamilies.ELEMENT_INSTANCE_KEY, 3);
        }

        @Test
        public void shouldVisitValuesAsJson() {
            // given
            final var experimental = new ZeebeDbReader(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
            final var incidentMap = new HashMap<String, String>();
            ZeebeDbReader.JsonValueVisitor jsonVisitor = (cf, k, v) -> {
                if (cf == ZbColumnFamilies.INCIDENTS) {
                    incidentMap.put(new String(k), v);
                }
            };

            // when
            experimental.visitDBWithJsonValues(jsonVisitor);

            // then
            assertThat(incidentMap).containsValue("{\"incidentRecord\":{\"errorType\":\"IO_MAPPING_ERROR\",\"errorMessage\":\"failed to evaluate expression '{bar:foo}': no variable found for name 'foo'\",\"bpmnProcessId\":\"process\",\"processDefinitionKey\":2251799813685249,\"processInstanceKey\":2251799813685252,\"elementId\":\"incidentTask\",\"elementInstanceKey\":2251799813685261,\"jobKey\":-1,\"variableScopeKey\":2251799813685261}}");
        }

        @Test
        public void shouldListProcesses() {
            // given

            ZeebeDbReader zeebeDbReader = new ZeebeDbReader(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));

            zeebeDbReader.visitDB(((cf, key, value) ->
            {

                System.out.printf("\nColumnFamily?: '%s'", cf);
                System.out.printf("\nKey: '%s'", new String(key));
                System.out.printf("\nValue: '%s'", new String(value));
            }));
        }


        @Test
        public void shouldGetProcessDetails() throws JsonProcessingException {
            // given
            final var processes = new ArrayList<String>();
            Path runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
            final var processState = new ProcessState(runtimePath);
            final var returnedProcess = zeebeContentCreator.deploymentEvent.getProcesses().get(0);

            // when
            processState.processDetails(returnedProcess.getProcessDefinitionKey(), (k, v) -> processes.add(v));

            // then
            assertThat(processes).hasSize(1);

            final var objectMapper = new ObjectMapper();

            final var jsonNode = objectMapper.readTree(processes.get(0));

            assertThat(jsonNode.get("bpmnProcessId").asText()).isEqualTo(returnedProcess.getBpmnProcessId());
            assertThat(jsonNode.get("key").asLong())
                    .isEqualTo(returnedProcess.getProcessDefinitionKey());
            assertThat(jsonNode.get("resourceName").asText()).isEqualTo(returnedProcess.getResourceName());
            assertThat(jsonNode.get("version").asInt()).isEqualTo(returnedProcess.getVersion());
            assertThat(jsonNode.get("resource").asText())
                    .isEqualTo(Base64.getEncoder().encodeToString(Bpmn.convertToString(PROCESS).getBytes(StandardCharsets.UTF_8)));
        }


  @Test
  public void shouldNotFailOnNonExistingProcess() {
    // given
      final var processes = new ArrayList<String>();

    // when
    final var processState = new ProcessState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    processState.processDetails(0xCAFE, (k, v) -> processes.add(v));

    // then
    assertThat(processes).isEmpty();
  }
//
//  @Test
//  public void shouldGetProcessInstanceDetails() {
//    // given
//
//    // when
//    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
//    final var actualInstanceDetails = processState
//        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());
//
//    // then
//    assertThat(actualInstanceDetails).isNotNull();
//    assertThat(actualInstanceDetails.getKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualInstanceDetails.getState()).isEqualTo(ProcessInstanceIntent.ELEMENT_ACTIVATED.toString());
//    assertThat(actualInstanceDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(actualInstanceDetails.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(actualInstanceDetails.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
//    assertThat(actualInstanceDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualInstanceDetails.getElementId()).isEqualTo("process");
//    assertThat(actualInstanceDetails.getElementType()).isEqualTo(BpmnElementType.PROCESS);
//    assertThat(actualInstanceDetails.getParentProcessInstanceKey()).isEqualTo(-1);
//    assertThat(actualInstanceDetails.getParentElementInstanceKey()).isEqualTo(-1);
//    assertThat(actualInstanceDetails.getFlowScopeKey()).isEqualTo(-1);
//    assertThat(actualInstanceDetails.getChildren()).hasSize(2);
//
//    assertThat(actualInstanceDetails.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("" + returnedProcessInstance.getVersion())
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(BpmnElementType.PROCESS.toString())
//        .contains("process");
//  }
//
//
//  @Test
//  public void shouldGetElementInstanceDetails() {
//    // given
//    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
//    final var processInstance = processState
//        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());
//
//    // when
//    InstanceDetails actualElementInstance = processState
//        .instanceDetails(processInstance.getChildren().get(1));
//
//    // then
//    assertThat(actualElementInstance).isNotNull();
//    assertThat(actualElementInstance.getState()).isEqualTo(ProcessInstanceIntent.ELEMENT_ACTIVATED.toString());
//    assertThat(actualElementInstance.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(actualElementInstance.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(actualElementInstance.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
//    assertThat(actualElementInstance.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualElementInstance.getElementId()).isEqualTo("task");
//    assertThat(actualElementInstance.getElementType()).isEqualTo(BpmnElementType.SERVICE_TASK);
//    assertThat(actualElementInstance.getParentProcessInstanceKey()).isEqualTo(-1);
//    assertThat(actualElementInstance.getParentElementInstanceKey()).isEqualTo(-1);
//    assertThat(actualElementInstance.getJobKey()).isEqualTo(jobKey.get());
//    assertThat(actualElementInstance.getFlowScopeKey()).isEqualTo(processInstance.getKey());
//    assertThat(actualElementInstance.getChildren()).isEmpty();
//
//    assertThat(actualElementInstance.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("" + returnedProcessInstance.getVersion())
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(BpmnElementType.SERVICE_TASK.toString())
//        .contains("task");
//  }
//
//
//  @Test
//  public void shouldListElementInstanceDetails() {
//    // given
//    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
//
//    // when
//    final var detailsList = processState.listInstances();
//
//    // then
//    assertThat(detailsList).hasSize(3);
//
//    final var actualInstanceDetails = detailsList.get(0);
//    assertThat(actualInstanceDetails).isNotNull();
//    assertThat(actualInstanceDetails.getKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualInstanceDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(actualInstanceDetails.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(actualInstanceDetails.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
//    assertThat(actualInstanceDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualInstanceDetails.getElementId()).isEqualTo("process");
//    assertThat(actualInstanceDetails.getElementType()).isEqualTo(BpmnElementType.PROCESS);
//    assertThat(actualInstanceDetails.getParentProcessInstanceKey()).isEqualTo(-1);
//    assertThat(actualInstanceDetails.getParentElementInstanceKey()).isEqualTo(-1);
//    assertThat(actualInstanceDetails.getFlowScopeKey()).isEqualTo(-1);
//    assertThat(actualInstanceDetails.getChildren()).hasSize(2);
//
//    assertThat(actualInstanceDetails.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("" + returnedProcessInstance.getVersion())
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(BpmnElementType.PROCESS.toString())
//        .contains("process");
//
//    var actualElementInstance = detailsList.get(1);
//    assertThat(actualElementInstance).isNotNull();
//    assertThat(actualElementInstance.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(actualElementInstance.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(actualElementInstance.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
//    assertThat(actualElementInstance.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualElementInstance.getElementId()).isEqualTo("incidentTask");
//    assertThat(actualElementInstance.getElementType()).isEqualTo(BpmnElementType.SERVICE_TASK);
//    assertThat(actualElementInstance.getParentProcessInstanceKey()).isEqualTo(-1);
//    assertThat(actualElementInstance.getParentElementInstanceKey()).isEqualTo(-1);
//    assertThat(actualElementInstance.getJobKey()).isZero(); // because of incident
//    assertThat(actualElementInstance.getFlowScopeKey()).isEqualTo(actualInstanceDetails.getKey());
//    assertThat(actualElementInstance.getChildren()).isEmpty();
//
//    assertThat(actualElementInstance.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("" + returnedProcessInstance.getVersion())
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(BpmnElementType.SERVICE_TASK.toString())
//        .contains("incidentTask");
//
//    actualElementInstance = detailsList.get(2);
//    assertThat(actualElementInstance).isNotNull();
//    assertThat(actualElementInstance.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(actualElementInstance.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(actualElementInstance.getVersion()).isEqualTo(returnedProcessInstance.getVersion());
//    assertThat(actualElementInstance.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(actualElementInstance.getElementId()).isEqualTo("task");
//    assertThat(actualElementInstance.getElementType()).isEqualTo(BpmnElementType.SERVICE_TASK);
//    assertThat(actualElementInstance.getParentProcessInstanceKey()).isEqualTo(-1);
//    assertThat(actualElementInstance.getParentElementInstanceKey()).isEqualTo(-1);
//    assertThat(actualElementInstance.getJobKey()).isEqualTo(jobKey.get());
//    assertThat(actualElementInstance.getFlowScopeKey()).isEqualTo(actualInstanceDetails.getKey());
//    assertThat(actualElementInstance.getChildren()).isEmpty();
//
//    assertThat(actualElementInstance.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("" + returnedProcessInstance.getVersion())
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(BpmnElementType.SERVICE_TASK.toString())
//        .contains("task");
//  }
//
//  @Test
//  public void shouldNotFailOnNonExistingProcessInstance() {
//    // given
//
//    // when
//    final var processState = new InstanceState(ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
//    final var actualInstanceDetails = processState
//        .instanceDetails(0xCAFE);
//
//    // then
//    assertThat(actualInstanceDetails).isNull();
//  }
//
//  @Test
//  public void shouldGetIncidentDetails() {
//    // given
//    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
//    final var processState = new InstanceState(runtimePath);
//    final var processInstance = processState
//        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());
//    InstanceDetails actualElementInstance = processState
//        .instanceDetails(processInstance.getChildren().get(0));
//    final var incidentState = new IncidentState(runtimePath);
//    final var incidentKey = incidentState.processInstanceIncidentKey(actualElementInstance.getKey());
//
//    // when
//    final var incidentDetails = incidentState.incidentDetails(incidentKey);
//
//    // then
//    assertThat(incidentDetails).isNotNull();
//    assertThat(incidentDetails.getKey()).isEqualTo(incidentKey);
//    assertThat(incidentDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(incidentDetails.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(incidentDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(incidentDetails.getElementInstanceKey()).isEqualTo(actualElementInstance.getKey());
//    assertThat(incidentDetails.getElementId()).isEqualTo("incidentTask");
//    assertThat(incidentDetails.getErrorMessage()).isEqualTo("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
//    assertThat(incidentDetails.getErrorType()).isEqualTo(ErrorType.IO_MAPPING_ERROR);
//    assertThat(incidentDetails.getVariablesScopeKey()).isEqualTo(actualElementInstance.getKey());
//    assertThat(incidentDetails.getJobKey()).isEqualTo(-1);
//
//    assertThat(incidentDetails.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("incidentTask")
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(ErrorType.IO_MAPPING_ERROR.toString())
//        .contains("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
//  }
//
//  @Test
//  public void shouldListIncidentDetails() {
//    // given
//    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
//    final var processState = new InstanceState(runtimePath);
//    final var processInstance = processState
//        .instanceDetails(returnedProcessInstance.getProcessInstanceKey());
//    InstanceDetails actualElementInstance = processState
//        .instanceDetails(processInstance.getChildren().get(0));
//    final var incidentState = new IncidentState(runtimePath);
//    final var incidentKey = incidentState.processInstanceIncidentKey(actualElementInstance.getKey());
//
//    // when
//    final var incidents = incidentState.listIncidents();
//
//    // then
//    assertThat(incidents).hasSize(1);
//
//    final var incidentDetails = incidents.get(0);
//    assertThat(incidentDetails).isNotNull();
//    assertThat(incidentDetails.getKey()).isEqualTo(incidentKey);
//    assertThat(incidentDetails.getBpmnProcessId()).isEqualTo(returnedProcessInstance.getBpmnProcessId());
//    assertThat(incidentDetails.getProcessDefinitionKey())
//        .isEqualTo(returnedProcessInstance.getProcessDefinitionKey());
//    assertThat(incidentDetails.getProcessInstanceKey()).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
//    assertThat(incidentDetails.getElementInstanceKey()).isEqualTo(actualElementInstance.getKey());
//    assertThat(incidentDetails.getElementId()).isEqualTo("incidentTask");
//    assertThat(incidentDetails.getErrorMessage()).isEqualTo("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
//    assertThat(incidentDetails.getErrorType()).isEqualTo(ErrorType.IO_MAPPING_ERROR);
//    assertThat(incidentDetails.getVariablesScopeKey()).isEqualTo(actualElementInstance.getKey());
//    assertThat(incidentDetails.getJobKey()).isEqualTo(-1);
//
//    assertThat(incidentDetails.toString())
//        .contains(returnedProcessInstance.getBpmnProcessId())
//        .contains("incidentTask")
//        .contains("" + returnedProcessInstance.getProcessDefinitionKey())
//        .contains(ErrorType.IO_MAPPING_ERROR.toString())
//        .contains("failed to evaluate expression '{bar:foo}': no variable found for name 'foo'");
//  }
//

    }
}
