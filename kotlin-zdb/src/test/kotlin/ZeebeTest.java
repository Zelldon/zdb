import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ZeebeTest {

  public File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  @Container
  public ZeebeContainer zeebeContainer = new ZeebeContainer().withFileSystemBind(tempDir.getPath(), "/usr/local/zeebe/data/", BindMode.READ_WRITE);

//  @AfterEach
//  public void cleanUp() throws IOException {
//    FileUtil.deleteFolderIfExists(tempDir.toPath());
//  }

  @Test
  public void shouldRunProcessInstanceUntilEnd() {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    // when
    // do something (e.g. deploy a process)
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    // then
    // verify (e.g. we can create an instance of the deployed process)
    final ProcessInstanceResult processInstanceResult =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .withResult()
            .send()
            .join();
    assertThat(processInstanceResult.getProcessDefinitionKey())
        .isEqualTo(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey());
  }

  @Test
  public void shouldCreateAndUseVolume() {
    // given

    // when

    // then
    assertThat(tempDir).exists();
    assertThat(tempDir.listFiles()).extracting(File::getName).containsExactly("raft-partition");
  }
}
