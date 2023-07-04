import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.util.FileUtil;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ZeebePaths;
import io.zell.zdb.state.banned.BannedInstanceState;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Disabled("We can't reliable produce banned instances currently, not easily at least.")
public class ZeebeBannedInstanceStateTest {

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
      .withEnv("ZEEBE_BROKER_EXPERIMENTAL_ROCKSDB_DISABLEWAL", "false")
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

    final var items = IntStream.range(1, 250_000).boxed().collect(Collectors.toList());

    returnedProcessInstance = client
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .variables(Map.of("items", items))
        .send()
        .join();


    client.newActivateJobsCommand().jobType("task").maxJobsToActivate(1000).send().join();
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
  public void shouldListBannedInstances() {
    // given
    final var runtimePath = ZeebePaths.Companion.getRuntimePath(tempDir, "1");
    final var bannedInstanceState = new BannedInstanceState(runtimePath);

    // when
    final var bannedInstances = Awaitility
        .await("should find banned instances").until(bannedInstanceState::listBannedInstances,
            listed -> !listed.isEmpty());

    // then
    assertThat(bannedInstances).isNotNull().isNotEmpty();
    assertThat(bannedInstances.get(0)).isEqualTo(returnedProcessInstance.getProcessInstanceKey());
  }

}
