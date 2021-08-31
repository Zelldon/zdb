import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import io.camunda.zeebe.util.FileUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.log.LogContentReader;
import io.zell.zdb.state.blacklist.BlacklistState;
import io.zell.zdb.state.general.GeneralState;
import io.zell.zdb.state.incident.IncidentState;
import io.zell.zdb.state.instance.InstanceDetails;
import io.zell.zdb.state.instance.InstanceState;
import io.zell.zdb.state.process.ProcessState;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ZeebeBlacklistStateTest {

  private static File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  static {
    // for the Zeebe container the folder need to exist
    tempDir.mkdirs();
  }

  private static final BpmnModelInstance process =
      Bpmn.createExecutableProcess("process")
          .startEvent()
            .serviceTask("task")
            .multiInstance(multi -> multi.parallel().zeebeInputCollection("=items").zeebeInputElement("item"))
            .zeebeJobType("type")
            .endEvent()
          .done();
  private static final String CONTAINER_PATH = "/usr/local/zeebe/data/";


  @Container
  public static ZeebeContainer zeebeContainer = new ZeebeContainer()
      /* run the container with the current user, in order to access the data and delete it later */
      .withCreateContainerCmdModifier(cmd -> cmd.withUser(TestUtils.getRunAsUser()))
      .withFileSystemBind(tempDir.getPath(), CONTAINER_PATH, BindMode.READ_WRITE);

  private static ProcessInstanceEvent returnedProcessInstance;

  @BeforeAll
  public static void setup() {
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    final var items = IntStream.range(1, 100_000).boxed().collect(Collectors.toList());

    returnedProcessInstance = client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(Map.of("items", items))
        .send()
        .join();

    try {
      Thread.sleep(1000); // give time to reach the multi instance
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    client.close();

  }

  @AfterAll
  public static void cleanup() throws Exception {
    FileUtil.deleteFolderIfExists(tempDir.toPath());
  }

  @Test
  public void shouldListBlacklistedInstances() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var blacklistState = new BlacklistState(runtimePath);

    // when
    final var blacklisted = Awaitility
        .await("should find blacklisted instances").until(blacklistState::listBlacklistedInstances,
            listed -> !listed.isEmpty());

    // then
    assertThat(blacklisted).isNotNull().isNotEmpty();
    assertThat(blacklisted.get(0)).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
  }

}
