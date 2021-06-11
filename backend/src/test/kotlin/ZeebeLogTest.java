import io.atomix.raft.storage.log.RaftLog;
import io.atomix.raft.storage.log.RaftLogReader.Mode;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ZeebeLogTest {

  private static File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  static {
    tempDir.mkdirs();
  }

  private static final BpmnModelInstance process =
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
  private static final String PARTITION_NAME_FORMAT = "raft-partition-partition-%d";

  @Container
  public static ZeebeContainer zeebeContainer = new ZeebeContainer()
      .withCreateContainerCmdModifier(cmd -> cmd.withUser(TestUtils.getRunAsUser()))
      .withFileSystemBind(tempDir.getPath(), CONTAINER_PATH, BindMode.READ_WRITE);

  private static DeploymentEvent deploymentEvent;
  private static ProcessInstanceEvent returnedProcessInstance;
  private static CountDownLatch jobLatch;
  private static final AtomicLong jobKey = new AtomicLong();

  @BeforeAll
  public static void setup() throws Exception {
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    deploymentEvent = client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    returnedProcessInstance = client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(Map.of("var1", "1", "var2", "12", "var3", "123"))
        .send()
        .join();

    client.newPublishMessageCommand().messageName("msg").correlationKey("123").timeToLive(Duration.ofSeconds(1)).send().join();
    client.newPublishMessageCommand().messageName("msg12").correlationKey("123").timeToLive(Duration.ofHours(1)).send().join();

    jobLatch = new CountDownLatch(1);
    client.newWorker().jobType("type").handler((jobClient, job) -> {
      jobKey.set(job.getKey());
      jobLatch.countDown();
    }).open();
    jobLatch.await();

  }

// This is currently not working - it will cause java.nio.file.AccessDeniedException: /tmp/data--7809705097131595652/raft-partition/partitions/1/runtime/OPTIONS-000007
// Might be related to the test container usage
//  @AfterAll
//  public static void cleanup() throws Exception {
//    FileUtil.deleteFolderIfExists(tempDir.toPath());
//  }

  @Test
  public void shouldPrintLog() {
    // given
    final var logPath = ZeebePaths.Companion.getLogPath(tempDir, "1");

//    SegmentedJournal.builder().withDirectory(logPath.toFile()).build()

    var partitionName = "";
    try {
      final int partitionId = Integer.parseInt(logPath.getFileName().toString());
      partitionName = String.format(PARTITION_NAME_FORMAT, partitionId);
    } catch (NumberFormatException nfe) {
      final var errorMsg =
          String.format(
              "Expected to extract partition as integer from path, but path was '%s'.", logPath);
      throw new IllegalArgumentException(errorMsg, nfe);
    }

    final var raftLog =
    RaftLog.builder()
        .withDirectory(logPath.toFile())
        .withName(partitionName)
        .withMaxSegmentSize(512 * 1024 * 1024).build();

    final var raftLogReader = raftLog.openReader(Mode.ALL);

    raftLogReader.seekToLast();


    // when



  }
}
