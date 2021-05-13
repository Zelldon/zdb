import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import io.zell.zdb.ProcessInspection;
import io.zell.zdb.ZeebePaths;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ProcessInspectionTest {

  public File tempDir = new File("/tmp/", "data-" + ThreadLocalRandom.current().nextLong());

  @Container
  public ZeebeContainer zeebeContainer = new ZeebeContainer().withFileSystemBind(tempDir.getPath(), "/usr/local/zeebe/data/", BindMode.READ_WRITE);

  @Test
  public void shouldListProcesses() {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .gatewayAddress(zeebeContainer.getExternalGatewayAddress())
            .usePlaintext()
            .build();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();
    final DeploymentEvent deploymentEvent =
        client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();

    // when
    final var workflowInspection = new ProcessInspection(
        ZeebePaths.Companion.getRuntimePath(tempDir, "1"));
    final var processes = workflowInspection.listProcesses();

    // then
    final var processString = processes.get(0);
    final var returnedProcess = deploymentEvent.getProcesses().get(0);
    assertThat(processString).contains(returnedProcess.getBpmnProcessId())
        .contains(returnedProcess.getResourceName())
        .contains("" + returnedProcess.getVersion())
        .contains("" + returnedProcess.getProcessDefinitionKey());
  }
}
