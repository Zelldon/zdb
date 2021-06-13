import io.atomix.raft.storage.log.RaftLog;
import io.atomix.raft.storage.log.RaftLogReader.Mode;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.util.FileUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.log.LogStatus;
import io.zell.zdb.log.LogStatusDetails;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
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

  @Container
  public static ZeebeContainer zeebeContainer = new ZeebeContainer()
      /* run the container with the current user, in order to access the data and delete it later */
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




  }
}
